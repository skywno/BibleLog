from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from common.models import BibleReference, Emotion, FaithReaction, NoteVisibility


@dataclass
class NoteRecord:
    note_id: str
    author_id: str
    content: str
    prayer_topic: str | None
    emotion: Emotion | None
    reference: BibleReference | None
    visibility: NoteVisibility
    church_id: str | None
    group_ids: set[str]
    created_at: datetime
    updated_at: datetime
    deleted_at: datetime | None = None

    @property
    def is_deleted(self) -> bool:
        return self.deleted_at is not None


@dataclass
class FeedTimelineEntry:
    note_id: str
    author_id: str
    created_at: datetime
    visibility: NoteVisibility
    church_id: str | None = None
    group_ids: set[str] | None = None


@dataclass
class ReactionRecord:
    note_id: str
    user_id: str
    reaction_type: FaithReaction
    created_at: datetime


@dataclass
class UserMembership:
    church_id: str | None
    group_ids: set[str]
