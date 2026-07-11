from __future__ import annotations

from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, Field


class BibleReference(BaseModel):
    book_id: int = Field(ge=1, le=66)
    start_chapter: int
    start_verse: int
    end_chapter: int
    end_verse: int


class UserProfile(BaseModel):
    id: str
    nickname: str
    bio: str = ""
    is_logged_in: bool = True


class UpdateUserProfileRequest(BaseModel):
    nickname: str | None = None
    bio: str | None = None


class OAuthAuthorizeResponse(BaseModel):
    authorization_url: str
    state: str


class AuthTokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserProfile


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class ReadingRecord(BaseModel):
    id: str
    date: date
    reference: BibleReference
    minutes_read: int
    created_at: datetime


class CreateReadingRecordRequest(BaseModel):
    date: date
    reference: BibleReference
    minutes_read: int


class ReadingProgress(BaseModel):
    overall: float
    old_testament: float
    new_testament: float
    by_book: dict[str, float]


class ReadingStats(BaseModel):
    total_minutes: int
    average_daily_minutes: float
    current_streak: int
    best_streak: int
    monthly_reading_days: dict[int, int] = Field(default_factory=dict)


Emotion = Literal["gratitude", "joy", "peace", "sadness", "moved"]
NoteVisibility = Literal["public", "friends", "small_group", "church", "private"]
FaithReaction = Literal["empathy", "pray_together", "amen", "grace"]
FeedFilter = Literal["all", "small_group", "church", "friends"]
FeedSort = Literal["latest", "popular"]
AiConversationMode = Literal["chat", "prayer"]

FAITH_REACTIONS: tuple[FaithReaction, ...] = (
    "empathy",
    "pray_together",
    "amen",
    "grace",
)


class MeditationNote(BaseModel):
    id: str
    content: str
    prayer_topic: str | None = None
    emotion: Emotion | None = None
    reference: BibleReference | None = None
    visibility: NoteVisibility
    author_id: str
    author_name: str
    created_at: datetime
    updated_at: datetime


class NoteSummary(BaseModel):
    id: str
    author_id: str
    author_name: str
    excerpt: str
    visibility: NoteVisibility
    emotion: Emotion | None = None
    reference: BibleReference | None = None
    created_at: datetime


class UpsertJournalNoteRequest(BaseModel):
    content: str
    prayer_topic: str | None = None
    emotion: Emotion | None = None
    reference: BibleReference | None = None
    visibility: NoteVisibility


class ReactionCount(BaseModel):
    type: FaithReaction
    count: int
    reacted_by_me: bool


class FeedItem(BaseModel):
    note: MeditationNote
    reactions: list[ReactionCount]
    comment_count: int


class FeedPageResponse(BaseModel):
    items: list[FeedItem]
    next_cursor: str | None = None
    has_more: bool = False


class ToggleReactionRequest(BaseModel):
    reaction: FaithReaction


class NotesBatchRequest(BaseModel):
    note_ids: list[str]
    viewer_id: str


class NotesBatchResponse(BaseModel):
    notes: list[NoteSummary]


class RecentNotesByAuthorsRequest(BaseModel):
    author_ids: list[str]
    viewer_id: str
    since: datetime | None = None
    limit_per_author: int = 10


class RecentNoteEntry(BaseModel):
    note_id: str
    author_id: str
    created_at: datetime
    visibility: NoteVisibility
    church_id: str | None = None
    group_ids: set[str] = Field(default_factory=set)


class RecentNotesByAuthorsResponse(BaseModel):
    entries: list[RecentNoteEntry]


class ReactionsBatchRequest(BaseModel):
    note_ids: list[str]
    viewer_id: str


class NoteReactions(BaseModel):
    note_id: str
    reactions: list[ReactionCount]


class ReactionsBatchResponse(BaseModel):
    items: list[NoteReactions]


class CommentCountBatchRequest(BaseModel):
    note_ids: list[str]


class NoteCommentCount(BaseModel):
    note_id: str
    count: int


class CommentCountBatchResponse(BaseModel):
    items: list[NoteCommentCount]


class AiConversationSummary(BaseModel):
    id: str
    mode: AiConversationMode
    title: str | None = None
    created_at: datetime
    updated_at: datetime


class AiMessage(BaseModel):
    id: str
    content: str
    is_from_user: bool
    timestamp: datetime
    suggested_reference: BibleReference | None = None


class SendAiMessageRequest(BaseModel):
    content: str
    mode: AiConversationMode = "chat"


class SendAiMessageResponse(BaseModel):
    user_message: AiMessage
    assistant_message: AiMessage
    provider: str
