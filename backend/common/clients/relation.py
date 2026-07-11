from __future__ import annotations

import httpx

from common.contracts.relation import RelationReader
from common.domain import UserMembership
from common.settings.base import BaseServiceSettings


class HttpRelationClient(RelationReader):
    def __init__(self, settings: BaseServiceSettings, client: httpx.AsyncClient) -> None:
        self._base = settings.user_service_url.rstrip("/")  # type: ignore[attr-defined]
        self._token = settings.internal_service_token
        self._client = client

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    async def list_friend_ids(self, user_id: str) -> list[str]:
        response = await self._client.get(
            f"{self._base}/internal/relations/friends/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return response.json()

    async def get_membership(self, user_id: str) -> UserMembership:
        response = await self._client.get(
            f"{self._base}/internal/relations/memberships/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        data = response.json()
        return UserMembership(
            church_id=data.get("church_id"),
            group_ids=set(data.get("group_ids") or []),
        )

    async def list_group_member_ids(self, group_ids: set[str]) -> list[str]:
        response = await self._client.post(
            f"{self._base}/internal/relations/group-members",
            headers=self._headers(),
            json={"group_ids": sorted(group_ids)},
        )
        response.raise_for_status()
        return response.json()

    async def list_church_member_ids(self, church_id: str) -> list[str]:
        response = await self._client.get(
            f"{self._base}/internal/relations/church-members/{church_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return response.json()
