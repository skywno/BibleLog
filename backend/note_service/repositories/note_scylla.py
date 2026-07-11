from __future__ import annotations

from datetime import UTC, datetime

from cassandra.cluster import Session

from common.db.scylla import scylla_timestamp, traced_execute

from common.domain import NoteRecord
from common.models import UpsertJournalNoteRequest
from note_service.repositories.note import (
    NoteRepository,
    _bucket,
    _build_record,
    _record_from_row,
    _reference_to_json,
)


class ScyllaNoteRepository(NoteRepository):
    def __init__(self, session: Session) -> None:
        self._session = session

    def create(
        self,
        author_id: str,
        request: UpsertJournalNoteRequest,
        *,
        church_id: str | None = None,
        group_ids: set[str] | None = None,
    ) -> NoteRecord:
        record = _build_record(author_id, request, church_id=church_id, group_ids=group_ids)
        self._write(record)
        return record

    def update(self, author_id: str, note_id: str, request: UpsertJournalNoteRequest) -> NoteRecord:
        existing = self.get_by_id(note_id)
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
        self._write(updated)
        return updated

    def delete(self, author_id: str, note_id: str) -> None:
        existing = self.get_by_id(note_id)
        if existing is None or existing.author_id != author_id:
            raise KeyError(note_id)
        deleted = NoteRecord(
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
        self._write(deleted)

    def get_by_id(self, note_id: str) -> NoteRecord | None:
        row = traced_execute(self._session,
            "SELECT * FROM notes_by_id WHERE note_id = %s",
            (note_id,),
        ).one()
        if row is None:
            return None
        data = row._asdict()
        data["created_at"] = scylla_timestamp(data["created_at"])
        data["updated_at"] = scylla_timestamp(data["updated_at"])
        if data.get("deleted_at") is not None:
            data["deleted_at"] = scylla_timestamp(data["deleted_at"])
        return _record_from_row(data)

    def list_by_author(self, author_id: str) -> list[NoteRecord]:
        rows = traced_execute(self._session,
            """
            SELECT note_id, author_id, content, prayer_topic, emotion, visibility,
                   church_id, group_ids, reference, created_at, updated_at, deleted_at
            FROM notes_by_author
            WHERE author_id = %s
            """,
            (author_id,),
        )
        records: list[NoteRecord] = []
        for row in rows:
            data = row._asdict()
            data["created_at"] = scylla_timestamp(data["created_at"])
            data["updated_at"] = scylla_timestamp(data["updated_at"])
            if data.get("deleted_at") is not None:
                data["deleted_at"] = scylla_timestamp(data["deleted_at"])
            record = _record_from_row(data)
            if record.is_deleted:
                continue
            records.append(record)
        return records

    def list_public_since(self, since: datetime) -> list[NoteRecord]:
        buckets = {_bucket(since), _bucket(datetime.now(UTC))}
        records: list[NoteRecord] = []
        for bucket in buckets:
            rows = traced_execute(self._session,
                """
                SELECT note_id FROM notes_by_visibility
                WHERE visibility = %s AND bucket = %s AND created_at >= %s
                """,
                ("public", bucket, since),
            )
            for row in rows:
                record = self.get_by_id(row._asdict()["note_id"])
                if record and not record.is_deleted:
                    records.append(record)
        return records

    def list_recent_by_authors(
        self,
        author_ids: list[str],
        since: datetime | None,
        limit_per_author: int,
    ) -> list[NoteRecord]:
        records: list[NoteRecord] = []
        for author_id in author_ids:
            rows = traced_execute(self._session,
                """
                SELECT note_id, author_id, content, prayer_topic, emotion, visibility,
                       church_id, group_ids, reference, created_at, updated_at, deleted_at
                FROM notes_by_author
                WHERE author_id = %s
                LIMIT %s
                """,
                (author_id, limit_per_author),
            )
            for row in rows:
                data = row._asdict()
                data["created_at"] = scylla_timestamp(data["created_at"])
                data["updated_at"] = scylla_timestamp(data["updated_at"])
                if data.get("deleted_at") is not None:
                    data["deleted_at"] = scylla_timestamp(data["deleted_at"])
                record = _record_from_row(data)
                if record.is_deleted:
                    continue
                if since and record.created_at < since:
                    continue
                records.append(record)
        return records

    def _write(self, record: NoteRecord) -> None:
        reference = _reference_to_json(record.reference)
        traced_execute(self._session,
            """
            INSERT INTO notes_by_id (
                note_id, author_id, created_at, content, prayer_topic, emotion,
                visibility, church_id, group_ids, reference, updated_at, deleted_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                record.note_id,
                record.author_id,
                record.created_at,
                record.content,
                record.prayer_topic,
                record.emotion,
                record.visibility,
                record.church_id,
                record.group_ids,
                reference,
                record.updated_at,
                record.deleted_at,
            ),
        )
        traced_execute(self._session,
            """
            INSERT INTO notes_by_author (
                author_id, created_at, note_id, content, prayer_topic, emotion,
                visibility, church_id, group_ids, reference, updated_at, deleted_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                record.author_id,
                record.created_at,
                record.note_id,
                record.content,
                record.prayer_topic,
                record.emotion,
                record.visibility,
                record.church_id,
                record.group_ids,
                reference,
                record.updated_at,
                record.deleted_at,
            ),
        )
        if record.visibility != "private" and record.deleted_at is None:
            traced_execute(self._session,
                """
                INSERT INTO notes_by_visibility (
                    visibility, bucket, created_at, note_id, author_id
                ) VALUES (%s, %s, %s, %s, %s)
                """,
                (
                    record.visibility,
                    _bucket(record.created_at),
                    record.created_at,
                    record.note_id,
                    record.author_id,
                ),
            )
