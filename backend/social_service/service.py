from __future__ import annotations

from datetime import datetime

from common.clients.user import HttpUserClient
from common.events.kafka_bus import KafkaEventBus
from common.models import (
    Comment,
    CommentPageResponse,
    CreateCommentRequest,
    FAITH_REACTIONS,
    FaithReaction,
    NoteReactions,
    ReactionCount,
    UpdateCommentRequest,
)
from social_service.repositories.social import SocialRepository


class SocialService:
    def __init__(
        self,
        social: SocialRepository,
        users: HttpUserClient,
        event_bus: KafkaEventBus,
    ) -> None:
        self._social = social
        self._users = users
        self._event_bus = event_bus

    async def toggle_reaction(
        self,
        note_id: str,
        user_id: str,
        reaction: FaithReaction,
    ) -> FaithReaction | None:
        active = self._social.toggle_reaction(note_id, user_id, reaction)
        await self._event_bus.publish(
            "ReactionToggled",
            {"note_id": note_id, "user_id": user_id, "reaction": reaction},
        )
        return active

    def get_reactions_batch(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> dict[str, list[tuple[FaithReaction, int, bool]]]:
        return self._social.get_reactions_batch(note_ids, viewer_id)

    def get_comment_counts_batch(self, note_ids: list[str]) -> dict[str, int]:
        return self._social.get_comment_counts_batch(note_ids)

    def total_engagement(self, note_id: str) -> tuple[int, int]:
        return self._social.total_engagement(note_id)

    def reactions_batch_response(
        self,
        note_ids: list[str],
        viewer_id: str,
    ) -> list[NoteReactions]:
        batch = self.get_reactions_batch(note_ids, viewer_id)
        return [
            NoteReactions(
                note_id=note_id,
                reactions=[
                    ReactionCount(type=reaction, count=count, reacted_by_me=reacted_by_me)
                    for reaction, count, reacted_by_me in batch.get(note_id, [])
                ],
            )
            for note_id in note_ids
        ]

    async def create_comment(
        self,
        note_id: str,
        user_id: str,
        request: CreateCommentRequest,
    ) -> Comment:
        raw = self._social.add_comment(note_id, user_id, request.content)
        author = await self._users.get_user(user_id)
        comment = Comment(
            id=raw["id"],
            note_id=raw["note_id"],
            author_id=raw["author_id"],
            author_name=author.nickname,
            content=raw["content"],
            created_at=raw["created_at"],
            updated_at=raw["updated_at"],
        )
        await self._event_bus.publish(
            "CommentCreated",
            {"note_id": note_id, "comment_id": comment.id, "author_id": user_id},
        )
        return comment

    async def list_comments(
        self,
        note_id: str,
        *,
        cursor: str | None = None,
        limit: int = 20,
    ) -> CommentPageResponse:
        cursor_created_at: datetime | None = None
        cursor_comment_id: str | None = None
        if cursor:
            created_at_raw, cursor_comment_id = cursor.split("|", 1)
            cursor_created_at = datetime.fromisoformat(created_at_raw.replace("Z", "+00:00"))
        rows, next_cursor, has_more = self._social.list_comments(
            note_id,
            cursor_created_at=cursor_created_at,
            cursor_comment_id=cursor_comment_id,
            limit=limit,
        )
        items: list[Comment] = []
        for row in rows:
            author = await self._users.get_user(row["author_id"])
            items.append(
                Comment(
                    id=row["id"],
                    note_id=row["note_id"],
                    author_id=row["author_id"],
                    author_name=author.nickname,
                    content=row["content"],
                    created_at=row["created_at"],
                    updated_at=row["updated_at"],
                )
            )
        return CommentPageResponse(items=items, next_cursor=next_cursor, has_more=has_more)

    async def update_comment(
        self,
        comment_id: str,
        user_id: str,
        request: UpdateCommentRequest,
    ) -> Comment:
        raw = self._social.update_comment(comment_id, user_id, request.content)
        author = await self._users.get_user(user_id)
        return Comment(
            id=raw["id"],
            note_id=raw["note_id"],
            author_id=raw["author_id"],
            author_name=author.nickname,
            content=raw["content"],
            created_at=raw["created_at"],
            updated_at=raw["updated_at"],
        )

    def delete_comment(self, comment_id: str, user_id: str) -> None:
        self._social.delete_comment(comment_id, user_id)
