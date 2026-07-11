from __future__ import annotations

import uuid
from datetime import UTC, datetime

import psycopg

from common.domain import UserMembership
from common.models import FollowUserSummary, FollowRequest, FriendRequest, UserSearchResult
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
                "SELECT DISTINCT user_id FROM group_members WHERE group_id = ANY(%s)",
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

    def search_users(self, query: str, exclude_user_id: str, limit: int) -> list[UserSearchResult]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, nickname, bio, photo_url FROM users
                WHERE id != %s AND nickname ILIKE %s
                ORDER BY nickname
                LIMIT %s
                """,
                (exclude_user_id, f"{query}%", limit),
            )
            rows = cur.fetchall()
        return [UserSearchResult(**row) for row in rows]

    def list_friends(self, user_id: str) -> list[UserSearchResult]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT u.id, u.nickname, u.bio, u.photo_url
                FROM friendships f
                JOIN users u ON u.id = f.friend_id
                WHERE f.user_id = %s
                ORDER BY u.nickname
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [UserSearchResult(**row) for row in rows]

    def send_friend_request(self, from_user_id: str, to_user_id: str) -> FriendRequest:
        if from_user_id == to_user_id:
            raise ValueError("Cannot send friend request to yourself")
        request_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT 1 FROM friendships WHERE user_id = %s AND friend_id = %s",
                (from_user_id, to_user_id),
            )
            if cur.fetchone():
                raise ValueError("Already friends")
            cur.execute(
                """
                SELECT id, status FROM friend_requests
                WHERE from_user_id = %s AND to_user_id = %s
                   OR from_user_id = %s AND to_user_id = %s
                """,
                (from_user_id, to_user_id, to_user_id, from_user_id),
            )
            existing = cur.fetchone()
            if existing and existing["status"] == "pending":
                raise ValueError("Friend request already pending")
            cur.execute(
                """
                INSERT INTO friend_requests (id, from_user_id, to_user_id, status, created_at, updated_at)
                VALUES (%s, %s, %s, 'pending', %s, %s)
                ON CONFLICT (from_user_id, to_user_id) DO UPDATE
                SET status = 'pending', updated_at = EXCLUDED.updated_at
                RETURNING id, from_user_id, to_user_id, status, created_at
                """,
                (request_id, from_user_id, to_user_id, now, now),
            )
            row = cur.fetchone()
            cur.execute("SELECT nickname FROM users WHERE id = %s", (from_user_id,))
            from_nickname = cur.fetchone()["nickname"]
        self._conn.commit()
        return FriendRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=from_nickname,
            to_user_id=row["to_user_id"],
            status=row["status"],
            created_at=row["created_at"],
        )

    def list_incoming_friend_requests(self, user_id: str) -> list[FriendRequest]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT fr.id, fr.from_user_id, u.nickname AS from_user_nickname,
                       fr.to_user_id, fr.status, fr.created_at
                FROM friend_requests fr
                JOIN users u ON u.id = fr.from_user_id
                WHERE fr.to_user_id = %s AND fr.status = 'pending'
                ORDER BY fr.created_at DESC
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [FriendRequest(**row) for row in rows]

    def _get_friend_request(self, request_id: str, user_id: str) -> dict:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT fr.id, fr.from_user_id, u.nickname AS from_user_nickname,
                       fr.to_user_id, fr.status, fr.created_at
                FROM friend_requests fr
                JOIN users u ON u.id = fr.from_user_id
                WHERE fr.id = %s
                """,
                (request_id,),
            )
            row = cur.fetchone()
        if row is None:
            raise KeyError(request_id)
        if row["to_user_id"] != user_id:
            raise PermissionError(request_id)
        if row["status"] != "pending":
            raise ValueError("Request is not pending")
        return row

    def accept_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        row = self._get_friend_request(request_id, user_id)
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE friend_requests SET status = 'accepted', updated_at = %s WHERE id = %s",
                (now, request_id),
            )
            cur.execute(
                "INSERT INTO friendships (user_id, friend_id) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                (row["from_user_id"], row["to_user_id"]),
            )
            cur.execute(
                "INSERT INTO friendships (user_id, friend_id) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                (row["to_user_id"], row["from_user_id"]),
            )
        self._conn.commit()
        return FriendRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=row["from_user_nickname"],
            to_user_id=row["to_user_id"],
            status="accepted",
            created_at=row["created_at"],
        )

    def reject_friend_request(self, request_id: str, user_id: str) -> FriendRequest:
        row = self._get_friend_request(request_id, user_id)
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE friend_requests SET status = 'rejected', updated_at = %s WHERE id = %s",
                (now, request_id),
            )
        self._conn.commit()
        return FriendRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=row["from_user_nickname"],
            to_user_id=row["to_user_id"],
            status="rejected",
            created_at=row["created_at"],
        )

    def remove_friendship(self, user_id: str, friend_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM friendships WHERE user_id = %s AND friend_id = %s",
                (user_id, friend_id),
            )
            cur.execute(
                "DELETE FROM friendships WHERE user_id = %s AND friend_id = %s",
                (friend_id, user_id),
            )
        self._conn.commit()

    def follow(self, follower_id: str, followee_id: str) -> None:
        if follower_id == followee_id:
            raise ValueError("Cannot follow yourself")
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO follows (follower_id, followee_id, created_at)
                VALUES (%s, %s, %s)
                ON CONFLICT DO NOTHING
                """,
                (follower_id, followee_id, datetime.now(UTC)),
            )
        self._conn.commit()

    def unfollow(self, follower_id: str, followee_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM follows WHERE follower_id = %s AND followee_id = %s",
                (follower_id, followee_id),
            )
        self._conn.commit()

    def list_following(self, user_id: str) -> list[FollowUserSummary]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT u.id, u.nickname, u.bio, u.photo_url
                FROM follows f
                JOIN users u ON u.id = f.followee_id
                WHERE f.follower_id = %s
                ORDER BY u.nickname
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [FollowUserSummary(**row) for row in rows]

    def list_followers(self, user_id: str) -> list[FollowUserSummary]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT u.id, u.nickname, u.bio, u.photo_url
                FROM follows f
                JOIN users u ON u.id = f.follower_id
                WHERE f.followee_id = %s
                ORDER BY u.nickname
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [FollowUserSummary(**row) for row in rows]

    def list_following_ids(self, user_id: str) -> list[str]:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT followee_id FROM follows WHERE follower_id = %s",
                (user_id,),
            )
            rows = cur.fetchall()
        return [row["followee_id"] for row in rows]

    def list_follower_ids(self, user_id: str) -> list[str]:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT follower_id FROM follows WHERE followee_id = %s",
                (user_id,),
            )
            rows = cur.fetchall()
        return [row["follower_id"] for row in rows]

    def create_follow_request(self, from_user_id: str, to_user_id: str) -> FollowRequest:
        if from_user_id == to_user_id:
            raise ValueError("Cannot send follow request to yourself")
        request_id = str(uuid.uuid4())
        now = datetime.now(UTC)
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT 1 FROM follows WHERE follower_id = %s AND followee_id = %s",
                (from_user_id, to_user_id),
            )
            if cur.fetchone():
                raise ValueError("Already following")
            cur.execute(
                """
                SELECT id, status FROM follow_requests
                WHERE from_user_id = %s AND to_user_id = %s
                """,
                (from_user_id, to_user_id),
            )
            existing = cur.fetchone()
            if existing and existing["status"] == "pending":
                raise ValueError("Follow request already pending")
            cur.execute(
                """
                INSERT INTO follow_requests (id, from_user_id, to_user_id, status, created_at)
                VALUES (%s, %s, %s, 'pending', %s)
                ON CONFLICT (from_user_id, to_user_id) DO UPDATE
                SET status = 'pending', created_at = EXCLUDED.created_at
                RETURNING id, from_user_id, to_user_id, status, created_at
                """,
                (request_id, from_user_id, to_user_id, now),
            )
            row = cur.fetchone()
            cur.execute("SELECT nickname FROM users WHERE id = %s", (from_user_id,))
            from_nickname = cur.fetchone()["nickname"]
        self._conn.commit()
        return FollowRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=from_nickname,
            to_user_id=row["to_user_id"],
            status=row["status"],
            created_at=row["created_at"],
        )

    def list_incoming_follow_requests(self, user_id: str) -> list[FollowRequest]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT fr.id, fr.from_user_id, u.nickname AS from_user_nickname,
                       fr.to_user_id, fr.status, fr.created_at
                FROM follow_requests fr
                JOIN users u ON u.id = fr.from_user_id
                WHERE fr.to_user_id = %s AND fr.status = 'pending'
                ORDER BY fr.created_at DESC
                """,
                (user_id,),
            )
            rows = cur.fetchall()
        return [FollowRequest(**row) for row in rows]

    def _get_follow_request(self, request_id: str, user_id: str) -> dict:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT fr.id, fr.from_user_id, u.nickname AS from_user_nickname,
                       fr.to_user_id, fr.status, fr.created_at
                FROM follow_requests fr
                JOIN users u ON u.id = fr.from_user_id
                WHERE fr.id = %s
                """,
                (request_id,),
            )
            row = cur.fetchone()
        if row is None:
            raise KeyError(request_id)
        if row["to_user_id"] != user_id:
            raise PermissionError(request_id)
        if row["status"] != "pending":
            raise ValueError("Request is not pending")
        return row

    def accept_follow_request(self, request_id: str, user_id: str) -> FollowRequest:
        row = self._get_follow_request(request_id, user_id)
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE follow_requests SET status = 'accepted' WHERE id = %s",
                (request_id,),
            )
            cur.execute(
                """
                INSERT INTO follows (follower_id, followee_id, created_at)
                VALUES (%s, %s, %s)
                ON CONFLICT DO NOTHING
                """,
                (row["from_user_id"], row["to_user_id"], datetime.now(UTC)),
            )
        self._conn.commit()
        return FollowRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=row["from_user_nickname"],
            to_user_id=row["to_user_id"],
            status="accepted",
            created_at=row["created_at"],
        )

    def reject_follow_request(self, request_id: str, user_id: str) -> FollowRequest:
        row = self._get_follow_request(request_id, user_id)
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE follow_requests SET status = 'rejected' WHERE id = %s",
                (request_id,),
            )
        self._conn.commit()
        return FollowRequest(
            id=row["id"],
            from_user_id=row["from_user_id"],
            from_user_nickname=row["from_user_nickname"],
            to_user_id=row["to_user_id"],
            status="rejected",
            created_at=row["created_at"],
        )

    def has_pending_follow_request(self, from_user_id: str, to_user_id: str) -> bool:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT 1 FROM follow_requests
                WHERE from_user_id = %s AND to_user_id = %s AND status = 'pending'
                """,
                (from_user_id, to_user_id),
            )
            return cur.fetchone() is not None

    def is_approved_follower(self, follower_id: str, followee_id: str) -> bool:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT 1 FROM follows WHERE follower_id = %s AND followee_id = %s",
                (follower_id, followee_id),
            )
            return cur.fetchone() is not None
