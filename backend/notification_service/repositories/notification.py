from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime


class NotificationRepository(ABC):
    @abstractmethod
    def create(
        self,
        user_id: str,
        event_type: str,
        payload: dict,
    ) -> dict: ...

    @abstractmethod
    def list_for_user(
        self,
        user_id: str,
        *,
        cursor: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]: ...
