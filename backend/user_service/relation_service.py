from __future__ import annotations

from common.events.kafka_bus import KafkaEventBus
from common.models import (
    FollowRequest,
    FollowUserSummary,
    FriendRequest,
    SendFriendRequestBody,
    UserSearchResult,
)
from user_service.repositories.relation import RelationRepository
from user_service.repositories.user import UserRepository


class RelationService:
    def __init__(
        self,
        relations: RelationRepository,
        users: UserRepository,
        event_bus: KafkaEventBus,
    ) -> None:
        self._relations = relations
        self._users = users
        self._event_bus = event_bus

    def search_users(self, query: str, user_id: str, limit: int = 20) -> list[UserSearchResult]:
        return self._relations.search_users(query, user_id, limit)

    def list_friends(self, user_id: str) -> list[UserSearchResult]:
        return self._relations.list_friends(user_id)

    async def send_friend_request(self, user_id: str, body: SendFriendRequestBody) -> FriendRequest:
        request = self._relations.send_friend_request(user_id, body.to_user_id)
        await self._event_bus.publish(
            "FriendRequestSent",
            {
                "request_id": request.id,
                "from_user_id": request.from_user_id,
                "to_user_id": request.to_user_id,
            },
        )
        return request

    def list_incoming_friend_requests(self, user_id: str) -> list[FriendRequest]:
        return self._relations.list_incoming_friend_requests(user_id)

    async def accept_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        request = self._relations.accept_friend_request(request_id, user_id)
        await self._event_bus.publish(
            "FriendshipAccepted",
            {
                "request_id": request.id,
                "from_user_id": request.from_user_id,
                "to_user_id": request.to_user_id,
            },
        )
        return request

    def reject_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        return self._relations.reject_friend_request(request_id, user_id)

    def remove_friendship(self, user_id: str, friend_id: str) -> None:
        self._relations.remove_friendship(user_id, friend_id)

    async def follow(self, follower_id: str, followee_id: str) -> None:
        if follower_id == followee_id:
            raise ValueError("Cannot follow yourself")
        followee = self._users.get_user(followee_id)
        if followee.profile_visibility == "private":
            if self._relations.is_approved_follower(follower_id, followee_id):
                return
            if self._relations.has_pending_follow_request(follower_id, followee_id):
                raise ValueError("Follow request already pending")
            request = self._relations.create_follow_request(follower_id, followee_id)
            await self._event_bus.publish(
                "FollowRequestSent",
                {
                    "request_id": request.id,
                    "from_user_id": request.from_user_id,
                    "to_user_id": request.to_user_id,
                },
            )
            return
        self._relations.follow(follower_id, followee_id)
        await self._event_bus.publish(
            "UserFollowed",
            {"follower_id": follower_id, "followee_id": followee_id},
        )

    def unfollow(self, follower_id: str, followee_id: str) -> None:
        self._relations.unfollow(follower_id, followee_id)

    def list_following(self, user_id: str) -> list[FollowUserSummary]:
        return self._relations.list_following(user_id)

    def list_followers(self, user_id: str) -> list[FollowUserSummary]:
        return self._relations.list_followers(user_id)

    def list_incoming_follow_requests(self, user_id: str) -> list[FollowRequest]:
        return self._relations.list_incoming_follow_requests(user_id)

    async def accept_follow_request(self, request_id: str, user_id: str) -> FollowRequest:
        request = self._relations.accept_follow_request(request_id, user_id)
        await self._event_bus.publish(
            "FollowRequestAccepted",
            {
                "request_id": request.id,
                "from_user_id": request.from_user_id,
                "to_user_id": request.to_user_id,
            },
        )
        return request

    def reject_follow_request(self, request_id: str, user_id: str) -> FollowRequest:
        return self._relations.reject_follow_request(request_id, user_id)
