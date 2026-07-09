from __future__ import annotations

from dataclasses import dataclass

from reading_service.repositories.reading import ReadingRepository
from reading_service.repositories.reading_memory import MemoryReadingRepository
from reading_service.repositories.reading_scylla import ScyllaReadingRepository
from reading_service.service import ReadingService
from shared.config import Settings, get_settings
from shared.db.scylla import get_scylla_session


@dataclass
class ReadingContainer:
    settings: Settings
    reading: ReadingRepository
    reading_service: ReadingService


_container: ReadingContainer | None = None


def build_reading_container(settings: Settings | None = None) -> ReadingContainer:
    settings = settings or get_settings()
    if settings.storage_backend == "scylla":
        session = get_scylla_session(settings)
        if session is None:
            raise RuntimeError("ScyllaDB session is not available")
        reading: ReadingRepository = ScyllaReadingRepository(session)
    else:
        reading = MemoryReadingRepository()
    return ReadingContainer(
        settings=settings,
        reading=reading,
        reading_service=ReadingService(reading),
    )


def get_reading_container() -> ReadingContainer:
    global _container
    if _container is None:
        _container = build_reading_container()
    return _container


def set_reading_container(container: ReadingContainer | None) -> None:
    global _container
    _container = container


def reset_reading_container() -> None:
    set_reading_container(None)
