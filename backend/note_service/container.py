from __future__ import annotations

from dataclasses import dataclass

import httpx

from feed_service.cache import FeedCacheService
from note_service.repositories.note import NoteRepository
from note_service.repositories.note_memory import MemoryNoteRepository
from note_service.repositories.note_scylla import ScyllaNoteRepository
from note_service.service import NoteService
from shared.clients.user import HttpRelationRepository, HttpUserRepository
from shared.config import Settings, get_settings
from shared.db.scylla import get_scylla_session
from shared.events.bus import event_bus
from user_service.repositories.relation import RelationRepository
from user_service.repositories.user import UserRepository


@dataclass
class NoteContainer:
    settings: Settings
    notes: NoteRepository
    users: UserRepository
    relations: RelationRepository
    feed_cache: FeedCacheService
    note_service: NoteService


_container: NoteContainer | None = None


def _notify_feed_invalidate(settings: Settings) -> None:
    try:
        httpx.post(
            f"{settings.feed_service_url.rstrip('/')}/internal/cache/invalidate-all",
            headers={"X-Internal-Token": settings.internal_service_token},
            timeout=5.0,
        )
    except httpx.HTTPError:
        pass


def build_note_container(settings: Settings | None = None) -> NoteContainer:
    settings = settings or get_settings()
    if settings.storage_backend == "scylla":
        session = get_scylla_session(settings)
        if session is None:
            raise RuntimeError("ScyllaDB session is not available")
        notes: NoteRepository = ScyllaNoteRepository(session)
    else:
        notes = MemoryNoteRepository()

    users: UserRepository = HttpUserRepository(settings)
    relations: RelationRepository = HttpRelationRepository(settings)
    feed_cache = FeedCacheService(settings)
    note_service = NoteService(notes, users, relations, feed_cache, settings)

    event_bus.subscribe("NotePublished", lambda _: _notify_feed_invalidate(settings))
    event_bus.subscribe("NoteUpdated", lambda _: _notify_feed_invalidate(settings))
    event_bus.subscribe("NoteDeleted", lambda _: _notify_feed_invalidate(settings))

    return NoteContainer(
        settings=settings,
        notes=notes,
        users=users,
        relations=relations,
        feed_cache=feed_cache,
        note_service=note_service,
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
