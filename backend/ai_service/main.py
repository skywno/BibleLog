from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from ai_service.container import build_ai_container, reset_ai_container, set_ai_container
from ai_service.router import router as ai_router
from shared.config import get_settings
from shared.db.postgres import close_postgres

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_ai_container()
    set_ai_container(build_ai_container(settings))
    logger.info("ai-service started")
    yield
    close_postgres()
    reset_ai_container()


app = FastAPI(title="BibleLog AI Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(ai_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai"}
