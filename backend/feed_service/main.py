from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.clients.http import close_async_http_client
from common.db.redis_client import close_redis
from common.events.kafka_bus import close_event_bus
from feed_service.container import build_feed_container, reset_feed_container, set_feed_container
from feed_service.internal_router import router as internal_router
from feed_service.router import router as feed_router
from feed_service.settings import get_feed_settings

logger = logging.getLogger(__name__)
settings = get_feed_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_feed_container()
    container = build_feed_container(settings)
    set_feed_container(container)
    await container.event_bus.start()
    logger.info("feed-service started")
    yield
    await close_event_bus("feed-service")
    await close_async_http_client()
    close_redis()
    reset_feed_container()


app = create_service_app(
    title="BibleLog Feed Service",
    version="0.4.0",
    service_name="feed",
    routers=[feed_router, internal_router],
    lifespan=lifespan,
)
