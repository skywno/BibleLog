from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from feed_service.container import build_feed_container, reset_feed_container, set_feed_container
from feed_service.internal_router import router as internal_router
from feed_service.router import router as feed_router
from shared.config import get_settings
from shared.db.redis_client import close_redis

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_feed_container()
    set_feed_container(build_feed_container(settings))
    logger.info("feed-service started")
    yield
    close_redis()
    reset_feed_container()


app = FastAPI(title="BibleLog Feed Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(feed_router)
app.include_router(internal_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "feed"}
