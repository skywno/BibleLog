from __future__ import annotations

from abc import ABC, abstractmethod

from common.domain import UserMembership


class RelationRepository(ABC):
    @abstractmethod
    def list_friend_ids(self, user_id: str) -> list[str]: ...

    @abstractmethod
    def get_membership(self, user_id: str) -> UserMembership: ...

    @abstractmethod
    def list_group_member_ids(self, group_ids: set[str]) -> list[str]: ...

    @abstractmethod
    def list_church_member_ids(self, church_id: str) -> list[str]: ...
