from __future__ import annotations

from shared.domain import UserMembership
from user_service.repositories.relation import RelationRepository


class MemoryRelationRepository(RelationRepository):
    def __init__(self) -> None:
        self.friendships: dict[str, set[str]] = {}
        self.memberships: dict[str, UserMembership] = {}

    def list_friend_ids(self, user_id: str) -> list[str]:
        return sorted(self.friendships.get(user_id, set()))

    def get_membership(self, user_id: str) -> UserMembership:
        return self.memberships.get(user_id, UserMembership(church_id=None, group_ids=set()))

    def list_group_member_ids(self, group_ids: set[str]) -> list[str]:
        if not group_ids:
            return []
        result: set[str] = set()
        for user_id, membership in self.memberships.items():
            if membership.group_ids & group_ids:
                result.add(user_id)
        return sorted(result)

    def list_church_member_ids(self, church_id: str) -> list[str]:
        return sorted(
            user_id
            for user_id, membership in self.memberships.items()
            if membership.church_id == church_id
        )

    def set_membership(self, user_id: str, membership: UserMembership) -> None:
        self.memberships[user_id] = membership

    def add_friendship(self, user_id: str, friend_id: str) -> None:
        self.friendships.setdefault(user_id, set()).add(friend_id)
        self.friendships.setdefault(friend_id, set()).add(user_id)
