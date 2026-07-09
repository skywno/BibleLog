from __future__ import annotations

from dataclasses import dataclass

from feed_service.cache import FeedCacheService
from feed_service.service import FeedService
from shared.clients.note import HttpNoteClient
from shared.clients.social import HttpSocialClient
from shared.clients.user import HttpRelationRepository
from shared.config import Settings, get_settings
from shared.db.redis_client import get_redis
from user_service.repositories.relation import RelationRepository


@dataclass
class FeedContainer:
    settings: Settings
    feed_cache: FeedCacheService
    feed_service: FeedService


_container: FeedContainer | None = None


def build_feed_container(settings: Settings | None = None) -> FeedContainer:
    settings = settings or get_settings()
    get_redis(settings)
    feed_cache = FeedCacheService(settings)

    notes = HttpNoteClient(settings)
    social = HttpSocialClient(settings)
    relations: RelationRepository = HttpRelationRepository(settings)

    feed_service = FeedService(notes, social, relations, feed_cache, settings)
    return FeedContainer(settings=settings, feed_cache=feed_cache, feed_service=feed_service)


def get_feed_container() -> FeedContainer:
    global _container
    if _container is None:
        _container = build_feed_container()
    return _container


def set_feed_container(container: FeedContainer | None) -> None:
    global _container
    _container = container


def reset_feed_container() -> None:
    set_feed_container(None)
