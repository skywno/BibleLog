from typing import Annotated

from fastapi import Depends

from common.deps import CurrentUserIdDep, get_current_user_id
from social_service.container import SocialContainer, get_social_container

SocialContainerDep = Annotated[SocialContainer, Depends(get_social_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "SocialContainerDep"]
