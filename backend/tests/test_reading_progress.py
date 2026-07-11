from __future__ import annotations

import unittest
from datetime import date, datetime, timezone

from common.bible_catalog import total_verses_in_book, verse_count
from common.models import BibleReference, ReadingRecord
from common.reading_progress import reading_progress_from_records
from reading_service.repositories.reading import reading_stats_from_records


def _record(
    book_id: int,
    start_chapter: int,
    start_verse: int,
    end_chapter: int,
    end_verse: int,
    end_book_id: int | None = None,
) -> ReadingRecord:
    return ReadingRecord(
        id="test-record",
        date=date(2026, 7, 11),
        reference=BibleReference(
            book_id=book_id,
            start_chapter=start_chapter,
            start_verse=start_verse,
            end_chapter=end_chapter,
            end_verse=end_verse,
            end_book_id=end_book_id,
        ),
        minutes_read=15,
        created_at=datetime(2026, 7, 11, tzinfo=timezone.utc),
    )


class ReadingProgressTest(unittest.TestCase):
    def test_empty_records_return_zero_progress(self) -> None:
        progress = reading_progress_from_records([])

        self.assertEqual(progress["overall"], 0.0)
        self.assertEqual(progress["old_testament"], 0.0)
        self.assertEqual(progress["new_testament"], 0.0)
        self.assertTrue(all(value == 0.0 for value in progress["by_book"].values()))

    def test_single_chapter_progress(self) -> None:
        genesis_1_verses = verse_count(1, 1)
        genesis_total = total_verses_in_book(1)

        progress = reading_progress_from_records([_record(1, 1, 1, 1, genesis_1_verses)])

        self.assertAlmostEqual(progress["by_book"]["1"], genesis_1_verses / genesis_total)
        self.assertGreater(progress["overall"], 0.0)
        self.assertEqual(progress["new_testament"], 0.0)

    def test_overlapping_records_do_not_double_count(self) -> None:
        single = reading_progress_from_records([_record(1, 1, 1, 1, 1)])
        duplicated = reading_progress_from_records(
            [
                _record(1, 1, 1, 1, 1),
                _record(1, 1, 1, 1, 1),
            ]
        )

        self.assertEqual(single["by_book"]["1"], duplicated["by_book"]["1"])


class ReadingStatsTest(unittest.TestCase):
    def test_best_streak_is_not_hardcoded(self) -> None:
        stats = reading_stats_from_records(
            [
                _record(1, 1, 1, 1, 1),
            ]
        )

        self.assertEqual(stats["best_streak"], 1)


if __name__ == "__main__":
    unittest.main()
