from __future__ import annotations

from abc import ABC, abstractmethod

from common.models import UserProfile


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
        photo_url: str | None = None,
        profile_visibility: str | None = None,
    ) -> UserProfile: ...

    @abstractmethod
    def save_refresh_token(self, token: str, user_id: str) -> None: ...

    @abstractmethod
    def pop_refresh_token(self, token: str) -> str | None: ...

    @abstractmethod
    def save_oauth_state(self, state: str, redirect_uri: str) -> None: ...

    @abstractmethod
    def pop_oauth_state(self, state: str) -> str | None: ...
