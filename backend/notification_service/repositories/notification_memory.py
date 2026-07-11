from __future__ import annotations

import uuid
from datetime import UTC, datetime

from notification_service.repositories.notification import NotificationRepository


class MemoryNotificationRepository(NotificationRepository):
    def __init__(self) -> None:
        self._items: dict[str, list[dict]] = {}

    def create(self, user_id: str, event_type: str, payload: dict) -> dict:
        item = {
            "id": str(uuid.uuid4()),
            "user_id": user_id,
            "event_type": event_type,
            "payload": payload,
            "read": False,
            "created_at": datetime.now(UTC),
        }
        self._items.setdefault(user_id, []).append(item)
        return item

    def list_for_user(
        self,
        user_id: str,
        *,
        cursor: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]:
        items = sorted(
            self._items.get(user_id, []),
            key=lambda item: item["created_at"],
            reverse=True,
        )
        if cursor:
            cursor_time = datetime.fromisoformat(cursor.replace("Z", "+00:00"))
            items = [item for item in items if item["created_at"] < cursor_time]
        has_more = len(items) > limit
        page = items[:limit]
        next_cursor = page[-1]["created_at"].isoformat() if has_more and page else None
        return page, next_cursor, has_more
