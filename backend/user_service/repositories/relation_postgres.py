from __future__ import annotations

import psycopg

from shared.domain import UserMembership
from user_service.repositories.relation import RelationRepository


class PostgresRelationRepository(RelationRepository):
    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn

    def list_friend_ids(self, user_id: str) -> list[str]:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT friend_id FROM friendships WHERE user_id = %s",
                (user_id,),
            )
            rows = cur.fetchall()
        return [row["friend_id"] for row in rows]

    def get_membership(self, user_id: str) -> UserMembership:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT church_id, group_ids FROM user_memberships WHERE user_id = %s",
                (user_id,),
            )
            row = cur.fetchone()
        if row is None:
            return UserMembership(church_id=None, group_ids=set())
        group_ids = row.get("group_ids") or []
        return UserMembership(church_id=row.get("church_id"), group_ids=set(group_ids))

    def list_group_member_ids(self, group_ids: set[str]) -> list[str]:
        if not group_ids:
            return []
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT user_id FROM user_memberships
                WHERE group_ids && %s::text[]
                """,
                (list(group_ids),),
            )
            rows = cur.fetchall()
        return [row["user_id"] for row in rows]

    def list_church_member_ids(self, church_id: str) -> list[str]:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT user_id FROM user_memberships WHERE church_id = %s",
                (church_id,),
            )
            rows = cur.fetchall()
        return [row["user_id"] for row in rows]
