from __future__ import annotations

import uuid
from datetime import UTC, datetime
from typing import Any

from ai_service.repositories.ai import AiConversationRepository
from common.models import AiMessage, BibleReference, SendAiMessageResponse


class MemoryAiConversationRepository(AiConversationRepository):
    def __init__(self) -> None:
        self.conversations: dict[str, list[dict[str, Any]]] = {}
        self.messages: dict[str, list[dict[str, Any]]] = {}

    def create_conversation(self, user_id: str, mode: str = "chat") -> dict:
        conversation = {
            "id": str(uuid.uuid4()),
            "mode": mode,
            "title": "새 대화",
            "created_at": datetime.now(UTC),
            "updated_at": datetime.now(UTC),
        }
        self.conversations.setdefault(user_id, []).append(conversation)
        self.messages[conversation["id"]] = []
        return conversation

    def list_conversations(self, user_id: str) -> list[dict]:
        return self.conversations.get(user_id, [])

    def list_messages(self, conversation_id: str) -> list[dict]:
        return self.messages.get(conversation_id, [])

    def append_messages(
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
        self.messages.setdefault(conversation_id, []).extend(
            [user_message.model_dump(), assistant_message.model_dump()]
        )
        return SendAiMessageResponse(
            user_message=user_message,
            assistant_message=assistant_message,
            provider=provider,
        )

    def conversation_exists(self, conversation_id: str) -> bool:
        return conversation_id in self.messages
