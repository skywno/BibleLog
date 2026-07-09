from fastapi import APIRouter, Depends, HTTPException, status

from note_service.deps import CurrentUserIdDep, NoteContainerDep, get_current_user_id
from shared.models import MeditationNote, UpsertJournalNoteRequest

router = APIRouter(
    prefix="/journal",
    tags=["journal"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/notes")
def list_journal_notes(user_id: CurrentUserIdDep, container: NoteContainerDep) -> list[MeditationNote]:
    return container.note_service.list_mine(user_id)


@router.get("/notes/{note_id}")
def get_journal_note(
    note_id: str,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    try:
        return container.note_service.get_detail(user_id, note_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Note not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden") from exc


@router.post("/notes", status_code=status.HTTP_201_CREATED)
def create_journal_note(
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    return container.note_service.create(user_id, body)


@router.patch("/notes/{note_id}")
def update_journal_note(
    note_id: str,
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    try:
        return container.note_service.update(user_id, note_id, body)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Note not found") from exc


@router.delete("/notes/{note_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_journal_note(
    note_id: str,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> None:
    try:
        container.note_service.delete(user_id, note_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Note not found") from exc
