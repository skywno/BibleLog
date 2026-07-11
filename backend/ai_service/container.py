from __future__ import annotations

from dataclasses import dataclass

from ai_service.providers import create_ai_provider
from ai_service.repositories.ai import AiConversationRepository
from ai_service.repositories.ai_memory import MemoryAiConversationRepository
from ai_service.repositories.ai_postgres import PostgresAiConversationRepository
from ai_service.settings import AiServiceSettings, get_ai_settings
from common.db.postgres import get_postgres


@dataclass
class AiContainer:
    settings: AiServiceSettings
    conversations: AiConversationRepository


_container: AiContainer | None = None


def build_ai_container(settings: AiServiceSettings | None = None) -> AiContainer:
    settings = settings or get_ai_settings()
    postgres = get_postgres(settings)
    if postgres is not None:
        conversations: AiConversationRepository = PostgresAiConversationRepository(postgres)
    else:
        conversations = MemoryAiConversationRepository()
    create_ai_provider(settings)
    return AiContainer(settings=settings, conversations=conversations)


def get_ai_container() -> AiContainer:
    global _container
    if _container is None:
        _container = build_ai_container()
    return _container


def set_ai_container(container: AiContainer | None) -> None:
    global _container
    _container = container


def reset_ai_container() -> None:
    set_ai_container(None)
