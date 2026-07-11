from typing import Annotated

from fastapi import Depends

from ai_service.container import AiContainer, get_ai_container
from common.deps import CurrentUserIdDep, SettingsDep, get_current_user_id

AiContainerDep = Annotated[AiContainer, Depends(get_ai_container)]

__all__ = ["AiContainerDep", "CurrentUserIdDep", "SettingsDep", "get_current_user_id"]
