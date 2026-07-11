from __future__ import annotations

from common.cache.feed_cache import FeedCacheService
from common.contracts.note import NoteReader
from common.contracts.relation import RelationReader
from common.contracts.social import SocialReader
from common.domain import FeedTimelineEntry
from common.models import (
    FeedFilter,
    FeedItem,
    FeedPageResponse,
    FeedSort,
    MeditationNote,
    ReactionCount,
)
from feed_service.cursor import (
    page_entries,
    score_for_latest,
    score_for_popular,
    sort_entries,
)
from feed_service.settings import FeedServiceSettings


class FeedService:
    def __init__(
        self,
        notes: NoteReader,
        social: SocialReader,
        relations: RelationReader,
        feed_cache: FeedCacheService,
        settings: FeedServiceSettings,
    ) -> None:
        self._notes = notes
        self._social = social
        self._relations = relations
        self._cache = feed_cache
        self._settings = settings

    async def get_feed(
        self,
        viewer_id: str,
        feed_filter: FeedFilter = "all",
        sort: FeedSort = "latest",
        limit: int | None = None,
        cursor: str | None = None,
    ) -> FeedPageResponse:
        page_limit = min(limit or self._settings.feed_default_limit, self._settings.feed_max_limit)
        entries = await self._load_timeline(viewer_id, feed_filter, sort)
        page, next_cursor, has_more = page_entries(entries, cursor, page_limit)
        if not page:
            return FeedPageResponse(items=[], next_cursor=None, has_more=False)

        note_ids = [entry.note_id for entry in page]
        summaries = await self._notes.batch_summaries(viewer_id, note_ids)
        summary_by_id = {summary.id: summary for summary in summaries}
        reactions = await self._social.get_reactions_batch(note_ids, viewer_id)
        comment_counts = await self._social.get_comment_counts_batch(note_ids)

        items: list[FeedItem] = []
        for entry in page:
            summary = summary_by_id.get(entry.note_id)
            if summary is None:
                continue
            note = MeditationNote(
                id=summary.id,
                content=summary.excerpt,
                prayer_topic=None,
                emotion=summary.emotion,
                reference=summary.reference,
                visibility=summary.visibility,
                author_id=summary.author_id,
                author_name=summary.author_name,
                created_at=summary.created_at,
                updated_at=summary.created_at,
            )
            reaction_rows = reactions.get(entry.note_id, [])
            items.append(
                FeedItem(
                    note=note,
                    reactions=[
                        ReactionCount(type=reaction, count=count, reacted_by_me=reacted_by_me)
                        for reaction, count, reacted_by_me in reaction_rows
                    ],
                    comment_count=comment_counts.get(entry.note_id, 0),
                )
            )

        return FeedPageResponse(items=items, next_cursor=next_cursor, has_more=has_more)

    async def toggle_reaction(self, viewer_id: str, note_id: str, reaction) -> FeedItem:
        note = await self._notes.get_detail(viewer_id, note_id)
        await self._social.toggle_reaction(note_id, viewer_id, reaction)
        reaction_rows = (await self._social.get_reactions_batch([note_id], viewer_id)).get(note_id, [])
        comment_count = (await self._social.get_comment_counts_batch([note_id])).get(note_id, 0)
        return FeedItem(
            note=note,
            reactions=[
                ReactionCount(type=item_type, count=count, reacted_by_me=reacted_by_me)
                for item_type, count, reacted_by_me in reaction_rows
            ],
            comment_count=comment_count,
        )

    async def _load_timeline(
        self,
        viewer_id: str,
        feed_filter: FeedFilter,
        sort: FeedSort,
    ) -> list[FeedTimelineEntry]:
        cache_key = self._cache.feed_zset_key(viewer_id, feed_filter, sort)
        cached = self._cache.get_zset_entries(cache_key)
        if cached:
            return await self._entries_from_cache(cached)

        entries = await self._assemble_entries(viewer_id, feed_filter)
        if sort == "popular":
            scored = []
            for entry in entries:
                reaction_total, comment_count = await self._social.total_engagement(entry.note_id)
                score = score_for_popular(entry.created_at, reaction_total, comment_count)
                scored.append((entry.note_id, score))
            self._cache.populate_popular(viewer_id, feed_filter, scored)
        else:
            scored = [(entry.note_id, score_for_latest(entry.created_at)) for entry in entries]
            self._cache.populate_latest(viewer_id, feed_filter, scored)
        return sort_entries(entries)

    async def _entries_from_cache(self, cached: list[tuple[str, float]]) -> list[FeedTimelineEntry]:
        entries: list[FeedTimelineEntry] = []
        for note_id, _score in cached:
            entry = await self._notes.get_timeline_entry(note_id)
            if entry is not None:
                entries.append(entry)
        return sort_entries(entries)

    async def _assemble_entries(self, viewer_id: str, feed_filter: FeedFilter) -> list[FeedTimelineEntry]:
        author_ids = await self._collect_author_ids(viewer_id, feed_filter)
        author_ids = sorted(set(author_ids) - {viewer_id})
        return await self._notes.recent_entries_for_feed(viewer_id, author_ids)

    async def _collect_author_ids(self, viewer_id: str, feed_filter: FeedFilter) -> list[str]:
        membership = await self._relations.get_membership(viewer_id)
        friend_ids = await self._relations.list_friend_ids(viewer_id)

        if feed_filter == "friends":
            return friend_ids

        if feed_filter == "following":
            return await self._relations.list_following_ids(viewer_id)

        if feed_filter == "small_group":
            if not membership.group_ids:
                return []
            return await self._relations.list_group_member_ids(membership.group_ids)

        if feed_filter == "church":
            if membership.church_id is None:
                return []
            return await self._relations.list_church_member_ids(membership.church_id)

        group_member_ids: list[str] = []
        if membership.group_ids:
            group_member_ids = await self._relations.list_group_member_ids(membership.group_ids)
        church_member_ids: list[str] = []
        if membership.church_id:
            church_member_ids = await self._relations.list_church_member_ids(membership.church_id)
        return friend_ids + group_member_ids + church_member_ids
