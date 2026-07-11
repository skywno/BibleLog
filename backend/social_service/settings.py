from __future__ import annotations

from functools import lru_cache
from typing import Literal

from common.settings.base import BaseServiceSettings


class SocialServiceSettings(BaseServiceSettings):
    storage_backend: Literal["memory", "scylla"] = "memory"
    scylla_hosts: str = "127.0.0.1"
    scylla_port: int = 9042
    scylla_keyspace: str = "biblelog"
    user_service_url: str = "http://user-service:8001"


@lru_cache
def get_social_settings() -> SocialServiceSettings:
    return SocialServiceSettings()
