from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.db.postgres import close_postgres
from common.events.kafka_bus import close_event_bus
from common.telemetry import setup_telemetry
from user_service.container import build_user_container, reset_user_container, set_user_container
from user_service.internal_router import router as internal_router
from user_service.routers.auth import router as auth_router
from user_service.routers.organizations import router as organizations_router
from user_service.routers.relations import router as relations_router
from user_service.routers.users import router as users_router
from user_service.settings import get_user_settings

logger = logging.getLogger(__name__)
settings = get_user_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_user_container()
    container = build_user_container(settings)
    set_user_container(container)
    await container.event_bus.start()
    logger.info("user-service started")
    yield
    await close_event_bus("user-service")
    close_postgres()
    reset_user_container()


setup_telemetry("user-service", version="0.5.0")

app = create_service_app(
    title="BibleLog User Service",
    version="0.5.0",
    service_name="user",
    routers=[
        auth_router,
        users_router,
        relations_router,
        organizations_router,
        internal_router,
    ],
    lifespan=lifespan,
)
