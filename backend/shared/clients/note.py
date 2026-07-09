from __future__ import annotations

from datetime import UTC, datetime, timedelta

import httpx

from shared.config import Settings
from shared.domain import FeedTimelineEntry
from shared.models import (
    MeditationNote,
    NoteSummary,
    NotesBatchRequest,
    NotesBatchResponse,
    RecentNotesByAuthorsRequest,
    RecentNotesByAuthorsResponse,
    UpsertJournalNoteRequest,
)


class HttpNoteClient:
    """HTTP adapter used by feed-service — mirrors NoteService feed-related methods."""

    def __init__(self, settings: Settings) -> None:
        self._base = settings.note_service_url.rstrip("/")
        self._token = settings.internal_service_token
        self._client = httpx.Client(timeout=15.0)

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    def get_detail(self, viewer_id: str, note_id: str) -> MeditationNote:
        response = self._client.get(
            f"{self._base}/internal/notes/{note_id}",
            params={"viewer_id": viewer_id},
            headers=self._headers(),
        )
        if response.status_code == 404:
            raise KeyError(note_id)
        if response.status_code == 403:
            raise PermissionError(note_id)
        response.raise_for_status()
        return MeditationNote.model_validate(response.json())

    def batch_summaries(self, viewer_id: str, note_ids: list[str]) -> list[NoteSummary]:
        body = NotesBatchRequest(note_ids=note_ids, viewer_id=viewer_id)
        response = self._client.post(
            f"{self._base}/internal/notes/batch",
            headers=self._headers(),
            json=body.model_dump(),
        )
        response.raise_for_status()
        return NotesBatchResponse.model_validate(response.json()).notes

    def get_timeline_entry(self, note_id: str) -> FeedTimelineEntry | None:
        response = self._client.get(
            f"{self._base}/internal/notes/timeline/{note_id}",
            headers=self._headers(),
        )
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return FeedTimelineEntry.model_validate(response.json())

    def recent_entries_for_feed(
        self,
        viewer_id: str,
        author_ids: list[str],
        since_days: int = 30,
        limit_per_author: int = 10,
    ) -> list[FeedTimelineEntry]:
        since = datetime.now(UTC) - timedelta(days=since_days)
        body = RecentNotesByAuthorsRequest(
            author_ids=author_ids,
            viewer_id=viewer_id,
            since=since,
            limit_per_author=limit_per_author,
        )
        response = self._client.post(
            f"{self._base}/internal/notes/recent-for-feed",
            headers=self._headers(),
            json=body.model_dump(mode="json"),
        )
        response.raise_for_status()
        payload = RecentNotesByAuthorsResponse.model_validate(response.json())
        return [
            FeedTimelineEntry(
                note_id=entry.note_id,
                author_id=entry.author_id,
                created_at=entry.created_at,
                visibility=entry.visibility,
                church_id=entry.church_id,
                group_ids=entry.group_ids,
            )
            for entry in payload.entries
        ]
