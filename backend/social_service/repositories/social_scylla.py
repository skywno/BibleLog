from __future__ import annotations

from datetime import UTC, datetime

from cassandra.cluster import Session

from common.db.scylla import traced_execute
from common.models import FAITH_REACTIONS, FaithReaction
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
        existing = traced_execute(self._session,
            "SELECT reaction_type FROM reactions_by_note WHERE note_id = %s AND user_id = %s",
            (note_id, user_id),
        ).one()
        if existing and existing._asdict()["reaction_type"] == reaction:
            traced_execute(self._session,
                "DELETE FROM reactions_by_note WHERE note_id = %s AND user_id = %s",
                (note_id, user_id),
            )
            return None
        traced_execute(self._session,
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
            rows = traced_execute(self._session,
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
        rows = traced_execute(self._session,
            "SELECT comment_id FROM comments_by_note WHERE note_id = %s",
            (note_id,),
        )
        return sum(1 for _ in rows)

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return {note_id: self.get_comment_count(note_id) for note_id in note_ids}

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        reactions = traced_execute(self._session,
            "SELECT reaction_type FROM reactions_by_note WHERE note_id = %s",
            (note_id,),
        )
        reaction_total = sum(1 for _ in reactions)
        return reaction_total, self.get_comment_count(note_id)

    def add_comment(self, note_id: str, author_id: str, content: str) -> dict:
        import uuid

        comment_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        traced_execute(self._session,
            """
            INSERT INTO comments_by_note (note_id, created_at, comment_id, author_id, content)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (note_id, now, comment_id, author_id, content),
        )
        traced_execute(self._session,
            """
            INSERT INTO comments_by_id (comment_id, note_id, author_id, content, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (comment_id, note_id, author_id, content, now, now),
        )
        return {
            "id": comment_id,
            "note_id": note_id,
            "author_id": author_id,
            "content": content,
            "created_at": now,
            "updated_at": now,
        }

    def list_comments(
        self,
        note_id: str,
        *,
        cursor_created_at: datetime | None = None,
        cursor_comment_id: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]:
        rows = traced_execute(self._session,
            "SELECT created_at, comment_id, author_id, content FROM comments_by_note WHERE note_id = %s",
            (note_id,),
        )
        items: list[dict] = []
        for row in rows:
            data = row._asdict()
            if cursor_created_at is not None:
                if data["created_at"] < cursor_created_at:
                    continue
                if data["created_at"] == cursor_created_at and cursor_comment_id and data["comment_id"] <= cursor_comment_id:
                    continue
            items.append(
                {
                    "id": data["comment_id"],
                    "note_id": note_id,
                    "author_id": data["author_id"],
                    "content": data["content"],
                    "created_at": data["created_at"],
                    "updated_at": data["created_at"],
                }
            )
        items.sort(key=lambda item: (item["created_at"], item["id"]))
        has_more = len(items) > limit
        page = items[:limit]
        next_cursor = None
        if has_more and page:
            last = page[-1]
            next_cursor = f"{last['created_at'].isoformat()}|{last['id']}"
        return page, next_cursor, has_more

    def get_comment(self, comment_id: str) -> dict | None:
        row = traced_execute(self._session,
            "SELECT comment_id, note_id, author_id, content, created_at, updated_at FROM comments_by_id WHERE comment_id = %s",
            (comment_id,),
        ).one()
        if row is None:
            return None
        data = row._asdict()
        return {
            "id": data["comment_id"],
            "note_id": data["note_id"],
            "author_id": data["author_id"],
            "content": data["content"],
            "created_at": data["created_at"],
            "updated_at": data["updated_at"],
        }

    def update_comment(self, comment_id: str, author_id: str, content: str) -> dict:
        existing = self.get_comment(comment_id)
        if existing is None:
            raise KeyError(comment_id)
        if existing["author_id"] != author_id:
            raise PermissionError(comment_id)
        now = datetime.now(UTC)
        traced_execute(self._session,
            "UPDATE comments_by_id SET content = %s, updated_at = %s WHERE comment_id = %s",
            (content, now, comment_id),
        )
        traced_execute(self._session,
            """
            UPDATE comments_by_note SET content = %s
            WHERE note_id = %s AND created_at = %s AND comment_id = %s
            """,
            (content, existing["note_id"], existing["created_at"], comment_id),
        )
        return {**existing, "content": content, "updated_at": now}

    def delete_comment(self, comment_id: str, author_id: str) -> None:
        existing = self.get_comment(comment_id)
        if existing is None:
            raise KeyError(comment_id)
        if existing["author_id"] != author_id:
            raise PermissionError(comment_id)
        traced_execute(self._session,
            "DELETE FROM comments_by_id WHERE comment_id = %s",
            (comment_id,),
        )
        traced_execute(self._session,
            """
            DELETE FROM comments_by_note
            WHERE note_id = %s AND created_at = %s AND comment_id = %s
            """,
            (existing["note_id"], existing["created_at"], comment_id),
        )
