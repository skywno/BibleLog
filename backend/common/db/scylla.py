from __future__ import annotations

import logging
import time
from datetime import UTC, date, datetime, timedelta
from pathlib import Path

from cassandra.cluster import Cluster, NoHostAvailable, Session
from cassandra.policies import DCAwareRoundRobinPolicy

from common.config import Settings

logger = logging.getLogger(__name__)

_session: Session | None = None


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
