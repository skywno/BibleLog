from __future__ import annotations

from dataclasses import dataclass

from common.clients.http import get_async_http_client
from common.clients.user import HttpUserClient
from common.db.scylla import get_scylla_session
from common.events.kafka_bus import KafkaEventBus, get_event_bus
from social_service.repositories.social import SocialRepository
from social_service.repositories.social_memory import MemorySocialRepository
from social_service.repositories.social_scylla import ScyllaSocialRepository
from social_service.service import SocialService
from social_service.settings import SocialServiceSettings, get_social_settings


@dataclass
class SocialContainer:
    settings: SocialServiceSettings
    social: SocialRepository
    users: HttpUserClient
    social_service: SocialService
    event_bus: KafkaEventBus


_container: SocialContainer | None = None


def build_social_container(settings: SocialServiceSettings | None = None) -> SocialContainer:
    settings = settings or get_social_settings()
    if settings.storage_backend == "scylla":
        session = get_scylla_session(settings)
        if session is None:
            raise RuntimeError("ScyllaDB session is not available")
        social: SocialRepository = ScyllaSocialRepository(session)
    else:
        social = MemorySocialRepository()

    http_client = get_async_http_client()
    users = HttpUserClient(settings, http_client)
    event_bus = get_event_bus(settings, group_id="social-service")
    social_service = SocialService(social, users, event_bus)
    return SocialContainer(
        settings=settings,
        social=social,
        users=users,
        social_service=social_service,
        event_bus=event_bus,
    )


def get_social_container() -> SocialContainer:
    global _container
    if _container is None:
        _container = build_social_container()
    return _container


def set_social_container(container: SocialContainer | None) -> None:
    global _container
    _container = container


def reset_social_container() -> None:
    set_social_container(None)
