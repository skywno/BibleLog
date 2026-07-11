from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect, status

from common.auth.jwt import decode_access_token
from common.deps import get_current_user_id
from common.models import NotificationPageResponse
from notification_service.container import get_notification_container
from notification_service.deps import CurrentUserIdDep
from notification_service.settings import get_notification_settings

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.get("", dependencies=[Depends(get_current_user_id)])
def list_notifications(
    user_id: CurrentUserIdDep,
    cursor: str | None = None,
    limit: int = Query(default=20, ge=1, le=50),
) -> NotificationPageResponse:
    container = get_notification_container()
    return container.notification_service.list_notifications(user_id, cursor=cursor, limit=limit)


@router.websocket("/ws")
async def notifications_websocket(
    websocket: WebSocket,
    token: str = Query(...),
) -> None:
    settings = get_notification_settings()
    container = get_notification_container()
    try:
        user_id = decode_access_token(token, settings)
    except Exception:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await container.hub.connect(user_id, websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        await container.hub.disconnect(user_id, websocket)
