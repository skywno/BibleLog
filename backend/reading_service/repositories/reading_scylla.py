from __future__ import annotations

import json
import uuid
from datetime import UTC, datetime

from cassandra.cluster import Session

from common.db.scylla import scylla_date, scylla_timestamp, traced_execute
from common.models import BibleReference, CreateReadingRecordRequest, ReadingRecord
from reading_service.repositories.reading import ReadingRepository


class ScyllaReadingRepository(ReadingRepository):
    def __init__(self, session: Session) -> None:
        self._session = session

    def add_record(self, user_id: str, request: CreateReadingRecordRequest) -> ReadingRecord:
        for row in self.list_records(user_id):
            if row.date == request.date and row.reference.model_dump() == request.reference.model_dump():
                raise ValueError("동일한 날짜에 같은 범위의 기록이 이미 있습니다.")
        record = ReadingRecord(
            id=str(uuid.uuid4()),
            date=request.date,
            reference=request.reference,
            minutes_read=request.minutes_read,
            created_at=datetime.now(UTC),
        )
        reference = record.reference
        traced_execute(self._session,
            """
            INSERT INTO reading_records_by_user (
                user_id, date, record_id, book_id, start_chapter, start_verse,
                end_book_id, end_chapter, end_verse, minutes_read, created_at, reference_json
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                user_id,
                record.date,
                record.id,
                reference.book_id,
                reference.start_chapter,
                reference.start_verse,
                reference.end_book_id,
                reference.end_chapter,
                reference.end_verse,
                record.minutes_read,
                record.created_at,
                reference.model_dump_json(),
            ),
        )
        return record

    def list_records(self, user_id: str) -> list[ReadingRecord]:
        rows = traced_execute(self._session,
            """
            SELECT record_id, date, minutes_read, created_at, reference_json
            FROM reading_records_by_user
            WHERE user_id = %s
            """,
            (user_id,),
        )
        records: list[ReadingRecord] = []
        for row in rows:
            data = row._asdict()
            reference = BibleReference.model_validate_json(data["reference_json"])
            records.append(
                ReadingRecord(
                    id=data["record_id"],
                    date=scylla_date(data["date"]),
                    reference=reference,
                    minutes_read=data["minutes_read"],
                    created_at=scylla_timestamp(data["created_at"]),
                )
            )
        return records
