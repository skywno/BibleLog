from __future__ import annotations

import uuid
from datetime import UTC, datetime

import psycopg

from common.models import UserProfile
from user_service.repositories.user import UserRepository


class PostgresUserRepository(UserRepository):
    def __init__(self, conn: psycopg.Connection) -> None:
        self._conn = conn

    def ensure_user(self, user_id: str, nickname: str, bio: str = "") -> UserProfile:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO users (id, nickname, bio)
                VALUES (%s, %s, %s)
                ON CONFLICT (id) DO NOTHING
                """,
                (user_id, nickname, bio),
            )
            cur.execute(
                """
                INSERT INTO user_memberships (user_id, church_id, group_ids)
                VALUES (%s, NULL, '{}')
                ON CONFLICT (user_id) DO NOTHING
                """,
                (user_id,),
            )
        self._conn.commit()
        return self.get_user(user_id)

    def get_user(self, user_id: str) -> UserProfile:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT id, nickname, bio, photo_url, profile_visibility FROM users WHERE id = %s",
                (user_id,),
            )
            row = cur.fetchone()
        if row is None:
            raise KeyError(user_id)
        return UserProfile(**row, is_logged_in=True)

    def update_user(
        self,
        user_id: str,
        nickname: str | None,
        bio: str | None,
        photo_url: str | None = None,
        profile_visibility: str | None = None,
    ) -> UserProfile:
        with self._conn.cursor() as cur:
            if nickname is not None:
                cur.execute("UPDATE users SET nickname = %s WHERE id = %s", (nickname, user_id))
            if bio is not None:
                cur.execute("UPDATE users SET bio = %s WHERE id = %s", (bio, user_id))
            if photo_url is not None:
                cur.execute("UPDATE users SET photo_url = %s WHERE id = %s", (photo_url, user_id))
            if profile_visibility is not None:
                cur.execute(
                    "UPDATE users SET profile_visibility = %s WHERE id = %s",
                    (profile_visibility, user_id),
                )
        self._conn.commit()
        return self.get_user(user_id)

    def save_refresh_token(self, token: str, user_id: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "INSERT INTO refresh_tokens (token, user_id) VALUES (%s, %s)",
                (token, user_id),
            )
        self._conn.commit()

    def pop_refresh_token(self, token: str) -> str | None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM refresh_tokens WHERE token = %s RETURNING user_id",
                (token,),
            )
            row = cur.fetchone()
        self._conn.commit()
        return row["user_id"] if row else None

    def save_oauth_state(self, state: str, redirect_uri: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "INSERT INTO oauth_states (state, redirect_uri) VALUES (%s, %s)",
                (state, redirect_uri),
            )
        self._conn.commit()

    def pop_oauth_state(self, state: str) -> str | None:
        with self._conn.cursor() as cur:
            cur.execute(
                "DELETE FROM oauth_states WHERE state = %s RETURNING redirect_uri",
                (state,),
            )
            row = cur.fetchone()
        self._conn.commit()
        return row["redirect_uri"] if row else None
