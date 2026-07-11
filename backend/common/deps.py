from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from common.auth.jwt import decode_access_token
from common.settings.base import BaseServiceSettings


def get_auth_settings() -> BaseServiceSettings:
    return BaseServiceSettings()


security = HTTPBearer(auto_error=False)

SettingsDep = Annotated[BaseServiceSettings, Depends(get_auth_settings)]
BearerCredentialsDep = Annotated[HTTPAuthorizationCredentials | None, Depends(security)]


def get_current_user_id(
    credentials: BearerCredentialsDep,
    settings: SettingsDep,
) -> str:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication required")
    return decode_access_token(credentials.credentials, settings)


CurrentUserIdDep = Annotated[str, Depends(get_current_user_id)]
