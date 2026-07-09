from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from reading_service.container import build_reading_container, reset_reading_container, set_reading_container
from reading_service.router import router as reading_router
from shared.config import get_settings
from shared.db.scylla import close_scylla

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_reading_container()
    set_reading_container(build_reading_container(settings))
    logger.info("reading-service started")
    yield
    close_scylla()
    reset_reading_container()


app = FastAPI(title="BibleLog Reading Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(reading_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "reading"}
