from typing import Annotated

from fastapi import Depends

from note_service.container import NoteContainer, get_note_container
from shared.deps import CurrentUserIdDep, get_current_user_id

NoteContainerDep = Annotated[NoteContainer, Depends(get_note_container)]

__all__ = ["CurrentUserIdDep", "get_current_user_id", "NoteContainerDep"]
