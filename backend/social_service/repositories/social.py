from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime

from common.models import FAITH_REACTIONS, FaithReaction


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

    @abstractmethod
    def add_comment(self, note_id: str, author_id: str, content: str) -> dict: ...

    @abstractmethod
    def list_comments(
        self,
        note_id: str,
        *,
        cursor_created_at: datetime | None = None,
        cursor_comment_id: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]: ...

    @abstractmethod
    def get_comment(self, comment_id: str) -> dict | None: ...

    @abstractmethod
    def update_comment(self, comment_id: str, author_id: str, content: str) -> dict: ...

    @abstractmethod
    def delete_comment(self, comment_id: str, author_id: str) -> None: ...
