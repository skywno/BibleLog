from __future__ import annotations

from functools import lru_cache
from typing import Literal

from common.settings.base import BaseServiceSettings


class Settings(BaseServiceSettings):
    """Compatibility settings object used by DB helpers and legacy imports."""

    app_name: str = "BibleLog API"
    api_prefix: str = ""
    refresh_token_expire_days: int = 30

    google_client_id: str = ""
    google_client_secret: str = ""
    facebook_client_id: str = ""
    facebook_client_secret: str = ""

    oauth_redirect_base: str = "http://localhost:8000/auth"

    ai_provider: str = "mock"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-3-5-haiku-latest"

    storage_backend: Literal["memory", "scylla"] = "memory"

    scylla_hosts: str = "127.0.0.1"
    scylla_port: int = 9042
    scylla_keyspace: str = "biblelog"

    redis_url: str = "redis://localhost:6379/0"
    redis_enabled: bool = True

    postgres_url: str = "postgresql://biblelog:biblelog@localhost:5432/biblelog"
    postgres_enabled: bool = True

    feed_latest_ttl_seconds: int = 60
    feed_popular_ttl_seconds: int = 300
    feed_default_limit: int = 20
    feed_max_limit: int = 50
    note_excerpt_length: int = 120

    kafka_enabled: bool = True
    kafka_bootstrap_servers: str = "kafka:9092"
    kafka_topic: str = "biblelog.events"

    user_service_url: str = "http://user-service:8001"
    note_service_url: str = "http://note-service:8002"
    notification_service_url: str = "http://notification-service:8007"
    social_service_url: str = "http://social-service:8003"
    feed_service_url: str = "http://feed-service:8004"
    reading_service_url: str = "http://reading-service:8005"
    ai_service_url: str = "http://ai-service:8006"


@lru_cache
def get_settings() -> Settings:
    return Settings()
