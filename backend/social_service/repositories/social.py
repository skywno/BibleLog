from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime

from shared.models import FAITH_REACTIONS, FaithReaction


class SocialRepository(ABC):
    @abstractmethod
    def toggle_reaction(
        self,
        note_id: str,
        user_id: str,
        reaction: FaithReaction,
    ) -> FaithReaction | None:
        """Returns active reaction or None if removed."""

    @abstractmethod
    def get_reactions(
        self,
        note_id: str,
        viewer_id: str,
    ) -> list[tuple[FaithReaction, int, bool]]: ...

    @abstractmethod
    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]: ...

    @abstractmethod
    def get_comment_count(self, note_id: str) -> int: ...

    @abstractmethod
    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]: ...

    @abstractmethod
    def total_engagement(self, note_id: str) -> tuple[int, int]:
        """Return (reaction_total, comment_count)."""
