from typing import Annotated

from fastapi import Depends

from common.deps import CurrentUserIdDep, get_current_user_id
from user_service.container import UserContainer, get_user_container

UserContainerDep = Annotated[UserContainer, Depends(get_user_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "UserContainerDep"]
