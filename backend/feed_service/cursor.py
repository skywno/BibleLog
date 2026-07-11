from __future__ import annotations

import base64
from datetime import datetime

from common.domain import FeedTimelineEntry


def encode_cursor(created_at: datetime, note_id: str) -> str:
    raw = f"{created_at.isoformat()}|{note_id}"
    return base64.urlsafe_b64encode(raw.encode()).decode().rstrip("=")


def decode_cursor(cursor: str) -> tuple[datetime, str]:
    padded = cursor + "=" * (-len(cursor) % 4)
    raw = base64.urlsafe_b64decode(padded.encode()).decode()
    created_at_raw, note_id = raw.split("|", 1)
    created_at = datetime.fromisoformat(created_at_raw.replace("Z", "+00:00"))
    return created_at, note_id


def sort_entries(entries: list[FeedTimelineEntry]) -> list[FeedTimelineEntry]:
    return sorted(entries, key=lambda entry: (entry.created_at, entry.note_id), reverse=True)


def page_entries(
    entries: list[FeedTimelineEntry],
    cursor: str | None,
    limit: int,
) -> tuple[list[FeedTimelineEntry], str | None, bool]:
    sorted_entries = sort_entries(entries)
    if cursor:
        cursor_at, cursor_id = decode_cursor(cursor)
        sorted_entries = [
            entry
            for entry in sorted_entries
            if (entry.created_at < cursor_at)
            or (entry.created_at == cursor_at and entry.note_id < cursor_id)
        ]

    page = sorted_entries[: limit + 1]
    has_more = len(page) > limit
    items = page[:limit]
    next_cursor = None
    if has_more and items:
        last = items[-1]
        next_cursor = encode_cursor(last.created_at, last.note_id)
    return items, next_cursor, has_more


def score_for_latest(created_at: datetime) -> float:
    return created_at.timestamp()


def score_for_popular(
    created_at: datetime,
    reaction_total: int,
    comment_count: int,
) -> float:
    hours = max((datetime.now(created_at.tzinfo) - created_at).total_seconds() / 3600, 0.0)
    time_decay = 1 / (1 + hours / 24)
    return (reaction_total * 2 + comment_count * 3) * time_decay
