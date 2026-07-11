from __future__ import annotations

import json
from pathlib import Path

_DATA_PATH = Path(__file__).resolve().parent.parent / "data" / "bible_verse_counts.json"
_RAW: dict[str, list[int]] = json.loads(_DATA_PATH.read_text())
VERSES_PER_CHAPTER: dict[int, list[int]] = {int(k): v for k, v in _RAW.items()}
TOTAL_BOOKS = 66

def total_chapters(book_id: int) -> int:
    counts = VERSES_PER_CHAPTER.get(book_id)
    return len(counts) if counts else 0

def verse_count(book_id: int, chapter: int) -> int:
    counts = VERSES_PER_CHAPTER.get(book_id)
    if not counts or chapter < 1 or chapter > len(counts):
        return 0
    return counts[chapter - 1]

def is_valid_point(book_id: int, chapter: int, verse: int) -> bool:
    max_verse = verse_count(book_id, chapter)
    return max_verse > 0 and 1 <= verse <= max_verse

def compare_point(book_id: int, chapter: int, verse: int) -> int:
    return book_id * 1_000_000 + chapter * 1_000 + verse

def is_valid_range(
    start_book_id: int, start_chapter: int, start_verse: int,
    end_book_id: int, end_chapter: int, end_verse: int,
) -> bool:
    if not is_valid_point(start_book_id, start_chapter, start_verse):
        return False
    if not is_valid_point(end_book_id, end_chapter, end_verse):
        return False
    return compare_point(end_book_id, end_chapter, end_verse) >= compare_point(
        start_book_id, start_chapter, start_verse
    )

def normalize_end_book_id(book_id: int, end_book_id: int | None) -> int:
    return end_book_id if end_book_id is not None else book_id
