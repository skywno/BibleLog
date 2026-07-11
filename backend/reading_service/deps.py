from typing import Annotated

from fastapi import Depends

from reading_service.container import ReadingContainer, get_reading_container
from common.deps import CurrentUserIdDep, get_current_user_id

ReadingContainerDep = Annotated[ReadingContainer, Depends(get_reading_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "ReadingContainerDep"]
