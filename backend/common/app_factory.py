from __future__ import annotations

import logging
from collections.abc import AsyncIterator, Callable, Sequence
from contextlib import asynccontextmanager
from typing import Any

from fastapi import APIRouter, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from common.exceptions import register_exception_handlers
from common.telemetry import instrument_app

logger = logging.getLogger(__name__)


def create_service_app(
    *,
    title: str,
    version: str,
    service_name: str,
    routers: Sequence[APIRouter],
    lifespan: Callable[[FastAPI], Any],
) -> FastAPI:
    app = FastAPI(title=title, version=version, lifespan=lifespan)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    register_exception_handlers(app)
    for router in routers:
        app.include_router(router)

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok", "service": service_name}

    instrument_app(app)
    return app
