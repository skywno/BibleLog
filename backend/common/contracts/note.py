from __future__ import annotations

from datetime import datetime
from typing import Protocol

from common.domain import FeedTimelineEntry
from common.models import MeditationNote, NoteSummary


class NoteReader(Protocol):
    def get_detail(self, viewer_id: str, note_id: str) -> MeditationNote: ...

    def batch_summaries(self, viewer_id: str, note_ids: list[str]) -> list[NoteSummary]: ...

    def get_timeline_entry(self, note_id: str) -> FeedTimelineEntry | None: ...

    def recent_entries_for_feed(
        self,
        viewer_id: str,
        author_ids: list[str],
        *,
        since_days: int = 30,
        since: datetime | None = None,
        limit_per_author: int = 10,
    ) -> list[FeedTimelineEntry]: ...
