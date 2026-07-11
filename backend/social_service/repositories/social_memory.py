from __future__ import annotations

from datetime import UTC, datetime

from common.models import FAITH_REACTIONS, FaithReaction
from social_service.repositories.social import SocialRepository


class MemorySocialRepository(SocialRepository):
    def __init__(self) -> None:
        self._reactions: dict[str, dict[str, FaithReaction]] = {}
        self._comments_by_note: dict[str, list[dict]] = {}
        self._comments_by_id: dict[str, dict] = {}

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
        return len(self._comments_by_note.get(note_id, []))

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return {note_id: self.get_comment_count(note_id) for note_id in note_ids}

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        return len(self._reactions.get(note_id, {})), self.get_comment_count(note_id)

    def add_comment(self, note_id: str, author_id: str, content: str) -> dict:
        import uuid

        comment_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        comment = {
            "id": comment_id,
            "note_id": note_id,
            "author_id": author_id,
            "content": content,
            "created_at": now,
            "updated_at": now,
        }
        self._comments_by_note.setdefault(note_id, []).append(comment)
        self._comments_by_id[comment_id] = comment
        return comment

    def list_comments(
        self,
        note_id: str,
        *,
        cursor_created_at: datetime | None = None,
        cursor_comment_id: str | None = None,
        limit: int = 20,
    ) -> tuple[list[dict], str | None, bool]:
        items = sorted(
            self._comments_by_note.get(note_id, []),
            key=lambda item: (item["created_at"], item["id"]),
        )
        if cursor_created_at is not None:
            filtered = []
            for item in items:
                if item["created_at"] < cursor_created_at:
                    continue
                if item["created_at"] == cursor_created_at and cursor_comment_id and item["id"] <= cursor_comment_id:
                    continue
                filtered.append(item)
            items = filtered
        has_more = len(items) > limit
        page = items[:limit]
        next_cursor = None
        if has_more and page:
            last = page[-1]
            next_cursor = f"{last['created_at'].isoformat()}|{last['id']}"
        return page, next_cursor, has_more

    def get_comment(self, comment_id: str) -> dict | None:
        return self._comments_by_id.get(comment_id)

    def update_comment(self, comment_id: str, author_id: str, content: str) -> dict:
        existing = self.get_comment(comment_id)
        if existing is None:
            raise KeyError(comment_id)
        if existing["author_id"] != author_id:
            raise PermissionError(comment_id)
        now = datetime.now(UTC)
        updated = {**existing, "content": content, "updated_at": now}
        self._comments_by_id[comment_id] = updated
        note_comments = self._comments_by_note.get(existing["note_id"], [])
        self._comments_by_note[existing["note_id"]] = [
            updated if item["id"] == comment_id else item for item in note_comments
        ]
        return updated

    def delete_comment(self, comment_id: str, author_id: str) -> None:
        existing = self.get_comment(comment_id)
        if existing is None:
            raise KeyError(comment_id)
        if existing["author_id"] != author_id:
            raise PermissionError(comment_id)
        del self._comments_by_id[comment_id]
        self._comments_by_note[existing["note_id"]] = [
            item for item in self._comments_by_note.get(existing["note_id"], []) if item["id"] != comment_id
        ]
