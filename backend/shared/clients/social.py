from __future__ import annotations

import httpx

from shared.config import Settings
from shared.models import (
    CommentCountBatchRequest,
    CommentCountBatchResponse,
    FaithReaction,
    ReactionsBatchRequest,
    ReactionsBatchResponse,
    ToggleReactionRequest,
)


class HttpSocialClient:
    """HTTP adapter used by feed-service — mirrors SocialService feed-related methods."""

    def __init__(self, settings: Settings) -> None:
        self._base = settings.social_service_url.rstrip("/")
        self._token = settings.internal_service_token
        self._client = httpx.Client(timeout=10.0)

    def _headers(self) -> dict[str, str]:
        return {"X-Internal-Token": self._token}

    def toggle_reaction(self, note_id: str, user_id: str, reaction: FaithReaction) -> FaithReaction | None:
        response = self._client.post(
            f"{self._base}/internal/reactions/{note_id}/toggle",
            headers=self._headers(),
            json=ToggleReactionRequest(reaction=reaction).model_dump(),
            params={"user_id": user_id},
        )
        response.raise_for_status()
        data = response.json()
        return data.get("active_reaction")

    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]:
        body = ReactionsBatchRequest(note_ids=note_ids, viewer_id=viewer_id)
        response = self._client.post(
            f"{self._base}/internal/reactions/batch",
            headers=self._headers(),
            json=body.model_dump(),
        )
        response.raise_for_status()
        payload = ReactionsBatchResponse.model_validate(response.json())
        result: dict[str, list[tuple[FaithReaction, int, bool]]] = {}
        for item in payload.items:
            result[item.note_id] = [
                (row.type, row.count, row.reacted_by_me) for row in item.reactions
            ]
        return result

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        body = CommentCountBatchRequest(note_ids=note_ids)
        response = self._client.post(
            f"{self._base}/internal/comments/count/batch",
            headers=self._headers(),
            json=body.model_dump(),
        )
        response.raise_for_status()
        payload = CommentCountBatchResponse.model_validate(response.json())
        return {item.note_id: item.count for item in payload.items}

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        response = self._client.get(
            f"{self._base}/internal/engagement/{note_id}",
            headers=self._headers(),
        )
        response.raise_for_status()
        data = response.json()
        return data["reaction_total"], data["comment_count"]
