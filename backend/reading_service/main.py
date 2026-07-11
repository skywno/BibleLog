from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from common.app_factory import create_service_app
from common.db.scylla import close_scylla
from common.telemetry import setup_telemetry
from reading_service.container import build_reading_container, reset_reading_container, set_reading_container
from reading_service.router import router as reading_router
from reading_service.settings import get_reading_settings

logger = logging.getLogger(__name__)
settings = get_reading_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_reading_container()
    set_reading_container(build_reading_container(settings))
    logger.info("reading-service started")
    yield
    close_scylla()
    reset_reading_container()


setup_telemetry("reading-service", version="0.4.0")

app = create_service_app(
    title="BibleLog Reading Service",
    version="0.4.0",
    service_name="reading",
    routers=[reading_router],
    lifespan=lifespan,
)
