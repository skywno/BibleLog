from __future__ import annotations

import logging
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

import psycopg
from psycopg.rows import dict_row

from common.config import Settings

logger = logging.getLogger(__name__)

_pool: psycopg.Connection | None = None


def get_postgres(settings: Settings) -> psycopg.Connection | None:
    global _pool
    if not settings.postgres_enabled:
        return None
    if settings.storage_backend == "memory":
        return None
    if _pool is None or _pool.closed:
        _pool = psycopg.connect(settings.postgres_url, row_factory=dict_row)
        _apply_schema(_pool)
        logger.info("Connected to PostgreSQL")
    return _pool


def _apply_schema(conn: psycopg.Connection) -> None:
    schema_path = Path(__file__).resolve().parents[2] / "schema" / "postgres.sql"
    sql = schema_path.read_text(encoding="utf-8")
    with conn.cursor() as cur:
        cur.execute(sql)
    conn.commit()


@contextmanager
def postgres_cursor(settings: Settings) -> Iterator[psycopg.Cursor]:
    conn = get_postgres(settings)
    if conn is None:
        raise RuntimeError("PostgreSQL is not available")
    with conn.cursor() as cur:
        yield cur
    conn.commit()


def close_postgres() -> None:
    global _pool
    if _pool is not None and not _pool.closed:
        _pool.close()
        _pool = None
