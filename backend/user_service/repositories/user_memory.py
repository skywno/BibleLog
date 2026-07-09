from __future__ import annotations

import uuid
from datetime import UTC, datetime
from typing import Any

from shared.models import AiMessage, BibleReference, SendAiMessageResponse, UserProfile
from user_service.repositories.user import UserRepository


class MemoryUserRepository(UserRepository):
    def __init__(self) -> None:
        self.users: dict[str, dict[str, str]] = {}
        self.refresh_tokens: dict[str, str] = {}
        self.oauth_states: dict[str, str] = {}
        self.ai_conversations: dict[str, list[dict[str, Any]]] = {}
        self.ai_messages: dict[str, list[dict[str, Any]]] = {}

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        if user_id not in self.users:
            self.users[user_id] = {"id": user_id, "nickname": nickname, "bio": bio}
            self.ai_conversations[user_id] = []
        return UserProfile(**self.users[user_id], is_logged_in=True)

    def get_user(self, user_id: str) -> UserProfile:
        return UserProfile(**self.users[user_id], is_logged_in=True)

    def update_user(
        self,
        user_id: str,
        nickname: str | None,
        bio: str | None,
    ) -> UserProfile:
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

    def save_oauth_state(self, state: str, redirect_uri: str) -> None:
        self.oauth_states[state] = redirect_uri

    def pop_oauth_state(self, state: str) -> str | None:
        return self.oauth_states.pop(state, None)

    def create_ai_conversation(self, user_id: str, mode: str = "chat") -> dict:
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

    def list_ai_conversations(self, user_id: str) -> list[dict]:
        return self.ai_conversations.get(user_id, [])

    def list_ai_messages(self, conversation_id: str) -> list[dict]:
        return self.ai_messages.get(conversation_id, [])

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
        )
        assistant_message = AiMessage(
            id=str(uuid.uuid4()),
            content=assistant_content,
            is_from_user=False,
            timestamp=now,
            suggested_reference=suggested,
        )
        self.ai_messages.setdefault(conversation_id, []).extend(
            [user_message.model_dump(), assistant_message.model_dump()]
        )
        return SendAiMessageResponse(
            user_message=user_message,
            assistant_message=assistant_message,
            provider=provider,
        )

    def conversation_exists(self, conversation_id: str) -> bool:
        return conversation_id in self.ai_messages
