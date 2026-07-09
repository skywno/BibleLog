from __future__ import annotations

import secrets
import uuid
from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from jose import JWTError, jwt

from shared.config import Settings


def create_access_token(user_id: str, settings: Settings) -> tuple[str, int]:
    expires_in = settings.access_token_expire_minutes * 60
    payload = {
        "sub": user_id,
        "exp": datetime.now(UTC) + timedelta(minutes=settings.access_token_expire_minutes),
        "type": "access",
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm), expires_in


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
