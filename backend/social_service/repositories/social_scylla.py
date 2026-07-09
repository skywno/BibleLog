from __future__ import annotations

from datetime import UTC, datetime

from cassandra.cluster import Session

from shared.models import FAITH_REACTIONS, FaithReaction
from social_service.repositories.social import SocialRepository


class ScyllaSocialRepository(SocialRepository):
    def __init__(self, session: Session) -> None:
        self._session = session

    def toggle_reaction(
        self,
        note_id: str,
        user_id: str,
        reaction: FaithReaction,
    ) -> FaithReaction | None:
        existing = self._session.execute(
            "SELECT reaction_type FROM reactions_by_note WHERE note_id = %s AND user_id = %s",
            (note_id, user_id),
        ).one()
        if existing and existing._asdict()["reaction_type"] == reaction:
            self._session.execute(
                "DELETE FROM reactions_by_note WHERE note_id = %s AND user_id = %s",
                (note_id, user_id),
            )
            return None
        self._session.execute(
            """
            INSERT INTO reactions_by_note (note_id, user_id, reaction_type, created_at)
            VALUES (%s, %s, %s, %s)
            """,
            (note_id, user_id, reaction, datetime.now(UTC)),
        )
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
            rows = self._session.execute(
                "SELECT user_id, reaction_type FROM reactions_by_note WHERE note_id = %s",
                (note_id,),
            )
            counts: dict[FaithReaction, int] = {reaction: 0 for reaction in FAITH_REACTIONS}
            mine: FaithReaction | None = None
            for row in rows:
                data = row._asdict()
                reaction = data["reaction_type"]
                counts[reaction] += 1
                if data["user_id"] == viewer_id:
                    mine = reaction
            result[note_id] = [
                (reaction, counts[reaction], mine == reaction)
                for reaction in FAITH_REACTIONS
                if counts[reaction] > 0 or mine == reaction
            ]
        return result

    def get_comment_count(self, note_id: str) -> int:
        rows = self._session.execute(
            "SELECT comment_id FROM comments_by_note WHERE note_id = %s",
            (note_id,),
        )
        return sum(1 for _ in rows)

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return {note_id: self.get_comment_count(note_id) for note_id in note_ids}

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        reactions = self._session.execute(
            "SELECT reaction_type FROM reactions_by_note WHERE note_id = %s",
            (note_id,),
        )
        reaction_total = sum(1 for _ in reactions)
        return reaction_total, self.get_comment_count(note_id)
