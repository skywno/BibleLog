from __future__ import annotations

import uuid
from datetime import UTC, datetime

from common.domain import UserMembership
from common.models import FollowUserSummary, FriendRequest, UserSearchResult
from user_service.repositories.relation import RelationRepository


class MemoryRelationRepository(RelationRepository):
    def __init__(self) -> None:
        self.friendships: dict[str, set[str]] = {}
        self.memberships: dict[str, UserMembership] = {}
        self.group_members: dict[str, set[str]] = {}
        self.users: dict[str, UserSearchResult] = {}
        self.friend_requests: dict[str, FriendRequest] = {}
        self.follows: dict[str, set[str]] = {}

    def register_user(self, user_id: str, nickname: str, bio: str = "") -> None:
        self.users[user_id] = UserSearchResult(id=user_id, nickname=nickname, bio=bio)

    def list_friend_ids(self, user_id: str) -> list[str]:
        return sorted(self.friendships.get(user_id, set()))

    def get_membership(self, user_id: str) -> UserMembership:
        return self.memberships.get(user_id, UserMembership(church_id=None, group_ids=set()))

    def list_group_member_ids(self, group_ids: set[str]) -> list[str]:
        if not group_ids:
            return []
        result: set[str] = set()
        for group_id in group_ids:
            result.update(self.group_members.get(group_id, set()))
        return sorted(result)

    def list_church_member_ids(self, church_id: str) -> list[str]:
        return sorted(
            user_id
            for user_id, membership in self.memberships.items()
            if membership.church_id == church_id
        )

    def search_users(self, query: str, exclude_user_id: str, limit: int) -> list[UserSearchResult]:
        results = [
            user
            for user in self.users.values()
            if user.id != exclude_user_id and user.nickname.lower().startswith(query.lower())
        ]
        return sorted(results, key=lambda u: u.nickname)[:limit]

    def list_friends(self, user_id: str) -> list[UserSearchResult]:
        return [
            self.users[friend_id]
            for friend_id in self.list_friend_ids(user_id)
            if friend_id in self.users
        ]

    def send_friend_request(self, from_user_id: str, to_user_id: str) -> FriendRequest:
        if from_user_id == to_user_id:
            raise ValueError("Cannot send friend request to yourself")
        if to_user_id in self.friendships.get(from_user_id, set()):
            raise ValueError("Already friends")
        for request in self.friend_requests.values():
            if request.status == "pending" and {
                request.from_user_id,
                request.to_user_id,
            } == {from_user_id, to_user_id}:
                raise ValueError("Friend request already pending")
        request_id = str(uuid.uuid4())
        from_user = self.users.get(from_user_id, UserSearchResult(id=from_user_id, nickname=from_user_id))
        request = FriendRequest(
            id=request_id,
            from_user_id=from_user_id,
            from_user_nickname=from_user.nickname,
            to_user_id=to_user_id,
            status="pending",
            created_at=datetime.now(UTC),
        )
        self.friend_requests[request_id] = request
        return request

    def list_incoming_friend_requests(self, user_id: str) -> list[FriendRequest]:
        return [
            request
            for request in self.friend_requests.values()
            if request.to_user_id == user_id and request.status == "pending"
        ]

    def accept_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        request = self.friend_requests.get(request_id)
        if request is None:
            raise KeyError(request_id)
        if request.to_user_id != user_id:
            raise PermissionError(request_id)
        if request.status != "pending":
            raise ValueError("Request is not pending")
        self.add_friendship(request.from_user_id, request.to_user_id)
        updated = request.model_copy(update={"status": "accepted"})
        self.friend_requests[request_id] = updated
        return updated

    def reject_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        request = self.friend_requests.get(request_id)
        if request is None:
            raise KeyError(request_id)
        if request.to_user_id != user_id:
            raise PermissionError(request_id)
        if request.status != "pending":
            raise ValueError("Request is not pending")
        updated = request.model_copy(update={"status": "rejected"})
        self.friend_requests[request_id] = updated
        return updated

    def remove_friendship(self, user_id: str, friend_id: str) -> None:
        self.friendships.setdefault(user_id, set()).discard(friend_id)
        self.friendships.setdefault(friend_id, set()).discard(user_id)

    def follow(self, follower_id: str, followee_id: str) -> None:
        if follower_id == followee_id:
            raise ValueError("Cannot follow yourself")
        self.follows.setdefault(follower_id, set()).add(followee_id)

    def unfollow(self, follower_id: str, followee_id: str) -> None:
        self.follows.setdefault(follower_id, set()).discard(followee_id)

    def list_following(self, user_id: str) -> list[FollowUserSummary]:
        return [
            FollowUserSummary(id=user.id, nickname=user.nickname, bio=user.bio)
            for followee_id in sorted(self.follows.get(user_id, set()))
            if (user := self.users.get(followee_id)) is not None
        ]

    def list_followers(self, user_id: str) -> list[FollowUserSummary]:
        followers: list[FollowUserSummary] = []
        for follower_id, followees in self.follows.items():
            if user_id in followees and (user := self.users.get(follower_id)) is not None:
                followers.append(FollowUserSummary(id=user.id, nickname=user.nickname, bio=user.bio))
        return sorted(followers, key=lambda u: u.nickname)

    def list_following_ids(self, user_id: str) -> list[str]:
        return sorted(self.follows.get(user_id, set()))

    def list_follower_ids(self, user_id: str) -> list[str]:
        return [
            follower_id
            for follower_id, followees in self.follows.items()
            if user_id in followees
        ]

    def set_membership(self, user_id: str, membership: UserMembership) -> None:
        self.memberships[user_id] = membership

    def add_friendship(self, user_id: str, friend_id: str) -> None:
        self.friendships.setdefault(user_id, set()).add(friend_id)
        self.friendships.setdefault(friend_id, set()).add(user_id)

    def add_group_member(self, group_id: str, user_id: str) -> None:
        self.group_members.setdefault(group_id, set()).add(user_id)
