from __future__ import annotations

import uuid
from abc import ABC, abstractmethod
from datetime import UTC, date, datetime, timedelta

from common.models import CreateReadingRecordRequest, ReadingRecord
from common.reading_progress import reading_progress_from_records


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


def _best_streak(dates: set[date]) -> int:
    if not dates:
        return 0
    sorted_dates = sorted(dates)
    best = 1
    current = 1
    for index in range(1, len(sorted_dates)):
        if (sorted_dates[index] - sorted_dates[index - 1]).days == 1:
            current += 1
            best = max(best, current)
        else:
            current = 1
    return best


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
        "best_streak": _best_streak(dates),
        "monthly_reading_days": monthly,
    }
