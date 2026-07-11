from __future__ import annotations

from typing import Annotated

from fastapi import Depends, Header, HTTPException, status

from common.config import Settings, get_settings


def verify_internal_token(
    settings: Annotated[Settings, Depends(get_settings)],
    x_internal_token: Annotated[str | None, Header()] = None,
) -> None:
    if x_internal_token != settings.internal_service_token:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid internal token")
