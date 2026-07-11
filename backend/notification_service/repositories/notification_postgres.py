from __future__ import annotations

import json
import uuid
from datetime import UTC, datetime

import psycopg

from notification_service.repositories.notification import NotificationRepository


class PostgresNotificationRepository(NotificationRepository):
    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn

    def create(self, user_id: str, event_type: str, payload: dict) -> dict:
        notification_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO notifications (id, user_id, event_type, payload, read, created_at)
                VALUES (%s, %s, %s, %s, FALSE, %s)
                RETURNING id, user_id, event_type, payload, read, created_at
                """,
                (notification_id, user_id, event_type, json.dumps(payload), now),
            )
            row = cur.fetchone()
        self._conn.commit()
        return self._row_to_dict(row)

    def list_for_user(
        self,
        user_id: str,
        *,
        cursor: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]:
        params: list = [user_id]
        cursor_clause = ""
        if cursor:
            cursor_clause = "AND created_at < %s"
            params.append(datetime.fromisoformat(cursor.replace("Z", "+00:00")))
        params.append(limit + 1)
        with self._conn.cursor() as cur:
            cur.execute(
                f"""
                SELECT id, user_id, event_type, payload, read, created_at
                FROM notifications
                WHERE user_id = %s {cursor_clause}
                ORDER BY created_at DESC
                LIMIT %s
                """,
                params,
            )
            rows = cur.fetchall()
        has_more = len(rows) > limit
        page = rows[:limit]
        next_cursor = page[-1]["created_at"].isoformat() if has_more and page else None
        return [self._row_to_dict(row) for row in page], next_cursor, has_more

    @staticmethod
    def _row_to_dict(row: dict) -> dict:
        payload = row.get("payload") or {}
        if isinstance(payload, str):
            payload = json.loads(payload)
        return {
            "id": row["id"],
            "user_id": row["user_id"],
            "event_type": row["event_type"],
            "payload": payload,
            "read": row["read"],
            "created_at": row["created_at"],
        }
