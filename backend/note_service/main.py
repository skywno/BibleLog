from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from note_service.container import build_note_container, reset_note_container, set_note_container
from note_service.internal_router import router as internal_router
from note_service.router import router as journal_router
from shared.config import get_settings
from shared.db.postgres import close_postgres
from shared.db.redis_client import close_redis
from shared.db.scylla import close_scylla

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_note_container()
    set_note_container(build_note_container(settings))
    logger.info("note-service started")
    yield
    close_redis()
    close_postgres()
    close_scylla()
    reset_note_container()


app = FastAPI(title="BibleLog Note Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(journal_router)
app.include_router(internal_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "note"}
