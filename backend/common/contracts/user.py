from __future__ import annotations

from typing import Protocol

from common.models import UserProfile


class UserReader(Protocol):
    def get_user(self, user_id: str) -> UserProfile: ...
