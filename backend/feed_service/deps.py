from typing import Annotated

from fastapi import Depends

from feed_service.container import FeedContainer, get_feed_container
from common.deps import CurrentUserIdDep, get_current_user_id

FeedContainerDep = Annotated[FeedContainer, Depends(get_feed_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "FeedContainerDep"]
