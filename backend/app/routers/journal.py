from fastapi import APIRouter, Depends, HTTPException, status

from app.deps import CurrentUserIdDep, get_current_user_id
from app.models import MeditationNote, UpsertJournalNoteRequest
from app.store import store

router = APIRouter(
    prefix="/journal",
    tags=["journal"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/notes")
def list_journal_notes(user_id: CurrentUserIdDep) -> list[MeditationNote]:
    return store.list_notes(user_id)


@router.post("/notes", status_code=status.HTTP_201_CREATED)
def create_journal_note(
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
) -> MeditationNote:
    return store.upsert_note(user_id, body)


@router.patch("/notes/{note_id}")
def update_journal_note(
    note_id: str,
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
) -> MeditationNote:
    try:
        return store.upsert_note(user_id, body, note_id=note_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Note not found") from exc


@router.delete("/notes/{note_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_journal_note(
    note_id: str,
    user_id: CurrentUserIdDep,
) -> None:
    store.delete_note(user_id, note_id)
