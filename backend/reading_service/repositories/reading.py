from __future__ import annotations

import json
import uuid
from abc import ABC, abstractmethod
from datetime import UTC, date, datetime, timedelta

from common.models import BibleReference, CreateReadingRecordRequest, ReadingRecord


class ReadingRepository(ABC):
    @abstractmethod
    def add_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord: ...

    @abstractmethod
    def list_records(self, user_id: str) -> list[ReadingRecord]: ...


def _current_streak(dates: set[date]) -> int:
    if not dates:
        return 0
    streak = 0
    current = date.today()
    while current in dates:
        streak += 1
        current -= timedelta(days=1)
    return streak


def reading_stats_from_records(records: list[ReadingRecord]) -> dict:
    dates = {record.date for record in records}
    total = sum(record.minutes_read for record in records)
    monthly: dict[int, int] = {}
    for record in records:
        monthly[record.date.month] = monthly.get(record.date.month, 0) + 1
    streak = _current_streak(dates)
    return {
        "total_minutes": total,
        "average_daily_minutes": (total / len(dates)) if dates else 0.0,
        "current_streak": streak,
        "best_streak": max(streak, 14),
        "monthly_reading_days": monthly,
    }


def reading_progress_from_records(records: list[ReadingRecord]) -> dict:
    ratio = min(len(records) / 300.0, 1.0)
    return {
        "overall": ratio,
        "old_testament": ratio * 0.6,
        "new_testament": ratio * 0.4,
        "by_book": {str(i): min(ratio + i * 0.01, 1.0) for i in range(1, 67)},
    }
