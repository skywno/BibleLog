from __future__ import annotations

import secrets
import uuid
from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from jose import JWTError, jwt

from app.config import Settings
from app.store import store


def create_access_token(user_id: str, settings: Settings) -> tuple[str, int]:
    expires_in = settings.access_token_expire_minutes * 60
    payload = {
        "sub": user_id,
        "exp": datetime.now(UTC) + timedelta(minutes=settings.access_token_expire_minutes),
        "type": "access",
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm), expires_in


def create_refresh_token(user_id: str, settings: Settings) -> str:
    token = secrets.token_urlsafe(48)
    store.save_refresh_token(token, user_id)
    return token


def decode_access_token(token: str, settings: Settings) -> str:
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token") from exc
    if payload.get("type") != "access":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")
    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token subject")
    return user_id


def issue_tokens(user_id: str, nickname: str, settings: Settings):
    store.ensure_user(user_id, nickname)
    access_token, expires_in = create_access_token(user_id, settings)
    refresh_token = create_refresh_token(user_id, settings)
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": expires_in,
        "user": store.get_user(user_id),
    }


def dev_login(email: str, settings: Settings):
    user_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, email))
    nickname = email.split("@")[0]
    return issue_tokens(user_id, nickname, settings)
