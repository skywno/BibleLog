from __future__ import annotations

import httpx

from shared.config import Settings
from shared.domain import UserMembership
from shared.models import UserProfile
from user_service.repositories.relation import RelationRepository
from user_service.repositories.user import UserRepository


class HttpUserRepository(UserRepository):
    def __init__(self, settings: Settings) -> None:
        self._base = settings.user_service_url.rstrip("/")
        self._token = settings.internal_service_token
        self._client = httpx.Client(timeout=10.0)

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        raise NotImplementedError("ensure_user is only available in user-service")

    def get_user(self, user_id: str) -> UserProfile:
        response = self._client.get(
            f"{self._base}/internal/users/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return UserProfile.model_validate(response.json())

    def update_user(self, user_id: str, nickname: str | None, bio: str | None) -> UserProfile:
        raise NotImplementedError("update_user is only available in user-service")

    def save_refresh_token(self, token: str, user_id: str) -> None:
        raise NotImplementedError

    def pop_refresh_token(self, token: str) -> str | None:
        raise NotImplementedError

    def save_oauth_state(self, state: str, redirect_uri: str) -> None:
        raise NotImplementedError

    def pop_oauth_state(self, state: str) -> str | None:
        raise NotImplementedError

    def create_ai_conversation(self, user_id: str, mode: str = "chat") -> dict:
        raise NotImplementedError

    def list_ai_conversations(self, user_id: str) -> list[dict]:
        raise NotImplementedError

    def list_ai_messages(self, conversation_id: str) -> list[dict]:
        raise NotImplementedError

    def append_ai_messages(self, conversation_id: str, user_content: str, assistant_content: str, suggested, provider: str):
        raise NotImplementedError

    def conversation_exists(self, conversation_id: str) -> bool:
        raise NotImplementedError


class HttpRelationRepository(RelationRepository):
    def __init__(self, settings: Settings) -> None:
        self._base = settings.user_service_url.rstrip("/")
        self._token = settings.internal_service_token
        self._client = httpx.Client(timeout=10.0)

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    def list_friend_ids(self, user_id: str) -> list[str]:
        response = self._client.get(
            f"{self._base}/internal/relations/friends/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return response.json()

    def get_membership(self, user_id: str) -> UserMembership:
        response = self._client.get(
            f"{self._base}/internal/relations/memberships/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        data = response.json()
        return UserMembership(
            church_id=data.get("church_id"),
            group_ids=set(data.get("group_ids") or []),
        )

    def list_group_member_ids(self, group_ids: set[str]) -> list[str]:
        response = self._client.post(
            f"{self._base}/internal/relations/group-members",
            headers=self._headers(),
            json={"group_ids": sorted(group_ids)},
        )
        response.raise_for_status()
        return response.json()

    def list_church_member_ids(self, church_id: str) -> list[str]:
        response = self._client.get(
            f"{self._base}/internal/relations/church-members/{church_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return response.json()
