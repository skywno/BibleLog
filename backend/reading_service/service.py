from __future__ import annotations

from common.bible_catalog import is_valid_range, normalize_end_book_id
from common.models import CreateReadingRecordRequest, ReadingProgress, ReadingRecord, ReadingStats
from reading_service.repositories.reading import (
    ReadingRepository,
    reading_progress_from_records,
    reading_stats_from_records,
)


class ReadingService:
    def __init__(self, reading: ReadingRepository) -> None:
        self._reading = reading

    def add_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord:
        reference = request.reference
        end_book_id = normalize_end_book_id(reference.book_id, reference.end_book_id)
        if not is_valid_range(
            reference.book_id,
            reference.start_chapter,
            reference.start_verse,
            end_book_id,
            reference.end_chapter,
            reference.end_verse,
        ):
            raise ValueError("유효하지 않은 성경 범위입니다.")
        return self._reading.add_record(user_id, request)

    def list_records(self, user_id: str) -> list[ReadingRecord]:
        return self._reading.list_records(user_id)

    def progress(self, user_id: str) -> ReadingProgress:
        records = self._reading.list_records(user_id)
        return ReadingProgress(**reading_progress_from_records(records))

    def stats(self, user_id: str) -> ReadingStats:
        records = self._reading.list_records(user_id)
        return ReadingStats(**reading_stats_from_records(records))
