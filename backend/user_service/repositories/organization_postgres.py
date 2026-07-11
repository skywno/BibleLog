from __future__ import annotations

import uuid
from datetime import UTC, datetime

import psycopg

from common.domain import UserMembership
from common.models import Church, CreateChurchRequest, CreateSmallGroupRequest, SmallGroup
from user_service.repositories.organization import OrganizationRepository


class PostgresOrganizationRepository(OrganizationRepository):
    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn

    def _ensure_membership_row(self, user_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO user_memberships (user_id, church_id, group_ids)
                VALUES (%s, NULL, '{}')
                ON CONFLICT (user_id) DO NOTHING
                """,
                (user_id,),
            )
        self._conn.commit()

    def get_memberships(self, user_id: str) -> UserMembership:
        self._ensure_membership_row(user_id)
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT church_id, group_ids FROM user_memberships WHERE user_id = %s",
                (user_id,),
            )
            row = cur.fetchone()
        group_ids = row.get("group_ids") or []
        return UserMembership(church_id=row.get("church_id"), group_ids=set(group_ids))

    def _sync_group_ids(self, user_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT group_id FROM group_members WHERE user_id = %s",
                (user_id,),
            )
            group_ids = [row["group_id"] for row in cur.fetchall()]
            cur.execute(
                "UPDATE user_memberships SET group_ids = %s WHERE user_id = %s",
                (group_ids, user_id),
            )
        self._conn.commit()

    def create_church(self, user_id: str, request: CreateChurchRequest) -> Church:
        church_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO churches (id, name, description, created_by, created_at)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (church_id, request.name, request.description, user_id, now),
            )
        self._conn.commit()
        self.join_church(user_id, church_id)
        return Church(
            id=church_id,
            name=request.name,
            description=request.description,
            created_by=user_id,
            created_at=now,
        )

    def get_church(self, church_id: str) -> Church:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT id, name, description, created_by, created_at FROM churches WHERE id = %s",
                (church_id,),
            )
            row = cur.fetchone()
        if row is None:
            raise KeyError(church_id)
        return Church(**row)

    def join_church(self, user_id: str, church_id: str) -> UserMembership:
        self.get_church(church_id)
        self._ensure_membership_row(user_id)
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE user_memberships SET church_id = %s WHERE user_id = %s",
                (church_id, user_id),
            )
        self._conn.commit()
        return self.get_memberships(user_id)

    def leave_church(self, user_id: str, church_id: str) -> UserMembership:
        membership = self.get_memberships(user_id)
        if membership.church_id != church_id:
            raise ValueError("Not a member of this church")
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE user_memberships SET church_id = NULL WHERE user_id = %s",
                (user_id,),
            )
        self._conn.commit()
        return self.get_memberships(user_id)

    def create_small_group(self, user_id: str, request: CreateSmallGroupRequest) -> SmallGroup:
        group_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO small_groups (id, church_id, name, leader_id, created_at)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (group_id, request.church_id, request.name, user_id, now),
            )
            cur.execute(
                """
                INSERT INTO group_members (group_id, user_id, role, joined_at)
                VALUES (%s, %s, 'leader', %s)
                """,
                (group_id, user_id, now),
            )
        self._conn.commit()
        self._ensure_membership_row(user_id)
        self._sync_group_ids(user_id)
        return SmallGroup(
            id=group_id,
            church_id=request.church_id,
            name=request.name,
            leader_id=user_id,
            created_at=now,
        )

    def get_small_group(self, group_id: str) -> SmallGroup:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT id, church_id, name, leader_id, created_at FROM small_groups WHERE id = %s",
                (group_id,),
            )
            row = cur.fetchone()
        if row is None:
            raise KeyError(group_id)
        return SmallGroup(**row)

    def join_small_group(self, user_id: str, group_id: str) -> UserMembership:
        self.get_small_group(group_id)
        self._ensure_membership_row(user_id)
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO group_members (group_id, user_id, role, joined_at)
                VALUES (%s, %s, 'member', %s)
                ON CONFLICT DO NOTHING
                """,
                (group_id, user_id, now),
            )
        self._conn.commit()
        self._sync_group_ids(user_id)
        return self.get_memberships(user_id)

    def leave_small_group(self, user_id: str, group_id: str) -> UserMembership:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM group_members WHERE group_id = %s AND user_id = %s",
                (group_id, user_id),
            )
        self._conn.commit()
        self._sync_group_ids(user_id)
        return self.get_memberships(user_id)
