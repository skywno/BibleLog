from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from ai_service.container import build_ai_container, reset_ai_container, set_ai_container
from ai_service.router import router as ai_router
from ai_service.settings import get_ai_settings
from common.app_factory import create_service_app
from common.db.postgres import close_postgres

logger = logging.getLogger(__name__)
settings = get_ai_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_ai_container()
    set_ai_container(build_ai_container(settings))
    logger.info("ai-service started")
    yield
    close_postgres()
    reset_ai_container()


app = create_service_app(
    title="BibleLog AI Service",
    version="0.4.0",
    service_name="ai",
    routers=[ai_router],
    lifespan=lifespan,
)
