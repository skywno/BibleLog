from __future__ import annotations

from datetime import UTC, datetime, timedelta

from common.cache.feed_cache import FeedCacheService
from common.clients.relation import HttpRelationClient
from common.clients.user import HttpUserClient
from common.domain import FeedTimelineEntry, NoteRecord
from common.events.kafka_bus import KafkaEventBus
from common.models import (
    MeditationNote,
    NoteSummary,
    NoteVisibility,
    UpsertJournalNoteRequest,
)
from note_service.repositories.note import NoteRepository
from note_service.settings import NoteServiceSettings


class NoteService:
    def __init__(
        self,
        notes: NoteRepository,
        users: HttpUserClient,
        relations: HttpRelationClient,
        feed_cache: FeedCacheService,
        settings: NoteServiceSettings,
        event_bus: KafkaEventBus,
    ) -> None:
        self._notes = notes
        self._users = users
        self._relations = relations
        self._feed_cache = feed_cache
        self._settings = settings
        self._event_bus = event_bus

    async def create(self, author_id: str, request: UpsertJournalNoteRequest) -> MeditationNote:
        membership = await self._relations.get_membership(author_id)
        church_id = membership.church_id if request.visibility == "church" else None
        group_ids = membership.group_ids if request.visibility == "small_group" else set()
        record = self._notes.create(
            author_id,
            request,
            church_id=church_id,
            group_ids=group_ids,
        )
        await self._event_bus.publish(
            "NotePublished",
            {"note_id": record.note_id, "author_id": author_id, "visibility": record.visibility},
        )
        return await self._to_meditation_note(record)

    async def update(
        self,
        author_id: str,
        note_id: str,
        request: UpsertJournalNoteRequest,
    ) -> MeditationNote:
        record = self._notes.update(author_id, note_id, request)
        await self._event_bus.publish("NoteUpdated", {"note_id": note_id, "author_id": author_id})
        return await self._to_meditation_note(record)

    async def delete(self, author_id: str, note_id: str) -> None:
        self._notes.delete(author_id, note_id)
        await self._event_bus.publish("NoteDeleted", {"note_id": note_id, "author_id": author_id})

    async def get_detail(self, viewer_id: str, note_id: str) -> MeditationNote:
        record = self._notes.get_by_id(note_id)
        if record is None or record.is_deleted:
            raise KeyError(note_id)
        if not await self._can_view(viewer_id, record):
            raise PermissionError(note_id)
        return await self._to_meditation_note(record)

    async def list_mine(self, author_id: str) -> list[MeditationNote]:
        records = self._notes.list_by_author(author_id)
        return [await self._to_meditation_note(record) for record in records]

    async def list_for_viewer(self, viewer_id: str, author_id: str) -> list[MeditationNote]:
        records = self._notes.list_by_author(author_id)
        notes: list[MeditationNote] = []
        for record in records:
            if record.is_deleted:
                continue
            if not await self._can_view(viewer_id, record):
                continue
            notes.append(await self._to_meditation_note(record))
        return notes

    async def batch_summaries(self, viewer_id: str, note_ids: list[str]) -> list[NoteSummary]:
        summaries: list[NoteSummary] = []
        for note_id in note_ids:
            cached = self._feed_cache.get_note_summary(note_id)
            if cached is not None:
                summaries.append(cached)
                continue
            record = self._notes.get_by_id(note_id)
            if record is None or record.is_deleted or not await self._can_view(viewer_id, record):
                continue
            summary = await self._to_summary(record)
            self._feed_cache.set_note_summary(summary)
            summaries.append(summary)
        id_order = {note_id: index for index, note_id in enumerate(note_ids)}
        summaries.sort(key=lambda item: id_order.get(item.id, len(note_ids)))
        return summaries

    def get_timeline_entry(self, note_id: str) -> FeedTimelineEntry | None:
        record = self._notes.get_by_id(note_id)
        if record is None or record.is_deleted:
            return None
        return self._to_timeline_entry(record)

    async def recent_entries_for_feed(
        self,
        viewer_id: str,
        author_ids: list[str],
        *,
        since_days: int = 30,
        since: datetime | None = None,
        limit_per_author: int = 10,
        include_global_public: bool = False,
    ) -> list[FeedTimelineEntry]:
        effective_since = since if since is not None else datetime.now(UTC) - timedelta(days=since_days)
        if not author_ids and not include_global_public:
            return []

        records: list[NoteRecord] = []
        if author_ids:
            records = self._notes.list_recent_by_authors(author_ids, effective_since, limit_per_author)
        if include_global_public:
            records = list(
                {record.note_id: record for record in records + self._notes.list_public_since(effective_since)}.values()
            )

        entries: list[FeedTimelineEntry] = []
        for record in records:
            if record.is_deleted or not await self._can_view(viewer_id, record):
                continue
            entries.append(self._to_timeline_entry(record))
        return entries

    async def _can_view(self, viewer_id: str, record: NoteRecord) -> bool:
        if record.author_id == viewer_id:
            return True
        visibility = record.visibility
        if visibility == "private":
            return False
        if visibility == "public":
            return True
        if visibility == "friends":
            return viewer_id in await self._relations.list_friend_ids(record.author_id)
        membership = await self._relations.get_membership(viewer_id)
        if visibility == "church":
            return (
                record.church_id is not None
                and membership.church_id is not None
                and record.church_id == membership.church_id
            )
        if visibility == "small_group":
            return bool(record.group_ids & membership.group_ids)
        return False

    async def _to_meditation_note(self, record: NoteRecord) -> MeditationNote:
        author = await self._users.get_user(record.author_id)
        return MeditationNote(
            id=record.note_id,
            content=record.content,
            prayer_topic=record.prayer_topic,
            emotion=record.emotion,
            reference=record.reference,
            visibility=record.visibility,
            author_id=record.author_id,
            author_name=author.nickname,
            created_at=record.created_at,
            updated_at=record.updated_at,
        )

    async def _to_summary(self, record: NoteRecord) -> NoteSummary:
        author = await self._users.get_user(record.author_id)
        excerpt = record.content[: self._settings.note_excerpt_length]
        if len(record.content) > self._settings.note_excerpt_length:
            excerpt += "..."
        return NoteSummary(
            id=record.note_id,
            author_id=record.author_id,
            author_name=author.nickname,
            excerpt=excerpt,
            visibility=record.visibility,
            emotion=record.emotion,
            reference=record.reference,
            created_at=record.created_at,
        )

    def _to_timeline_entry(self, record: NoteRecord) -> FeedTimelineEntry:
        return FeedTimelineEntry(
            note_id=record.note_id,
            author_id=record.author_id,
            created_at=record.created_at,
            visibility=record.visibility,
            church_id=record.church_id,
            group_ids=record.group_ids,
        )
