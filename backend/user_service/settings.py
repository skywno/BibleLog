from __future__ import annotations

from functools import lru_cache
from typing import Literal

from common.settings.base import BaseServiceSettings


class UserServiceSettings(BaseServiceSettings):
    refresh_token_expire_days: int = 30
    google_client_id: str = ""
    google_client_secret: str = ""
    facebook_client_id: str = ""
    facebook_client_secret: str = ""
    oauth_redirect_base: str = "http://localhost:8000/auth"
    postgres_url: str = "postgresql://biblelog:biblelog@localhost:5432/biblelog"
    postgres_enabled: bool = True
    storage_backend: Literal["memory", "scylla"] = "memory"


@lru_cache
def get_user_settings() -> UserServiceSettings:
    return UserServiceSettings()
