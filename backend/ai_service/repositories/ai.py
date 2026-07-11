from __future__ import annotations

from abc import ABC, abstractmethod

from common.models import BibleReference, SendAiMessageResponse


class AiConversationRepository(ABC):
    @abstractmethod
    def create_conversation(self, user_id: str, mode: str = "chat") -> dict: ...

    @abstractmethod
    def list_conversations(self, user_id: str) -> list[dict]: ...

    @abstractmethod
    def list_messages(self, conversation_id: str) -> list[dict]: ...

    @abstractmethod
    def append_messages(
        self,
        conversation_id: str,
        user_content: str,
        assistant_content: str,
        suggested: BibleReference | None,
        provider: str,
    ) -> SendAiMessageResponse: ...

    @abstractmethod
    def conversation_exists(self, conversation_id: str) -> bool: ...
