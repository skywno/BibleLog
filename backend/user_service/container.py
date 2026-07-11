from __future__ import annotations

from dataclasses import dataclass

from common.db.postgres import get_postgres
from common.events.kafka_bus import KafkaEventBus, get_event_bus
from user_service.organization_service import OrganizationService
from user_service.relation_service import RelationService
from user_service.repositories.organization import OrganizationRepository
from user_service.repositories.organization_memory import MemoryOrganizationRepository
from user_service.repositories.organization_postgres import PostgresOrganizationRepository
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
    organizations: OrganizationRepository
    relation_service: RelationService
    organization_service: OrganizationService
    event_bus: KafkaEventBus


_container: UserContainer | None = None


def build_user_container(settings: UserServiceSettings | None = None) -> UserContainer:
    settings = settings or get_user_settings()
    postgres = get_postgres(settings)
    if postgres is not None:
        users: UserRepository = PostgresUserRepository(postgres)
        relations: RelationRepository = PostgresRelationRepository(postgres)
        organizations: OrganizationRepository = PostgresOrganizationRepository(postgres)
    else:
        users = MemoryUserRepository()
        relations = MemoryRelationRepository()
        organizations = MemoryOrganizationRepository()
        if isinstance(relations, MemoryRelationRepository) and isinstance(users, MemoryUserRepository):
            relations.memberships = organizations.memberships  # type: ignore[attr-defined]
            relations.group_members = organizations.group_members  # type: ignore[attr-defined]

    event_bus = get_event_bus(settings, group_id="user-service")
    relation_service = RelationService(relations, event_bus)
    organization_service = OrganizationService(organizations)
    return UserContainer(
        settings=settings,
        users=users,
        relations=relations,
        organizations=organizations,
        relation_service=relation_service,
        organization_service=organization_service,
        event_bus=event_bus,
    )


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
