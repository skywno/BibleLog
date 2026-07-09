from __future__ import annotations

from datetime import UTC, datetime, timedelta

from shared.config import Settings
from shared.domain import FeedTimelineEntry, NoteRecord
from shared.events.bus import event_bus
from shared.models import (
    MeditationNote,
    NoteSummary,
    NoteVisibility,
    UpsertJournalNoteRequest,
)
from note_service.repositories.note import NoteRepository
from user_service.repositories.relation import RelationRepository
from user_service.repositories.user import UserRepository
from feed_service.cache import FeedCacheService


class NoteService:
    def __init__(
        self,
        notes: NoteRepository,
        users: UserRepository,
        relations: RelationRepository,
        feed_cache: FeedCacheService,
        settings: Settings,
    ) -> None:
        self._notes = notes
        self._users = users
        self._relations = relations
        self._feed_cache = feed_cache
        self._settings = settings
        self._register_events()

    def _register_events(self) -> None:
        event_bus.subscribe("NotePublished", self._on_note_changed)
        event_bus.subscribe("NoteUpdated", self._on_note_changed)
        event_bus.subscribe("NoteDeleted", self._on_note_deleted)

    def _on_note_changed(self, payload: dict) -> None:
        note_id = payload["note_id"]
        self._feed_cache.invalidate_note_summary(note_id)
        self._feed_cache.invalidate_all_feeds()

    def _on_note_deleted(self, payload: dict) -> None:
        note_id = payload["note_id"]
        self._feed_cache.invalidate_note_summary(note_id)
        self._feed_cache.remove_note_from_feeds(note_id)

    def create(self, author_id: str, request: UpsertJournalNoteRequest) -> MeditationNote:
        record = self._notes.create(author_id, request)
        event_bus.publish(
            "NotePublished",
            {"note_id": record.note_id, "author_id": author_id, "visibility": record.visibility},
        )
        return self._to_meditation_note(record)

    def update(
        self,
        author_id: str,
        note_id: str,
        request: UpsertJournalNoteRequest,
    ) -> MeditationNote:
        record = self._notes.update(author_id, note_id, request)
        event_bus.publish("NoteUpdated", {"note_id": note_id, "author_id": author_id})
        return self._to_meditation_note(record)

    def delete(self, author_id: str, note_id: str) -> None:
        self._notes.delete(author_id, note_id)
        event_bus.publish("NoteDeleted", {"note_id": note_id, "author_id": author_id})

    def get_detail(self, viewer_id: str, note_id: str) -> MeditationNote:
        record = self._notes.get_by_id(note_id)
        if record is None or record.is_deleted:
            raise KeyError(note_id)
        if not self._can_view(viewer_id, record):
            raise PermissionError(note_id)
        return self._to_meditation_note(record)

    def list_mine(self, author_id: str) -> list[MeditationNote]:
        return [self._to_meditation_note(record) for record in self._notes.list_by_author(author_id)]

    def batch_summaries(self, viewer_id: str, note_ids: list[str]) -> list[NoteSummary]:
        summaries: list[NoteSummary] = []
        for note_id in note_ids:
            cached = self._feed_cache.get_note_summary(note_id)
            if cached is not None:
                summaries.append(cached)
                continue
            record = self._notes.get_by_id(note_id)
            if record is None or record.is_deleted or not self._can_view(viewer_id, record):
                continue
            summary = self._to_summary(record)
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

    def recent_entries_for_feed(
        self,
        viewer_id: str,
        author_ids: list[str],
        *,
        since_days: int = 30,
        since: datetime | None = None,
        limit_per_author: int = 10,
    ) -> list[FeedTimelineEntry]:
        effective_since = since if since is not None else datetime.now(UTC) - timedelta(days=since_days)
        records = self._notes.list_recent_by_authors(author_ids, effective_since, limit_per_author)
        public_records = self._notes.list_public_since(effective_since)
        merged = {record.note_id: record for record in records + public_records}
        entries: list[FeedTimelineEntry] = []
        for record in merged.values():
            if record.is_deleted or not self._can_view(viewer_id, record):
                continue
            entries.append(self._to_timeline_entry(record))
        return entries

    def _can_view(self, viewer_id: str, record: NoteRecord) -> bool:
        if record.author_id == viewer_id:
            return True
        visibility = record.visibility
        if visibility == "private":
            return False
        if visibility == "public":
            return True
        if visibility == "friends":
            return viewer_id in self._relations.list_friend_ids(record.author_id)
        membership = self._relations.get_membership(viewer_id)
        if visibility == "church":
            return (
                record.church_id is not None
                and membership.church_id is not None
                and record.church_id == membership.church_id
            )
        if visibility == "small_group":
            return bool(record.group_ids & membership.group_ids)
        return False

    def _to_meditation_note(self, record: NoteRecord) -> MeditationNote:
        author = self._users.get_user(record.author_id)
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

    def _to_summary(self, record: NoteRecord) -> NoteSummary:
        author = self._users.get_user(record.author_id)
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
