from __future__ import annotations

from functools import lru_cache
from typing import Literal

from common.settings.base import BaseServiceSettings


class AiServiceSettings(BaseServiceSettings):
    ai_provider: str = "mock"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-3-5-haiku-latest"
    postgres_url: str = "postgresql://biblelog:biblelog@localhost:5432/biblelog"
    postgres_enabled: bool = True
    storage_backend: Literal["memory", "scylla"] = "memory"


@lru_cache
def get_ai_settings() -> AiServiceSettings:
    return AiServiceSettings()
