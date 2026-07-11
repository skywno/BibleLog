from fastapi import APIRouter, Depends

from common.models import MeditationNote, UpsertJournalNoteRequest
from note_service.deps import CurrentUserIdDep, NoteContainerDep, get_current_user_id

router = APIRouter(
    prefix="/journal",
    tags=["journal"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/notes")
async def list_journal_notes(user_id: CurrentUserIdDep, container: NoteContainerDep) -> list[MeditationNote]:
    return await container.note_service.list_mine(user_id)


@router.get("/users/{user_id}/notes")
async def list_user_notes(
    user_id: str,
    viewer_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> list[MeditationNote]:
    return await container.note_service.list_for_viewer(viewer_id, user_id)


@router.get("/notes/{note_id}")
async def get_journal_note(
    note_id: str,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    return await container.note_service.get_detail(user_id, note_id)


@router.post("/notes", status_code=201)
async def create_journal_note(
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    return await container.note_service.create(user_id, body)


@router.patch("/notes/{note_id}")
async def update_journal_note(
    note_id: str,
    body: UpsertJournalNoteRequest,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> MeditationNote:
    return await container.note_service.update(user_id, note_id, body)


@router.delete("/notes/{note_id}", status_code=204)
async def delete_journal_note(
    note_id: str,
    user_id: CurrentUserIdDep,
    container: NoteContainerDep,
) -> None:
    await container.note_service.delete(user_id, note_id)
