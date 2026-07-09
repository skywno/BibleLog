from __future__ import annotations

from shared.models import CreateReadingRecordRequest, ReadingProgress, ReadingRecord, ReadingStats
from reading_service.repositories.reading import (
    ReadingRepository,
    reading_progress_from_records,
    reading_stats_from_records,
)


class ReadingService:
    def __init__(self, reading: ReadingRepository) -> None:
        self._reading = reading

    def add_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord:
        return self._reading.add_record(user_id, request)

    def list_records(self, user_id: str) -> list[ReadingRecord]:
        return self._reading.list_records(user_id)

    def progress(self, user_id: str) -> ReadingProgress:
        records = self._reading.list_records(user_id)
        return ReadingProgress(**reading_progress_from_records(records))

    def stats(self, user_id: str) -> ReadingStats:
        records = self._reading.list_records(user_id)
        return ReadingStats(**reading_stats_from_records(records))
