from fastapi import APIRouter, Depends, HTTPException, status

from reading_service.deps import CurrentUserIdDep, ReadingContainerDep, get_current_user_id
from common.models import (
    CreateReadingRecordRequest,
    ReadingProgress,
    ReadingRecord,
    ReadingStats,
)

router = APIRouter(
    prefix="/reading",
    tags=["reading"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/records")
def list_reading_records(user_id: CurrentUserIdDep, container: ReadingContainerDep) -> list[ReadingRecord]:
    return container.reading_service.list_records(user_id)


@router.post("/records", status_code=status.HTTP_201_CREATED)
def create_reading_record(
    body: CreateReadingRecordRequest,
    user_id: CurrentUserIdDep,
    container: ReadingContainerDep,
) -> ReadingRecord:
    try:
        return container.reading_service.add_record(user_id, body)
    except ValueError as exc:
        detail = str(exc)
        status_code = (
            status.HTTP_400_BAD_REQUEST
            if "유효하지 않은" in detail
            else status.HTTP_409_CONFLICT
        )
        raise HTTPException(status_code=status_code, detail=detail) from exc


@router.get("/progress")
def get_reading_progress(user_id: CurrentUserIdDep, container: ReadingContainerDep) -> ReadingProgress:
    return container.reading_service.progress(user_id)


@router.get("/stats")
def get_reading_stats(user_id: CurrentUserIdDep, container: ReadingContainerDep) -> ReadingStats:
    return container.reading_service.stats(user_id)
