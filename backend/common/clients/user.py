from __future__ import annotations

import httpx

from common.contracts.user import UserReader
from common.models import UserProfile
from common.settings.base import BaseServiceSettings


class HttpUserClient(UserReader):
    def __init__(self, settings: BaseServiceSettings, client: httpx.AsyncClient) -> None:
        self._base = settings.user_service_url.rstrip("/")  # type: ignore[attr-defined]
        self._token = settings.internal_service_token
        self._client = client

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    async def get_user(self, user_id: str) -> UserProfile:
        response = await self._client.get(
            f"{self._base}/internal/users/{user_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        return UserProfile.model_validate(response.json())
