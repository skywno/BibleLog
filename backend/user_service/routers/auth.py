from __future__ import annotations

import secrets
from typing import Annotated
from urllib.parse import urlencode

from authlib.integrations.httpx_client import AsyncOAuth2Client
from fastapi import APIRouter, Depends, HTTPException, Path, Query, status
from fastapi.responses import RedirectResponse

from shared.auth.jwt import create_access_token
from user_service.auth_tokens import (
    create_refresh_token,
    dev_login,
    issue_tokens,
)
from shared.config import Settings
from shared.deps import SettingsDep, get_current_user_id
from user_service.deps import UserContainerDep
from shared.models import AuthTokenResponse, OAuthAuthorizeResponse, RefreshTokenRequest

router = APIRouter(prefix="/auth", tags=["auth"])


def _oauth_client(provider: str, settings: Settings) -> AsyncOAuth2Client:
    if provider == "google":
        return AsyncOAuth2Client(
            client_id=settings.google_client_id,
            client_secret=settings.google_client_secret,
            scope="openid email profile",
        )
    if provider == "facebook":
        return AsyncOAuth2Client(
            client_id=settings.facebook_client_id,
            client_secret=settings.facebook_client_secret,
            scope="email public_profile",
        )
    raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported provider")


def _authorize_endpoint(provider: str) -> str:
    if provider == "google":
        return "https://accounts.google.com/o/oauth2/v2/auth"
    if provider == "facebook":
        return "https://www.facebook.com/v20.0/dialog/oauth"
    raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported provider")


def _token_endpoint(provider: str) -> str:
    if provider == "google":
        return "https://oauth2.googleapis.com/token"
    if provider == "facebook":
        return "https://graph.facebook.com/v20.0/oauth/access_token"
    raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported provider")


@router.get("/{provider}/authorize")
def get_oauth_authorize_url(
    provider: Annotated[str, Path()],
    redirect_uri: Annotated[str, Query()],
    settings: SettingsDep,
    container: UserContainerDep,
) -> OAuthAuthorizeResponse:
    if provider not in {"google", "facebook"}:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported provider")

    if provider == "google" and not settings.google_client_id:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Google OAuth is not configured on the server",
        )
    if provider == "facebook" and not settings.facebook_client_id:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Facebook OAuth is not configured on the server",
        )

    state = secrets.token_urlsafe(24)
    container.users.save_oauth_state(state, redirect_uri)

    callback_uri = f"{settings.oauth_redirect_base}/{provider}/callback"
    params = {
        "response_type": "code",
        "client_id": settings.google_client_id if provider == "google" else settings.facebook_client_id,
        "redirect_uri": callback_uri,
        "scope": "openid email profile" if provider == "google" else "email public_profile",
        "state": state,
    }
    if provider == "google":
        params["access_type"] = "offline"
        params["prompt"] = "consent"

    authorization_url = f"{_authorize_endpoint(provider)}?{urlencode(params)}"
    return OAuthAuthorizeResponse(authorization_url=authorization_url, state=state)


@router.get("/{provider}/callback", response_model=None)
async def handle_oauth_callback(
    provider: Annotated[str, Path()],
    code: Annotated[str, Query()],
    settings: SettingsDep,
    container: UserContainerDep,
    state: Annotated[str | None, Query()] = None,
) -> RedirectResponse | AuthTokenResponse:
    if provider not in {"google", "facebook"}:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Unsupported provider")

    client = _oauth_client(provider, settings)
    callback_uri = f"{settings.oauth_redirect_base}/{provider}/callback"
    token = await client.fetch_token(
        _token_endpoint(provider),
        code=code,
        redirect_uri=callback_uri,
    )

    if provider == "google":
        userinfo = await client.get("https://openidconnect.googleapis.com/v1/userinfo", token=token)
        profile = userinfo.json()
        email = profile.get("email", f"google-{secrets.token_hex(4)}@example.com")
        nickname = profile.get("name", email.split("@")[0])
    else:
        userinfo = await client.get(
            "https://graph.facebook.com/me",
            token=token,
            params={"fields": "id,name,email"},
        )
        profile = userinfo.json()
        email = profile.get("email", f"facebook-{profile['id']}@example.com")
        nickname = profile.get("name", email.split("@")[0])

    user_key = str(profile.get("sub", profile.get("id", email)))
    payload = issue_tokens(user_key, nickname, settings, container.users)

    client_redirect = container.users.pop_oauth_state(state) if state else None
    if client_redirect:
        fragment = urlencode(
            {
                "access_token": payload["access_token"],
                "refresh_token": payload["refresh_token"],
                "expires_in": payload["expires_in"],
            }
        )
        return RedirectResponse(url=f"{client_redirect}#{fragment}")

    return AuthTokenResponse(**payload)


@router.post("/token/refresh")
def refresh_auth_token(
    body: RefreshTokenRequest,
    settings: SettingsDep,
    container: UserContainerDep,
) -> AuthTokenResponse:
    user_id = container.users.pop_refresh_token(body.refresh_token)
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")
    user = container.users.get_user(user_id)
    access_token, expires_in = create_access_token(user_id, settings)
    refresh_token = create_refresh_token(user_id, settings, container.users)
    return AuthTokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=expires_in,
        user=user,
    )


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT, dependencies=[Depends(get_current_user_id)])
def logout() -> None:
    return None


@router.post("/dev/login")
def dev_login_endpoint(
    settings: SettingsDep,
    container: UserContainerDep,
    email: Annotated[str, Query()] = "demo@biblelog.app",
) -> AuthTokenResponse:
    if not settings.debug:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
    return AuthTokenResponse(**dev_login(email, settings, container.users))
