from __future__ import annotations

from common.models import UserProfile
from user_service.repositories.user import UserRepository


class MemoryUserRepository(UserRepository):
    def __init__(self) -> None:
        self.users: dict[str, dict[str, str]] = {}
        self.refresh_tokens: dict[str, str] = {}
        self.oauth_states: dict[str, str] = {}

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        if user_id not in self.users:
            self.users[user_id] = {"id": user_id, "nickname": nickname, "bio": bio}
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
