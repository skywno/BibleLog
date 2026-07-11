from __future__ import annotations

from functools import lru_cache
from typing import Literal

from common.settings.base import BaseServiceSettings


class FeedServiceSettings(BaseServiceSettings):
    redis_url: str = "redis://localhost:6379/0"
    redis_enabled: bool = True
    feed_latest_ttl_seconds: int = 60
    feed_popular_ttl_seconds: int = 300
    feed_default_limit: int = 20
    feed_max_limit: int = 50
    user_service_url: str = "http://user-service:8001"
    note_service_url: str = "http://note-service:8002"
    social_service_url: str = "http://social-service:8003"


@lru_cache
def get_feed_settings() -> FeedServiceSettings:
    return FeedServiceSettings()
