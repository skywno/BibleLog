from __future__ import annotations

from dataclasses import dataclass

from ai_service.providers import create_ai_provider
from shared.config import Settings, get_settings
from shared.db.postgres import get_postgres
from user_service.repositories.user import UserRepository
from user_service.repositories.user_memory import MemoryUserRepository
from user_service.repositories.user_postgres import PostgresUserRepository


@dataclass
class AiContainer:
    settings: Settings
    users: UserRepository


_container: AiContainer | None = None


def build_ai_container(settings: Settings | None = None) -> AiContainer:
    settings = settings or get_settings()
    postgres = get_postgres(settings)
    if postgres is not None:
        users: UserRepository = PostgresUserRepository(postgres)
    else:
        users = MemoryUserRepository()
    return AiContainer(settings=settings, users=users)


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
