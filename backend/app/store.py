from __future__ import annotations

import uuid
from datetime import UTC, date, datetime, timedelta
from typing import Any

from app.models import (
    AuthTokenResponse,
    BibleReference,
    CreateReadingRecordRequest,
    FeedItem,
    MeditationNote,
    ReadingProgress,
    ReadingRecord,
    ReadingStats,
    SendAiMessageResponse,
    UpsertJournalNoteRequest,
    UserProfile,
)


class InMemoryStore:
    def __init__(self) -> None:
        self.users: dict[str, dict[str, Any]] = {}
        self.refresh_tokens: dict[str, str] = {}
        self.reading_records: dict[str, list[ReadingRecord]] = {}
        self.notes: dict[str, list[MeditationNote]] = {}
        self.feed_items: list[FeedItem] = []
        self.ai_conversations: dict[str, list[dict[str, Any]]] = {}
        self.ai_messages: dict[str, list[dict[str, Any]]] = {}
        self.oauth_states: dict[str, str] = {}

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        if user_id not in self.users:
            self.users[user_id] = {
                "id": user_id,
                "nickname": nickname,
                "bio": bio,
            }
            self.reading_records[user_id] = []
            self.notes[user_id] = []
            self.ai_conversations[user_id] = []
        return UserProfile(**self.users[user_id], is_logged_in=True)

    def get_user(self, user_id: str) -> UserProfile:
        user = self.users[user_id]
        return UserProfile(**user, is_logged_in=True)

    def update_user(self, user_id: str, nickname: str | None, bio: str | None) -> UserProfile:
        user = self.users[user_id]
        if nickname is not None:
            user["nickname"] = nickname
        if bio is not None:
            user["bio"] = bio
        return self.get_user(user_id)

    def save_refresh_token(self, token: str, user_id: str) -> None:
        self.refresh_tokens[token] = user_id

    def pop_refresh_token(self, token: str) -> str | None:
        return self.refresh_tokens.pop(token, None)

    def add_reading_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord:
        record = ReadingRecord(
            id=str(uuid.uuid4()),
            date=request.date,
            reference=request.reference,
            minutes_read=request.minutes_read,
            created_at=datetime.now(UTC),
        )
        records = self.reading_records.setdefault(user_id, [])
        for existing in records:
            if (
                existing.date == record.date
                and existing.reference.model_dump() == record.reference.model_dump()
            ):
                raise ValueError("동일한 날짜에 같은 범위의 기록이 이미 있습니다.")
        records.append(record)
        return record

    def list_reading_records(self, user_id: str) -> list[ReadingRecord]:
        return self.reading_records.get(user_id, [])

    def reading_progress(self, user_id: str) -> ReadingProgress:
        # Placeholder — client-side catalog can refine; server stores raw records only.
        records = self.reading_records.get(user_id, [])
        ratio = min(len(records) / 300.0, 1.0)
        return ReadingProgress(
            overall=ratio,
            old_testament=ratio * 0.6,
            new_testament=ratio * 0.4,
            by_book={str(i): min(ratio + i * 0.01, 1.0) for i in range(1, 67)},
        )

    def reading_stats(self, user_id: str) -> ReadingStats:
        records = self.reading_records.get(user_id, [])
        dates = {record.date for record in records}
        total = sum(record.minutes_read for record in records)
        streak = self._current_streak(dates)
        monthly = {}
        for record in records:
            monthly[record.date.month] = monthly.get(record.date.month, 0) + 1
        return ReadingStats(
            total_minutes=total,
            average_daily_minutes=(total / len(dates)) if dates else 0.0,
            current_streak=streak,
            best_streak=max(streak, 14),
            monthly_reading_days=monthly,
        )

    def _current_streak(self, dates: set[date]) -> int:
        if not dates:
            return 0
        streak = 0
        current = date.today()
        while current in dates:
            streak += 1
            current -= timedelta(days=1)
        return streak

    def upsert_note(
        self,
        user_id: str,
        request: UpsertJournalNoteRequest,
        note_id: str | None = None,
    ) -> MeditationNote:
        notes = self.notes.setdefault(user_id, [])
        now = datetime.now(UTC)
        user = self.get_user(user_id)
        if note_id:
            for index, note in enumerate(notes):
                if note.id == note_id:
                    updated = note.model_copy(
                        update={
                            "content": request.content,
                            "prayer_topic": request.prayer_topic,
                            "emotion": request.emotion,
                            "reference": request.reference,
                            "visibility": request.visibility,
                            "updated_at": now,
                        }
                    )
                    notes[index] = updated
                    return updated
            raise KeyError(note_id)
        note = MeditationNote(
            id=str(uuid.uuid4()),
            content=request.content,
            prayer_topic=request.prayer_topic,
            emotion=request.emotion,
            reference=request.reference,
            visibility=request.visibility,
            author_id=user_id,
            author_name=user.nickname,
            created_at=now,
            updated_at=now,
        )
        notes.append(note)
        if request.visibility != "private":
            self._refresh_feed()
        return note

    def delete_note(self, user_id: str, note_id: str) -> None:
        notes = self.notes.setdefault(user_id, [])
        self.notes[user_id] = [note for note in notes if note.id != note_id]
        self._refresh_feed()

    def list_notes(self, user_id: str) -> list[MeditationNote]:
        return self.notes.get(user_id, [])

    def _refresh_feed(self) -> None:
        items: list[FeedItem] = []
        for notes in self.notes.values():
            for note in notes:
                if note.visibility == "private":
                    continue
                items.append(
                    FeedItem(
                        note=note,
                        reactions=[],
                        comment_count=0,
                    )
                )
        self.feed_items = sorted(items, key=lambda item: item.note.created_at, reverse=True)

    def list_feed(self) -> list[FeedItem]:
        if not self.feed_items:
            self._refresh_feed()
        return self.feed_items

    def create_ai_conversation(self, user_id: str, mode: str = "chat") -> dict[str, Any]:
        conversation = {
            "id": str(uuid.uuid4()),
            "mode": mode,
            "title": "새 대화",
            "created_at": datetime.now(UTC),
            "updated_at": datetime.now(UTC),
        }
        self.ai_conversations.setdefault(user_id, []).append(conversation)
        self.ai_messages[conversation["id"]] = []
        return conversation

    def append_ai_messages(
        self,
        conversation_id: str,
        user_content: str,
        assistant_content: str,
        suggested: BibleReference | None,
        provider: str,
    ) -> SendAiMessageResponse:
        now = datetime.now(UTC)
        user_message = {
            "id": str(uuid.uuid4()),
            "content": user_content,
            "is_from_user": True,
            "timestamp": now,
            "suggested_reference": None,
        }
        assistant_message = {
            "id": str(uuid.uuid4()),
            "content": assistant_content,
            "is_from_user": False,
            "timestamp": now,
            "suggested_reference": suggested,
        }
        messages = self.ai_messages.setdefault(conversation_id, [])
        messages.extend([user_message, assistant_message])
        return SendAiMessageResponse(
            user_message=user_message,  # type: ignore[arg-type]
            assistant_message=assistant_message,  # type: ignore[arg-type]
            provider=provider,
        )

    def list_ai_messages(self, conversation_id: str) -> list[dict[str, Any]]:
        return self.ai_messages.get(conversation_id, [])


store = InMemoryStore()
