from __future__ import annotations

from dataclasses import dataclass

from common.db.postgres import get_postgres
from user_service.repositories.relation import RelationRepository
from user_service.repositories.relation_memory import MemoryRelationRepository
from user_service.repositories.relation_postgres import PostgresRelationRepository
from user_service.repositories.user import UserRepository
from user_service.repositories.user_memory import MemoryUserRepository
from user_service.repositories.user_postgres import PostgresUserRepository
from user_service.settings import UserServiceSettings, get_user_settings


@dataclass
class UserContainer:
    settings: UserServiceSettings
    users: UserRepository
    relations: RelationRepository


_container: UserContainer | None = None


def build_user_container(settings: UserServiceSettings | None = None) -> UserContainer:
    settings = settings or get_user_settings()
    postgres = get_postgres(settings)
    if postgres is not None:
        users: UserRepository = PostgresUserRepository(postgres)
        relations: RelationRepository = PostgresRelationRepository(postgres)
    else:
        users = MemoryUserRepository()
        relations = MemoryRelationRepository()
    return UserContainer(settings=settings, users=users, relations=relations)


def get_user_container() -> UserContainer:
    global _container
    if _container is None:
        _container = build_user_container()
    return _container


def set_user_container(container: UserContainer | None) -> None:
    global _container
    _container = container


def reset_user_container() -> None:
    set_user_container(None)
