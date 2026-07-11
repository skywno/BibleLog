from __future__ import annotations

from common.bible_catalog import (
    NEW_TESTAMENT_BOOKS,
    OLD_TESTAMENT_BOOKS,
    TOTAL_BIBLE_VERSES,
    TOTAL_NEW_TESTAMENT_VERSES,
    TOTAL_OLD_TESTAMENT_VERSES,
    TOTAL_BOOKS,
    iter_verses_in_range,
    normalize_end_book_id,
    total_verses_in_book,
)
from common.models import BibleReference, ReadingRecord


def _verses_read_by_book(records: list[ReadingRecord]) -> dict[int, set[tuple[int, int]]]:
    by_book: dict[int, set[tuple[int, int]]] = {}
    for record in records:
        ref = record.reference
        end_book_id = normalize_end_book_id(ref.book_id, ref.end_book_id)
        for book_id, chapter, verse in iter_verses_in_range(
            ref.book_id,
            ref.start_chapter,
            ref.start_verse,
            end_book_id,
            ref.end_chapter,
            ref.end_verse,
        ):
            by_book.setdefault(book_id, set()).add((chapter, verse))
    return by_book


def reading_progress_from_records(records: list[ReadingRecord]) -> dict:
    verses_by_book = _verses_read_by_book(records)

    by_book: dict[str, float] = {}
    total_read = 0
    old_testament_read = 0
    new_testament_read = 0

    for book_id in range(1, TOTAL_BOOKS + 1):
        book_total = total_verses_in_book(book_id)
        book_read = len(verses_by_book.get(book_id, set()))
        ratio = (book_read / book_total) if book_total else 0.0
        by_book[str(book_id)] = min(ratio, 1.0)

        total_read += book_read
        if book_id in OLD_TESTAMENT_BOOKS:
            old_testament_read += book_read
        else:
            new_testament_read += book_read

    overall = (total_read / TOTAL_BIBLE_VERSES) if TOTAL_BIBLE_VERSES else 0.0
    old_ratio = (old_testament_read / TOTAL_OLD_TESTAMENT_VERSES) if TOTAL_OLD_TESTAMENT_VERSES else 0.0
    new_ratio = (new_testament_read / TOTAL_NEW_TESTAMENT_VERSES) if TOTAL_NEW_TESTAMENT_VERSES else 0.0

    return {
        "overall": min(overall, 1.0),
        "old_testament": min(old_ratio, 1.0),
        "new_testament": min(new_ratio, 1.0),
        "by_book": by_book,
    }
