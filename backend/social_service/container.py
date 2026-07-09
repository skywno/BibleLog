from __future__ import annotations

from dataclasses import dataclass

from feed_service.cache import FeedCacheService
from shared.config import Settings, get_settings
from shared.db.redis_client import get_redis
from shared.db.scylla import get_scylla_session
from social_service.repositories.social import SocialRepository
from social_service.repositories.social_memory import MemorySocialRepository
from social_service.repositories.social_scylla import ScyllaSocialRepository
from social_service.service import SocialService


@dataclass
class SocialContainer:
    settings: Settings
    social: SocialRepository
    feed_cache: FeedCacheService
    social_service: SocialService


_container: SocialContainer | None = None


def build_social_container(settings: Settings | None = None) -> SocialContainer:
    settings = settings or get_settings()
    if settings.storage_backend == "scylla":
        session = get_scylla_session(settings)
        if session is None:
            raise RuntimeError("ScyllaDB session is not available")
        social: SocialRepository = ScyllaSocialRepository(session)
    else:
        social = MemorySocialRepository()
    get_redis(settings)
    feed_cache = FeedCacheService(settings)
    social_service = SocialService(social, feed_cache)
    return SocialContainer(
        settings=settings,
        social=social,
        feed_cache=feed_cache,
        social_service=social_service,
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
