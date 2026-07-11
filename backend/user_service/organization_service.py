from __future__ import annotations

from common.domain import UserMembership
from common.models import Church, CreateChurchRequest, CreateSmallGroupRequest, SmallGroup
from user_service.repositories.organization import OrganizationRepository


class OrganizationService:
    def __init__(self, organizations: OrganizationRepository) -> None:
        self._organizations = organizations

    def create_church(self, user_id: str, request: CreateChurchRequest) -> Church:
        return self._organizations.create_church(user_id, request)

    def get_church(self, church_id: str) -> Church:
        return self._organizations.get_church(church_id)

    def join_church(self, user_id: str, church_id: str) -> UserMembership:
        return self._organizations.join_church(user_id, church_id)

    def leave_church(self, user_id: str, church_id: str) -> UserMembership:
        return self._organizations.leave_church(user_id, church_id)

    def create_small_group(self, user_id: str, request: CreateSmallGroupRequest) -> SmallGroup:
        return self._organizations.create_small_group(user_id, request)

    def get_small_group(self, group_id: str) -> SmallGroup:
        return self._organizations.get_small_group(group_id)

    def join_small_group(self, user_id: str, group_id: str) -> UserMembership:
        return self._organizations.join_small_group(user_id, group_id)

    def leave_small_group(self, user_id: str, group_id: str) -> UserMembership:
        return self._organizations.leave_small_group(user_id, group_id)

    def get_memberships(self, user_id: str) -> UserMembership:
        return self._organizations.get_memberships(user_id)
