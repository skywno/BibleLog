from __future__ import annotations

from datetime import UTC, datetime

from shared.models import FAITH_REACTIONS, FaithReaction
from social_service.repositories.social import SocialRepository


class MemorySocialRepository(SocialRepository):
    def __init__(self) -> None:
        self._reactions: dict[str, dict[str, FaithReaction]] = {}
        self._comments: dict[str, int] = {}

    def toggle_reaction(
        self,
        note_id: str,
        user_id: str,
        reaction: FaithReaction,
    ) -> FaithReaction | None:
        note_reactions = self._reactions.setdefault(note_id, {})
        current = note_reactions.get(user_id)
        if current == reaction:
            del note_reactions[user_id]
            return None
        note_reactions[user_id] = reaction
        return reaction

    def get_reactions(
        self,
        note_id: str,
        viewer_id: str,
    ) -> list[tuple[FaithReaction, int, bool]]:
        return self.get_reactions_batch([note_id], viewer_id)[note_id]

    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]:
        result: dict[str, list[tuple[FaithReaction, int, bool]]] = {}
        for note_id in note_ids:
            note_reactions = self._reactions.get(note_id, {})
            counts: dict[FaithReaction, int] = {reaction: 0 for reaction in FAITH_REACTIONS}
            mine = note_reactions.get(viewer_id)
            for reaction in note_reactions.values():
                counts[reaction] += 1
            result[note_id] = [
                (reaction, counts[reaction], mine == reaction)
                for reaction in FAITH_REACTIONS
                if counts[reaction] > 0 or mine == reaction
            ]
        return result

    def get_comment_count(self, note_id: str) -> int:
        return self._comments.get(note_id, 0)

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return {note_id: self._comments.get(note_id, 0) for note_id in note_ids}

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        return len(self._reactions.get(note_id, {})), self._comments.get(note_id, 0)
