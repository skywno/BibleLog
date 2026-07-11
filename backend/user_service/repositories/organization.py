from __future__ import annotations

from abc import ABC, abstractmethod

from common.domain import UserMembership
from common.models import Church, CreateChurchRequest, CreateSmallGroupRequest, SmallGroup


class OrganizationRepository(ABC):
    @abstractmethod
    def create_church(self, user_id: str, request: CreateChurchRequest) -> Church: ...

    @abstractmethod
    def get_church(self, church_id: str) -> Church: ...

    @abstractmethod
    def join_church(self, user_id: str, church_id: str) -> UserMembership: ...

    @abstractmethod
    def leave_church(self, user_id: str, church_id: str) -> UserMembership: ...

    @abstractmethod
    def create_small_group(self, user_id: str, request: CreateSmallGroupRequest) -> SmallGroup: ...

    @abstractmethod
    def get_small_group(self, group_id: str) -> SmallGroup: ...

    @abstractmethod
    def join_small_group(self, user_id: str, group_id: str) -> UserMembership: ...

    @abstractmethod
    def leave_small_group(self, user_id: str, group_id: str) -> UserMembership: ...

    @abstractmethod
    def get_memberships(self, user_id: str) -> UserMembership: ...

    @abstractmethod
    def search_churches(self, query: str, limit: int) -> list[Church]: ...

    @abstractmethod
    def search_small_groups(self, query: str, limit: int) -> list[SmallGroup]: ...
