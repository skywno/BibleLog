from __future__ import annotations

import uuid
from datetime import UTC, datetime

from shared.models import CreateReadingRecordRequest, ReadingRecord
from reading_service.repositories.reading import ReadingRepository


class MemoryReadingRepository(ReadingRepository):
    def __init__(self) -> None:
        self._records: dict[str, list[ReadingRecord]] = {}

    def add_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord:
        records = self._records.setdefault(user_id, [])
        for existing in records:
            if (
                existing.date == request.date
                and existing.reference.model_dump() == request.reference.model_dump()
            ):
                raise ValueError("동일한 날짜에 같은 범위의 기록이 이미 있습니다.")
        record = ReadingRecord(
            id=str(uuid.uuid4()),
            date=request.date,
            reference=request.reference,
            minutes_read=request.minutes_read,
            created_at=datetime.now(UTC),
        )
        records.append(record)
        return record

    def list_records(self, user_id: str) -> list[ReadingRecord]:
        return self._records.get(user_id, [])
