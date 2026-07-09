from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import UTC, datetime

from shared.models import BibleReference, SendAiMessageResponse, UserProfile


class UserRepository(ABC):
    @abstractmethod
    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile: ...

    @abstractmethod
    def get_user(self, user_id: str) -> UserProfile: ...

    @abstractmethod
    def update_user(
        self,
        user_id: str,
        nickname: str | None,
        bio: str | None,
    ) -> UserProfile: ...

    @abstractmethod
    def save_refresh_token(self, token: str, user_id: str) -> None: ...

    @abstractmethod
    def pop_refresh_token(self, token: str) -> str | None: ...

    @abstractmethod
    def save_oauth_state(self, state: str, redirect_uri: str) -> None: ...

    @abstractmethod
    def pop_oauth_state(self, state: str) -> str | None: ...

    @abstractmethod
    def create_ai_conversation(self, user_id: str, mode: str = "chat") -> dict: ...

    @abstractmethod
    def list_ai_conversations(self, user_id: str) -> list[dict]: ...

    @abstractmethod
    def list_ai_messages(self, conversation_id: str) -> list[dict]: ...

    @abstractmethod
    def append_ai_messages(
        self,
        conversation_id: str,
        user_content: str,
        assistant_content: str,
        suggested: BibleReference | None,
        provider: str,
    ) -> SendAiMessageResponse: ...

    @abstractmethod
    def conversation_exists(self, conversation_id: str) -> bool: ...
