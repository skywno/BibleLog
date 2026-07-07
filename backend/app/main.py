from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import ai, auth, feed, journal, reading, users

settings = get_settings()

app = FastAPI(
    title="BibleLog API",
    version="0.1.0",
    description="OpenAPI contract: ../openapi/biblelog-api.yaml",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(reading.router)
app.include_router(journal.router)
app.include_router(feed.router)
app.include_router(ai.router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/openapi.yaml", include_in_schema=False)
def openapi_file() -> dict[str, str | bool]:
    spec_path = Path(__file__).resolve().parents[2] / "openapi" / "biblelog-api.yaml"
    return {"path": str(spec_path), "exists": spec_path.exists()}
