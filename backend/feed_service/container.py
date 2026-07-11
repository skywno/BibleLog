from __future__ import annotations

from dataclasses import dataclass

from common.cache.feed_cache import FeedCacheService
from common.clients.http import get_async_http_client
from common.clients.note import HttpNoteClient
from common.clients.relation import HttpRelationClient
from common.clients.social import HttpSocialClient
from common.db.redis_client import get_redis
from common.events.kafka_bus import KafkaEventBus, get_event_bus
from feed_service.service import FeedService
from feed_service.settings import FeedServiceSettings, get_feed_settings


@dataclass
class FeedContainer:
    settings: FeedServiceSettings
    feed_cache: FeedCacheService
    feed_service: FeedService
    event_bus: KafkaEventBus


_container: FeedContainer | None = None


def _register_cache_handlers(feed_cache: FeedCacheService, event_bus: KafkaEventBus) -> None:
    async def on_note_changed(payload: dict) -> None:
        note_id = payload.get("note_id")
        if isinstance(note_id, str):
            feed_cache.on_note_changed(note_id)

    async def on_note_deleted(payload: dict) -> None:
        note_id = payload.get("note_id")
        if isinstance(note_id, str):
            feed_cache.on_note_deleted(note_id)

    async def on_reaction_toggled(_payload: dict) -> None:
        feed_cache.invalidate_all_feeds()

    event_bus.subscribe("NotePublished", on_note_changed)
    event_bus.subscribe("NoteUpdated", on_note_changed)
    event_bus.subscribe("NoteDeleted", on_note_deleted)
    event_bus.subscribe("ReactionToggled", on_reaction_toggled)


def build_feed_container(settings: FeedServiceSettings | None = None) -> FeedContainer:
    settings = settings or get_feed_settings()
    get_redis(settings)
    feed_cache = FeedCacheService(settings)
    event_bus = get_event_bus(settings, group_id="feed-service")
    _register_cache_handlers(feed_cache, event_bus)

    http_client = get_async_http_client()
    notes = HttpNoteClient(settings, http_client)
    social = HttpSocialClient(settings, http_client)
    relations = HttpRelationClient(settings, http_client)

    feed_service = FeedService(notes, social, relations, feed_cache, settings)
    return FeedContainer(
        settings=settings,
        feed_cache=feed_cache,
        feed_service=feed_service,
        event_bus=event_bus,
    )


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
