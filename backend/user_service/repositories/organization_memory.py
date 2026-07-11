from __future__ import annotations

import uuid
from datetime import UTC, datetime

from common.domain import UserMembership
from common.models import Church, CreateChurchRequest, CreateSmallGroupRequest, SmallGroup
from user_service.repositories.organization import OrganizationRepository


class MemoryOrganizationRepository(OrganizationRepository):
    def __init__(self) -> None:
        self.churches: dict[str, Church] = {}
        self.groups: dict[str, SmallGroup] = {}
        self.memberships: dict[str, UserMembership] = {}
        self.group_members: dict[str, set[str]] = {}

    def get_memberships(self, user_id: str) -> UserMembership:
        return self.memberships.get(user_id, UserMembership(church_id=None, group_ids=set()))

    def create_church(self, user_id: str, request: CreateChurchRequest) -> Church:
        church_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        church = Church(
            id=church_id,
            name=request.name,
            description=request.description,
            created_by=user_id,
            created_at=now,
        )
        self.churches[church_id] = church
        self.join_church(user_id, church_id)
        return church

    def get_church(self, church_id: str) -> Church:
        church = self.churches.get(church_id)
        if church is None:
            raise KeyError(church_id)
        return church

    def join_church(self, user_id: str, church_id: str) -> UserMembership:
        self.get_church(church_id)
        membership = self.get_memberships(user_id)
        self.memberships[user_id] = UserMembership(
            church_id=church_id,
            group_ids=membership.group_ids,
        )
        return self.memberships[user_id]

    def leave_church(self, user_id: str, church_id: str) -> UserMembership:
        membership = self.get_memberships(user_id)
        if membership.church_id != church_id:
            raise ValueError("Not a member of this church")
        self.memberships[user_id] = UserMembership(
            church_id=None,
            group_ids=membership.group_ids,
        )
        return self.memberships[user_id]

    def create_small_group(self, user_id: str, request: CreateSmallGroupRequest) -> SmallGroup:
        group_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        group = SmallGroup(
            id=group_id,
            church_id=request.church_id,
            name=request.name,
            leader_id=user_id,
            created_at=now,
        )
        self.groups[group_id] = group
        self.join_small_group(user_id, group_id)
        return group

    def get_small_group(self, group_id: str) -> SmallGroup:
        group = self.groups.get(group_id)
        if group is None:
            raise KeyError(group_id)
        return group

    def join_small_group(self, user_id: str, group_id: str) -> UserMembership:
        self.get_small_group(group_id)
        self.group_members.setdefault(group_id, set()).add(user_id)
        membership = self.get_memberships(user_id)
        group_ids = set(membership.group_ids)
        group_ids.add(group_id)
        self.memberships[user_id] = UserMembership(
            church_id=membership.church_id,
            group_ids=group_ids,
        )
        return self.memberships[user_id]

    def leave_small_group(self, user_id: str, group_id: str) -> UserMembership:
        self.group_members.setdefault(group_id, set()).discard(user_id)
        membership = self.get_memberships(user_id)
        group_ids = set(membership.group_ids)
        group_ids.discard(group_id)
        self.memberships[user_id] = UserMembership(
            church_id=membership.church_id,
            group_ids=group_ids,
        )
        return self.memberships[user_id]
