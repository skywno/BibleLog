from __future__ import annotations

from datetime import UTC, datetime, timedelta

from common.domain import NoteRecord
from common.models import UpsertJournalNoteRequest
from note_service.repositories.note import NoteRepository, _build_record


class MemoryNoteRepository(NoteRepository):
    def __init__(self) -> None:
        self._by_id: dict[str, NoteRecord] = {}

    def create(
        self,
        author_id: str,
        request: UpsertJournalNoteRequest,
        *,
        church_id: str | None = None,
        group_ids: set[str] | None = None,
    ) -> NoteRecord:
        record = _build_record(author_id, request, church_id=church_id, group_ids=group_ids)
        self._by_id[record.note_id] = record
        return record

    def update(self, author_id: str, note_id: str, request: UpsertJournalNoteRequest) -> NoteRecord:
        existing = self._by_id.get(note_id)
        if existing is None or existing.author_id != author_id or existing.is_deleted:
            raise KeyError(note_id)
        updated = NoteRecord(
            note_id=note_id,
            author_id=author_id,
            content=request.content,
            prayer_topic=request.prayer_topic,
            emotion=request.emotion,
            reference=request.reference,
            visibility=request.visibility,
            church_id=existing.church_id,
            group_ids=existing.group_ids,
            created_at=existing.created_at,
            updated_at=datetime.now(UTC),
            deleted_at=None,
        )
        self._by_id[note_id] = updated
        return updated

    def delete(self, author_id: str, note_id: str) -> None:
        existing = self._by_id.get(note_id)
        if existing is None or existing.author_id != author_id:
            raise KeyError(note_id)
        self._by_id[note_id] = NoteRecord(
            note_id=existing.note_id,
            author_id=existing.author_id,
            content=existing.content,
            prayer_topic=existing.prayer_topic,
            emotion=existing.emotion,
            reference=existing.reference,
            visibility=existing.visibility,
            church_id=existing.church_id,
            group_ids=existing.group_ids,
            created_at=existing.created_at,
            updated_at=datetime.now(UTC),
            deleted_at=datetime.now(UTC),
        )

    def get_by_id(self, note_id: str) -> NoteRecord | None:
        return self._by_id.get(note_id)

    def list_by_author(self, author_id: str) -> list[NoteRecord]:
        return sorted(
            [
                record
                for record in self._by_id.values()
                if record.author_id == author_id and not record.is_deleted
            ],
            key=lambda record: (record.created_at, record.note_id),
            reverse=True,
        )

    def list_public_since(self, since: datetime) -> list[NoteRecord]:
        return [
            record
            for record in self._by_id.values()
            if not record.is_deleted
            and record.visibility == "public"
            and record.created_at >= since
        ]

    def list_recent_by_authors(
        self,
        author_ids: list[str],
        since: datetime | None,
        limit_per_author: int,
    ) -> list[NoteRecord]:
        records: list[NoteRecord] = []
        for author_id in author_ids:
            author_notes = [
                record
                for record in self._by_id.values()
                if record.author_id == author_id and not record.is_deleted
            ]
            author_notes.sort(key=lambda record: (record.created_at, record.note_id), reverse=True)
            if since:
                author_notes = [note for note in author_notes if note.created_at >= since]
            records.extend(author_notes[:limit_per_author])
        return records

    def seed_demo(self, author_id: str) -> None:
        if self._by_id:
            return
        now = datetime.now(UTC)
        demo = UpsertJournalNoteRequest(
            content="오늘 말씀을 읽으며 하나님의 사랑을 다시금 깨달았습니다.",
            prayer_topic="가정의 평안",
            emotion="gratitude",
            visibility="public",
        )
        record = _build_record(author_id, demo, created_at=now - timedelta(hours=2))
        self._by_id[record.note_id] = record
