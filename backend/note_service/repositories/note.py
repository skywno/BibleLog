from __future__ import annotations

import json
import uuid
from abc import ABC, abstractmethod
from datetime import UTC, datetime

from common.domain import NoteRecord
from common.models import BibleReference, Emotion, NoteVisibility, UpsertJournalNoteRequest


class NoteRepository(ABC):
    @abstractmethod
    def create(
        self,
        author_id: str,
        request: UpsertJournalNoteRequest,
        *,
        church_id: str | None = None,
        group_ids: set[str] | None = None,
    ) -> NoteRecord: ...

    @abstractmethod
    def update(
        self,
        author_id: str,
        note_id: str,
        request: UpsertJournalNoteRequest,
    ) -> NoteRecord: ...

    @abstractmethod
    def delete(self, author_id: str, note_id: str) -> None: ...

    @abstractmethod
    def get_by_id(self, note_id: str) -> NoteRecord | None: ...

    @abstractmethod
    def list_by_author(self, author_id: str) -> list[NoteRecord]: ...

    @abstractmethod
    def list_public_since(self, since: datetime) -> list[NoteRecord]: ...

    @abstractmethod
    def list_recent_by_authors(
        self,
        author_ids: list[str],
        since: datetime | None,
        limit_per_author: int,
    ) -> list[NoteRecord]: ...


def _reference_to_json(reference: BibleReference | None) -> str | None:
    if reference is None:
        return None
    return reference.model_dump_json()


def _reference_from_json(raw: str | None) -> BibleReference | None:
    if not raw:
        return None
    return BibleReference.model_validate_json(raw)


def _record_from_row(row: dict) -> NoteRecord:
    group_ids = row.get("group_ids") or set()
    if isinstance(group_ids, list):
        group_ids = set(group_ids)
    return NoteRecord(
        note_id=row["note_id"],
        author_id=row["author_id"],
        content=row["content"],
        prayer_topic=row.get("prayer_topic"),
        emotion=row.get("emotion"),  # type: ignore[arg-type]
        reference=_reference_from_json(row.get("reference")),
        visibility=row["visibility"],  # type: ignore[arg-type]
        church_id=row.get("church_id"),
        group_ids=group_ids,
        created_at=row["created_at"],
        updated_at=row["updated_at"],
        deleted_at=row.get("deleted_at"),
    )


def _bucket(created_at: datetime) -> str:
    return created_at.strftime("%Y-%m")


def _build_record(
    author_id: str,
    request: UpsertJournalNoteRequest,
    note_id: str | None = None,
    created_at: datetime | None = None,
    *,
    church_id: str | None = None,
    group_ids: set[str] | None = None,
) -> NoteRecord:
    now = datetime.now(UTC)
    visibility = request.visibility
    resolved_church_id = church_id
    resolved_group_ids = set(group_ids or set())
    if visibility == "church":
        resolved_group_ids = set()
    elif visibility == "small_group":
        resolved_church_id = None
    elif visibility not in {"church", "small_group"}:
        resolved_church_id = None
        resolved_group_ids = set()
    return NoteRecord(
        note_id=note_id or str(uuid.uuid4()),
        author_id=author_id,
        content=request.content,
        prayer_topic=request.prayer_topic,
        emotion=request.emotion,
        reference=request.reference,
        visibility=request.visibility,
        church_id=resolved_church_id,
        group_ids=resolved_group_ids,
        created_at=created_at or now,
        updated_at=now,
        deleted_at=None,
    )
