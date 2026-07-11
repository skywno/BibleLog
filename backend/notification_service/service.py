from __future__ import annotations

import logging

import httpx

from common.clients.note import HttpNoteClient
from common.clients.relation import HttpRelationClient
from common.events.kafka_bus import KafkaEventBus
from common.models import NotificationItem, NotificationPageResponse
from notification_service.repositories.notification import NotificationRepository
from notification_service.websocket_hub import WebSocketHub

logger = logging.getLogger(__name__)


class NotificationService:
    def __init__(
        self,
        notifications: NotificationRepository,
        relations: HttpRelationClient,
        notes: HttpNoteClient,
        hub: WebSocketHub,
        event_bus: KafkaEventBus,
    ) -> None:
        self._notifications = notifications
        self._relations = relations
        self._notes = notes
        self._hub = hub
        self._event_bus = event_bus

    def register_event_handlers(self) -> None:
        self._event_bus.subscribe("NotePublished", self._on_note_published)
        self._event_bus.subscribe("CommentCreated", self._on_comment_created)
        self._event_bus.subscribe("ReactionToggled", self._on_reaction_toggled)
        self._event_bus.subscribe("FriendRequestSent", self._on_friend_request_sent)
        self._event_bus.subscribe("FriendshipAccepted", self._on_friendship_accepted)
        self._event_bus.subscribe("UserFollowed", self._on_user_followed)
        self._event_bus.subscribe("FollowRequestSent", self._on_follow_request_sent)
        self._event_bus.subscribe("FollowRequestAccepted", self._on_follow_request_accepted)

    async def _notify(self, user_id: str, event_type: str, payload: dict) -> None:
        stored = self._notifications.create(user_id, event_type, payload)
        await self._hub.push(user_id, event_type, stored["payload"])

    async def _on_note_published(self, payload: dict) -> None:
        author_id = payload.get("author_id")
        if not author_id:
            return
        recipients: set[str] = set()
        try:
            friends = await self._relations.list_friend_ids(author_id)
            followers = await self._relations.list_follower_ids(author_id)
            recipients.update(friends)
            recipients.update(followers)
        except httpx.HTTPError:
            logger.exception("Failed to resolve note recipients")
        for user_id in recipients:
            if user_id == author_id:
                continue
            await self._notify(user_id, "note_published", payload)

    async def _on_comment_created(self, payload: dict) -> None:
        note_id = payload.get("note_id")
        commenter_id = payload.get("author_id")
        if not note_id or not commenter_id:
            return
        try:
            entry = await self._notes.get_timeline_entry(note_id)
            if entry is None:
                return
            note_author_id = entry.author_id
            if note_author_id != commenter_id:
                await self._notify(
                    note_author_id,
                    "comment_created",
                    {**payload, "note_author_id": note_author_id},
                )
        except httpx.HTTPError:
            logger.exception("Failed to resolve note author for comment notification")

    async def _on_reaction_toggled(self, payload: dict) -> None:
        note_id = payload.get("note_id")
        user_id = payload.get("user_id")
        if not note_id or not user_id:
            return
        try:
            entry = await self._notes.get_timeline_entry(note_id)
            if entry is None:
                return
            note_author_id = entry.author_id
            if note_author_id != user_id:
                await self._notify(
                    note_author_id,
                    "reaction_toggled",
                    {**payload, "note_author_id": note_author_id},
                )
        except httpx.HTTPError:
            logger.exception("Failed to resolve note author for reaction notification")

    async def _on_friend_request_sent(self, payload: dict) -> None:
        to_user_id = payload.get("to_user_id")
        if to_user_id:
            await self._notify(to_user_id, "friend_request", payload)

    async def _on_friendship_accepted(self, payload: dict) -> None:
        for user_id in {payload.get("from_user_id"), payload.get("to_user_id")}:
            if user_id:
                await self._notify(user_id, "friend_accepted", payload)

    async def _on_user_followed(self, payload: dict) -> None:
        followee_id = payload.get("followee_id")
        if followee_id:
            await self._notify(followee_id, "follow", payload)

    async def _on_follow_request_sent(self, payload: dict) -> None:
        to_user_id = payload.get("to_user_id")
        if to_user_id:
            await self._notify(to_user_id, "follow_request", payload)

    async def _on_follow_request_accepted(self, payload: dict) -> None:
        from_user_id = payload.get("from_user_id")
        if from_user_id:
            await self._notify(from_user_id, "follow_accepted", payload)

    def list_notifications(
        self,
        user_id: str,
        *,
        cursor: str | None = None,
        limit: int = 20,
    ) -> NotificationPageResponse:
        rows, next_cursor, has_more = self._notifications.list_for_user(
            user_id,
            cursor=cursor,
            limit=limit,
        )
        return NotificationPageResponse(
            items=[
                NotificationItem(
                    id=row["id"],
                    event_type=row["event_type"],
                    payload=row["payload"],
                    read=row["read"],
                    created_at=row["created_at"],
                )
                for row in rows
            ],
            next_cursor=next_cursor,
            has_more=has_more,
        )
