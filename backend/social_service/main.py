from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.db.scylla import close_scylla
from common.events.kafka_bus import close_event_bus
from common.telemetry import setup_telemetry
from social_service.container import build_social_container, reset_social_container, set_social_container
from social_service.internal_router import router as internal_router
from social_service.settings import get_social_settings

logger = logging.getLogger(__name__)
settings = get_social_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_social_container()
    container = build_social_container(settings)
    set_social_container(container)
    await container.event_bus.start()
    logger.info("social-service started")
    yield
    await close_event_bus("social-service")
    close_scylla()
    reset_social_container()


setup_telemetry("social-service", version="0.4.0")

app = create_service_app(
    title="BibleLog Social Service",
    version="0.4.0",
    service_name="social",
    routers=[internal_router],
    lifespan=lifespan,
)
