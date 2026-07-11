from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.clients.http import close_async_http_client
from common.db.redis_client import close_redis
from common.db.scylla import close_scylla
from common.events.kafka_bus import close_event_bus
from common.telemetry import setup_telemetry
from note_service.container import build_note_container, reset_note_container, set_note_container
from note_service.internal_router import router as internal_router
from note_service.router import router as journal_router
from note_service.settings import get_note_settings

logger = logging.getLogger(__name__)
settings = get_note_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_note_container()
    container = build_note_container(settings)
    set_note_container(container)
    await container.event_bus.start()
    logger.info("note-service started")
    yield
    await close_event_bus("note-service")
    await close_async_http_client()
    close_redis()
    close_scylla()
    reset_note_container()


setup_telemetry("note-service", version="0.4.0")

app = create_service_app(
    title="BibleLog Note Service",
    version="0.4.0",
    service_name="note",
    routers=[journal_router, internal_router],
    lifespan=lifespan,
)
