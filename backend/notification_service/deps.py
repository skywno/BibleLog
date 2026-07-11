from typing import Annotated

from fastapi import Depends

from common.deps import CurrentUserIdDep, get_current_user_id
from notification_service.container import NotificationContainer, get_notification_container

NotificationContainerDep = Annotated[NotificationContainer, Depends(get_notification_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "NotificationContainerDep"]
