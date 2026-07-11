from __future__ import annotations

from abc import ABC, abstractmethod

from common.domain import UserMembership
from common.models import FollowUserSummary, FriendRequest, UserSearchResult


class RelationRepository(ABC):
    @abstractmethod
    def list_friend_ids(self, user_id: str) -> list[str]: ...

    @abstractmethod
    def get_membership(self, user_id: str) -> UserMembership: ...

    @abstractmethod
    def list_group_member_ids(self, group_ids: set[str]) -> list[str]: ...

    @abstractmethod
    def list_church_member_ids(self, church_id: str) -> list[str]: ...

    @abstractmethod
    def search_users(self, query: str, exclude_user_id: str, limit: int) -> list[UserSearchResult]: ...

    @abstractmethod
    def list_friends(self, user_id: str) -> list[UserSearchResult]: ...

    @abstractmethod
    def send_friend_request(self, from_user_id: str, to_user_id: str) -> FriendRequest: ...

    @abstractmethod
    def list_incoming_friend_requests(self, user_id: str) -> list[FriendRequest]: ...

    @abstractmethod
    def accept_friend_request(self, request_id: str, user_id: str) -> FriendRequest: ...

    @abstractmethod
    def reject_friend_request(self, request_id: str, user_id: str) -> FriendRequest: ...

    @abstractmethod
    def remove_friendship(self, user_id: str, friend_id: str) -> None: ...

    @abstractmethod
    def follow(self, follower_id: str, followee_id: str) -> None: ...

    @abstractmethod
    def unfollow(self, follower_id: str, followee_id: str) -> None: ...

    @abstractmethod
    def list_following(self, user_id: str) -> list[FollowUserSummary]: ...

    @abstractmethod
    def list_followers(self, user_id: str) -> list[FollowUserSummary]: ...

    @abstractmethod
    def list_following_ids(self, user_id: str) -> list[str]: ...

    @abstractmethod
    def list_follower_ids(self, user_id: str) -> list[str]: ...
