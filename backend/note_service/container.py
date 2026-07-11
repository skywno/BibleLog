from __future__ import annotations

from dataclasses import dataclass

from common.cache.feed_cache import FeedCacheService
from common.clients.http import get_async_http_client
from common.clients.relation import HttpRelationClient
from common.clients.user import HttpUserClient
from common.db.redis_client import get_redis
from common.db.scylla import get_scylla_session
from common.events.kafka_bus import KafkaEventBus, get_event_bus
from note_service.repositories.note import NoteRepository
from note_service.repositories.note_memory import MemoryNoteRepository
from note_service.repositories.note_scylla import ScyllaNoteRepository
from note_service.service import NoteService
from note_service.settings import NoteServiceSettings, get_note_settings


@dataclass
class NoteContainer:
    settings: NoteServiceSettings
    notes: NoteRepository
    users: HttpUserClient
    relations: HttpRelationClient
    feed_cache: FeedCacheService
    note_service: NoteService
    event_bus: KafkaEventBus


_container: NoteContainer | None = None


def build_note_container(settings: NoteServiceSettings | None = None) -> NoteContainer:
    settings = settings or get_note_settings()
    if settings.storage_backend == "scylla":
        session = get_scylla_session(settings)
        if session is None:
            raise RuntimeError("ScyllaDB session is not available")
        notes: NoteRepository = ScyllaNoteRepository(session)
    else:
        notes = MemoryNoteRepository()

    get_redis(settings)
    http_client = get_async_http_client()
    users = HttpUserClient(settings, http_client)
    relations = HttpRelationClient(settings, http_client)
    feed_cache = FeedCacheService(settings)
    event_bus = get_event_bus(settings, group_id="note-service")

    note_service = NoteService(notes, users, relations, feed_cache, settings, event_bus)
    return NoteContainer(
        settings=settings,
        notes=notes,
        users=users,
        relations=relations,
        feed_cache=feed_cache,
        note_service=note_service,
        event_bus=event_bus,
    )


def get_note_container() -> NoteContainer:
    global _container
    if _container is None:
        _container = build_note_container()
    return _container


def set_note_container(container: NoteContainer | None) -> None:
    global _container
    _container = container


def reset_note_container() -> None:
    set_note_container(None)
