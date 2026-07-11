from __future__ import annotations

from functools import lru_cache

from common.settings.base import BaseServiceSettings


class NotificationServiceSettings(BaseServiceSettings):
    postgres_url: str = "postgresql://biblelog:biblelog@localhost:5432/biblelog"
    postgres_enabled: bool = True
    user_service_url: str = "http://user-service:8001"
    note_service_url: str = "http://note-service:8002"


@lru_cache
def get_notification_settings() -> NotificationServiceSettings:
    return NotificationServiceSettings()
