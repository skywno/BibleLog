from fastapi import APIRouter, Depends, HTTPException, status

from app.deps import CurrentUserIdDep, get_current_user_id
from app.models import (
    CreateReadingRecordRequest,
    ReadingProgress,
    ReadingRecord,
    ReadingStats,
)
from app.store import store

router = APIRouter(
    prefix="/reading",
    tags=["reading"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/records")
def list_reading_records(user_id: CurrentUserIdDep) -> list[ReadingRecord]:
    return store.list_reading_records(user_id)


@router.post("/records", status_code=status.HTTP_201_CREATED)
def create_reading_record(
    body: CreateReadingRecordRequest,
    user_id: CurrentUserIdDep,
) -> ReadingRecord:
    try:
        return store.add_reading_record(user_id, body)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc)) from exc


@router.get("/progress")
def get_reading_progress(user_id: CurrentUserIdDep) -> ReadingProgress:
    return store.reading_progress(user_id)


@router.get("/stats")
def get_reading_stats(user_id: CurrentUserIdDep) -> ReadingStats:
    return store.reading_stats(user_id)
