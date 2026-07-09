from fastapi import APIRouter, Depends, HTTPException

from note_service.deps import NoteContainerDep
from shared.internal import verify_internal_token
from shared.models import (
    MeditationNote,
    NotesBatchRequest,
    NotesBatchResponse,
    RecentNotesByAuthorsRequest,
    RecentNotesByAuthorsResponse,
    RecentNoteEntry,
)
from shared.domain import FeedTimelineEntry

router = APIRouter(prefix="/internal", tags=["internal"], include_in_schema=False)


@router.post("/notes/batch", dependencies=[Depends(verify_internal_token)])
def batch_notes_internal(payload: NotesBatchRequest, container: NoteContainerDep) -> NotesBatchResponse:
    notes = container.note_service.batch_summaries(payload.viewer_id, payload.note_ids)
    return NotesBatchResponse(notes=notes)


@router.get("/notes/{note_id}", dependencies=[Depends(verify_internal_token)])
def get_note_internal(
    note_id: str,
    viewer_id: str,
    container: NoteContainerDep,
) -> MeditationNote:
    try:
        return container.note_service.get_detail(viewer_id, note_id)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail="Note not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=403, detail="Forbidden") from exc


@router.get("/notes/timeline/{note_id}", dependencies=[Depends(verify_internal_token)])
def timeline_entry_internal(note_id: str, container: NoteContainerDep) -> FeedTimelineEntry:
    entry = container.note_service.get_timeline_entry(note_id)
    if entry is None:
        raise HTTPException(status_code=404, detail="Note not found")
    return entry


@router.post("/notes/recent-for-feed", dependencies=[Depends(verify_internal_token)])
def recent_for_feed_internal(
    payload: RecentNotesByAuthorsRequest,
    container: NoteContainerDep,
) -> RecentNotesByAuthorsResponse:
    entries = container.note_service.recent_entries_for_feed(
        payload.viewer_id,
        payload.author_ids,
        since=payload.since,
        limit_per_author=payload.limit_per_author,
    )
    return RecentNotesByAuthorsResponse(
        entries=[
            RecentNoteEntry(
                note_id=entry.note_id,
                author_id=entry.author_id,
                created_at=entry.created_at,
                visibility=entry.visibility,
                church_id=entry.church_id,
                group_ids=entry.group_ids or set(),
            )
            for entry in entries
        ]
    )
