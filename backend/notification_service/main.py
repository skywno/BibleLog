from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.clients.http import close_async_http_client
from common.db.postgres import close_postgres
from common.events.kafka_bus import close_event_bus
from common.telemetry import setup_telemetry
from notification_service.container import (
    build_notification_container,
    reset_notification_container,
    set_notification_container,
)
from notification_service.router import router as notification_router
from notification_service.settings import get_notification_settings

logger = logging.getLogger(__name__)
settings = get_notification_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_notification_container()
    container = build_notification_container(settings)
    set_notification_container(container)
    await container.event_bus.start()
    logger.info("notification-service started")
    yield
    await close_event_bus("notification-service")
    await close_async_http_client()
    close_postgres()
    reset_notification_container()


setup_telemetry("notification-service", version="0.1.0")

app = create_service_app(
    title="BibleLog Notification Service",
    version="0.1.0",
    service_name="notification",
    routers=[notification_router],
    lifespan=lifespan,
)
