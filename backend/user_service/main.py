from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.db.postgres import close_postgres
from user_service.container import build_user_container, reset_user_container, set_user_container
from user_service.internal_router import router as internal_router
from user_service.routers.auth import router as auth_router
from user_service.routers.users import router as users_router
from user_service.settings import get_user_settings

logger = logging.getLogger(__name__)
settings = get_user_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_user_container()
    set_user_container(build_user_container(settings))
    logger.info("user-service started")
    yield
    close_postgres()
    reset_user_container()


app = create_service_app(
    title="BibleLog User Service",
    version="0.4.0",
    service_name="user",
    routers=[auth_router, users_router, internal_router],
    lifespan=lifespan,
)
