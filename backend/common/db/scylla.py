from __future__ import annotations

import logging
import re
import time
from datetime import UTC, date, datetime, timedelta
from pathlib import Path
from typing import Any

from cassandra.cluster import Cluster, NoHostAvailable, Session
from cassandra.policies import DCAwareRoundRobinPolicy

from common.config import Settings
from common.telemetry import traces_enabled

logger = logging.getLogger(__name__)

_session: Session | None = None

_TABLE_PATTERN = re.compile(
    r"\b(?:FROM|INTO|UPDATE|TABLE)\s+([a-z_][a-z0-9_]*)",
    re.IGNORECASE,
)


def _statement_table(statement: str) -> str | None:
    match = _TABLE_PATTERN.search(statement)
    return match.group(1) if match else None


def _operation_name(statement: str) -> str:
    first = statement.strip().split(None, 1)[0].upper()
    return first if first else "QUERY"


def traced_execute(session: Session, statement: str, parameters: Any = None):
    if not traces_enabled():
        return session.execute(statement, parameters)

    from opentelemetry import trace
    from opentelemetry.trace import SpanKind, Status, StatusCode

    table = _statement_table(statement)
    operation = _operation_name(statement)
    span_name = f"scylla.{operation.lower()}"
    if table:
        span_name = f"{span_name} {table}"

    tracer = trace.get_tracer("common.db.scylla")
    with tracer.start_as_current_span(span_name, kind=SpanKind.CLIENT) as span:
        span.set_attribute("db.system", "scylla")
        span.set_attribute("db.operation", operation)
        if table:
            span.set_attribute("db.cassandra.table", table)
        start = time.perf_counter()
        try:
            result = session.execute(statement, parameters)
            span.set_status(Status(StatusCode.OK))
            return result
        except Exception as exc:
            span.record_exception(exc)
            span.set_status(Status(StatusCode.ERROR, str(exc)))
            span.set_attribute("error.type", type(exc).__name__)
            raise
        finally:
            span.set_attribute("db.duration_ms", (time.perf_counter() - start) * 1000)


def get_scylla_session(settings: Settings) -> Session | None:
    global _session
    if settings.storage_backend != "scylla":
        return None
    if _session is not None:
        return _session

    hosts = [host.strip() for host in settings.scylla_hosts.split(",") if host.strip()]
    last_error: Exception | None = None
    for attempt in range(12):
        cluster = Cluster(
            hosts,
            port=settings.scylla_port,
            load_balancing_policy=DCAwareRoundRobinPolicy(local_dc="datacenter1"),
            connect_timeout=10,
        )
        try:
            _session = cluster.connect()
            break
        except NoHostAvailable as exc:
            last_error = exc
            cluster.shutdown()
            if attempt < 11:
                delay = min(2**attempt, 15)
                logger.warning("ScyllaDB not ready (attempt %s/12), retrying in %ss", attempt + 1, delay)
                time.sleep(delay)
            else:
                raise

    if _session is None:
        raise RuntimeError("ScyllaDB session is not available") from last_error

    _apply_schema(_session, settings.scylla_keyspace)
    _session.set_keyspace(settings.scylla_keyspace)
    logger.info("Connected to ScyllaDB keyspace=%s", settings.scylla_keyspace)
    return _session


def _apply_schema(session: Session, keyspace: str) -> None:
    schema_path = Path(__file__).resolve().parents[2] / "schema" / "scylla.cql"
    raw = schema_path.read_text(encoding="utf-8")
    statements = [part.strip() for part in raw.split(";") if part.strip()]
    for statement in statements:
        if statement.upper().startswith("USE "):
            continue
        if "CREATE KEYSPACE" in statement.upper():
            session.execute(statement)
            session.set_keyspace(keyspace)
            continue
        formatted = statement.replace("biblelog", keyspace)
        session.execute(formatted)


def close_scylla() -> None:
    global _session
    if _session is not None:
        _session.cluster.shutdown()
        _session = None


def scylla_date(value: date) -> date:
    if type(value).__name__ == "Date":
        return date(1970, 1, 1) + timedelta(days=value.days_from_epoch)
    return value


def scylla_timestamp(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value
