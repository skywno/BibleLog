from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class BaseServiceSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    debug: bool = True
    jwt_secret: str = "dev-change-me-in-production"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60
    internal_service_token: str = "dev-internal-token"

    kafka_enabled: bool = True
    kafka_bootstrap_servers: str = "kafka:9092"
    kafka_topic: str = "biblelog.events"
