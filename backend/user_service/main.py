from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from shared.config import get_settings
from shared.db.postgres import close_postgres
from user_service.container import build_user_container, reset_user_container, set_user_container
from user_service.internal_router import router as internal_router
from user_service.routers.auth import router as auth_router
from user_service.routers.users import router as users_router

logger = logging.getLogger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    reset_user_container()
    set_user_container(build_user_container(settings))
    logger.info("user-service started")
    yield
    close_postgres()
    reset_user_container()


app = FastAPI(title="BibleLog User Service", version="0.4.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(auth_router)
app.include_router(users_router)
app.include_router(internal_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "user"}
