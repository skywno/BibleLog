from __future__ import annotations

from typing import Protocol

from common.models import FaithReaction


class SocialReader(Protocol):
    def toggle_reaction(self, note_id: str, user_id: str, reaction: FaithReaction) -> FaithReaction | None: ...

    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]: ...

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]: ...

    def total_engagement(self, note_id: str) -> tuple[int, int]: ...
