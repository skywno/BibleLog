from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from shared.config import get_settings
from shared.db.redis_client import close_redis
from shared.db.scylla import close_scylla
from social_service.container import build_social_container, reset_social_container, set_social_container
from social_service.internal_router import router as internal_router

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_social_container()
    set_social_container(build_social_container(settings))
    logger.info("social-service started")
    yield
    close_redis()
    close_scylla()
    reset_social_container()


app = FastAPI(title="BibleLog Social Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(internal_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "social"}
