from __future__ import annotations

import secrets
import uuid
from datetime import UTC, datetime, timedelta

from jose import jwt

from shared.auth.jwt import create_access_token
from shared.config import Settings
from user_service.repositories.user import UserRepository


def create_refresh_token(user_id: str, settings: Settings, users: UserRepository) -> str:
    token = secrets.token_urlsafe(48)
    users.save_refresh_token(token, user_id)
    return token


def issue_tokens(user_id: str, nickname: str, settings: Settings, users: UserRepository):
    users.ensure_user(user_id, nickname)
    access_token, expires_in = create_access_token(user_id, settings)
    refresh_token = create_refresh_token(user_id, settings, users)
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": expires_in,
        "user": users.get_user(user_id),
    }


def dev_login(email: str, settings: Settings, users: UserRepository):
    user_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, email))
    nickname = email.split("@")[0]
    return issue_tokens(user_id, nickname, settings, users)
