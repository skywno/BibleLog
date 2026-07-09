from __future__ import annotations

import json
import uuid
from datetime import UTC, datetime

import psycopg

from shared.models import AiMessage, BibleReference, SendAiMessageResponse, UserProfile
from user_service.repositories.user import UserRepository


class PostgresUserRepository(UserRepository):
    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO users (id, nickname, bio)
                VALUES (%s, %s, %s)
                ON CONFLICT (id) DO NOTHING
                """,
                (user_id, nickname, bio),
            )
            cur.execute(
                """
                INSERT INTO user_memberships (user_id, church_id, group_ids)
                VALUES (%s, NULL, '{}')
                ON CONFLICT (user_id) DO NOTHING
                """,
                (user_id,),
            )
        self._conn.commit()
        return self.get_user(user_id)

    def get_user(self, user_id: str) -> UserProfile:
        with self._conn.cursor() as cur:
            cur.execute("SELECT id, nickname, bio FROM users WHERE id = %s", (user_id,))
            row = cur.fetchone()
        if row is None:
            raise KeyError(user_id)
        return UserProfile(**row, is_logged_in=True)

    def update_user(
        self,
        user_id: str,
        nickname: str | None,
        bio: str | None,
    ) -> UserProfile:
        with self._conn.cursor() as cur:
            if nickname is not None:
                cur.execute("UPDATE users SET nickname = %s WHERE id = %s", (nickname, user_id))
            if bio is not None:
                cur.execute("UPDATE users SET bio = %s WHERE id = %s", (bio, user_id))
        self._conn.commit()
        return self.get_user(user_id)

    def save_refresh_token(self, token: str, user_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "INSERT INTO refresh_tokens (token, user_id) VALUES (%s, %s)",
                (token, user_id),
            )
        self._conn.commit()

    def pop_refresh_token(self, token: str) -> str | None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM refresh_tokens WHERE token = %s RETURNING user_id",
                (token,),
            )
            row = cur.fetchone()
        self._conn.commit()
        return row["user_id"] if row else None

    def save_oauth_state(self, state: str, redirect_uri: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "INSERT INTO oauth_states (state, redirect_uri) VALUES (%s, %s)",
                (state, redirect_uri),
            )
        self._conn.commit()

    def pop_oauth_state(self, state: str) -> str | None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM oauth_states WHERE state = %s RETURNING redirect_uri",
                (state,),
            )
            row = cur.fetchone()
        self._conn.commit()
        return row["redirect_uri"] if row else None

    def create_ai_conversation(self, user_id: str, mode: str = "chat") -> dict:
        now = datetime.now(UTC)
        conversation = {
            "id": str(uuid.uuid4()),
            "mode": mode,
            "title": "새 대화",
            "created_at": now,
            "updated_at": now,
        }
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO ai_conversations (id, user_id, mode, title, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s)
                """,
                (
                    conversation["id"],
                    user_id,
                    conversation["mode"],
                    conversation["title"],
                    conversation["created_at"],
                    conversation["updated_at"],
                ),
            )
        self._conn.commit()
        return conversation

    def list_ai_conversations(self, user_id: str) -> list[dict]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, mode, title, created_at, updated_at
                FROM ai_conversations
                WHERE user_id = %s
                ORDER BY updated_at DESC
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [dict(row) for row in rows]

    def list_ai_messages(self, conversation_id: str) -> list[dict]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, content, is_from_user, timestamp, suggested_reference
                FROM ai_messages
                WHERE conversation_id = %s
                ORDER BY timestamp ASC
                """,
                (conversation_id,),
            )
            rows = cur.fetchall()
        messages = []
        for row in rows:
            suggested = row["suggested_reference"]
            if suggested and isinstance(suggested, str):
                suggested = json.loads(suggested)
            messages.append(
                {
                    "id": row["id"],
                    "content": row["content"],
                    "is_from_user": row["is_from_user"],
                    "timestamp": row["timestamp"],
                    "suggested_reference": suggested,
                }
            )
        return messages

    def append_ai_messages(
        self,
        conversation_id: str,
        user_content: str,
        assistant_content: str,
        suggested: BibleReference | None,
        provider: str,
    ) -> SendAiMessageResponse:
        now = datetime.now(UTC)
        user_message = AiMessage(
            id=str(uuid.uuid4()),
            content=user_content,
            is_from_user=True,
            timestamp=now,
            suggested_reference=None,
        )
        assistant_message = AiMessage(
            id=str(uuid.uuid4()),
            content=assistant_content,
            is_from_user=False,
            timestamp=now,
            suggested_reference=suggested,
        )
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO ai_messages (
                    id, conversation_id, content, is_from_user, timestamp, suggested_reference
                ) VALUES (%s, %s, %s, %s, %s, %s)
                """,
                (
                    user_message.id,
                    conversation_id,
                    user_message.content,
                    True,
                    user_message.timestamp,
                    None,
                ),
            )
            cur.execute(
                """
                INSERT INTO ai_messages (
                    id, conversation_id, content, is_from_user, timestamp, suggested_reference
                ) VALUES (%s, %s, %s, %s, %s, %s)
                """,
                (
                    assistant_message.id,
                    conversation_id,
                    assistant_message.content,
                    False,
                    assistant_message.timestamp,
                    json.dumps(suggested.model_dump()) if suggested else None,
                ),
            )
            cur.execute(
                "UPDATE ai_conversations SET updated_at = %s WHERE id = %s",
                (now, conversation_id),
            )
        self._conn.commit()
        return SendAiMessageResponse(
            user_message=user_message,
            assistant_message=assistant_message,
            provider=provider,
        )

    def conversation_exists(self, conversation_id: str) -> bool:
        with self._conn.cursor() as cur:
            cur.execute("SELECT 1 FROM ai_conversations WHERE id = %s", (conversation_id,))
            return cur.fetchone() is not None
