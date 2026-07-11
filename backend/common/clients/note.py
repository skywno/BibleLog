from __future__ import annotations

from datetime import UTC, datetime, timedelta

import httpx

from common.contracts.note import NoteReader
from common.domain import FeedTimelineEntry
from common.models import (
    MeditationNote,
    NoteSummary,
    NotesBatchRequest,
    NotesBatchResponse,
    RecentNotesByAuthorsRequest,
    RecentNotesByAuthorsResponse,
)
from common.settings.base import BaseServiceSettings


class HttpNoteClient(NoteReader):
    def __init__(self, settings: BaseServiceSettings, client: httpx.AsyncClient) -> None:
        self._base = settings.note_service_url.rstrip("/")  # type: ignore[attr-defined]
        self._token = settings.internal_service_token
        self._client = client

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    @staticmethod
    def _timeline_entry_from_json(data: dict) -> FeedTimelineEntry:
        group_ids = data.get("group_ids") or []
        if not isinstance(group_ids, set):
            group_ids = set(group_ids)
        created_at = data["created_at"]
        if isinstance(created_at, str):
            created_at = datetime.fromisoformat(created_at.replace("Z", "+00:00"))
        return FeedTimelineEntry(
            note_id=data["note_id"],
            author_id=data["author_id"],
            created_at=created_at,
            visibility=data["visibility"],
            church_id=data.get("church_id"),
            group_ids=group_ids,
        )

    async def get_detail(self, viewer_id: str, note_id: str) -> MeditationNote:
        response = await self._client.get(
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

    async def batch_summaries(self, viewer_id: str, note_ids: list[str]) -> list[NoteSummary]:
        body = NotesBatchRequest(note_ids=note_ids, viewer_id=viewer_id)
        response = await self._client.post(
            f"{self._base}/internal/notes/batch",
            headers=self._headers(),
            json=body.model_dump(),
        )
        response.raise_for_status()
        return NotesBatchResponse.model_validate(response.json()).notes

    async def get_timeline_entry(self, note_id: str) -> FeedTimelineEntry | None:
        response = await self._client.get(
            f"{self._base}/internal/notes/timeline/{note_id}",
            headers=self._headers(),
        )
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return self._timeline_entry_from_json(response.json())

    async def recent_entries_for_feed(
        self,
        viewer_id: str,
        author_ids: list[str],
        since_days: int = 30,
        since: datetime | None = None,
        limit_per_author: int = 10,
        include_global_public: bool = False,
    ) -> list[FeedTimelineEntry]:
        effective_since = since if since is not None else datetime.now(UTC) - timedelta(days=since_days)
        body = RecentNotesByAuthorsRequest(
            author_ids=author_ids,
            viewer_id=viewer_id,
            since=effective_since,
            limit_per_author=limit_per_author,
            include_global_public=include_global_public,
        )
        response = await self._client.post(
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
