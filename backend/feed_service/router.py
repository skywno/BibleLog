from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status

from feed_service.deps import CurrentUserIdDep, FeedContainerDep, get_current_user_id
from shared.models import FeedFilter, FeedItem, FeedPageResponse, FeedSort, ToggleReactionRequest

router = APIRouter(
    prefix="/feed",
    tags=["feed"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("")
def list_feed(
    user_id: CurrentUserIdDep,
    container: FeedContainerDep,
    filter: Annotated[FeedFilter, Query()] = "all",
    sort: Annotated[FeedSort, Query()] = "latest",
    limit: Annotated[int | None, Query(ge=1, le=50)] = None,
    cursor: Annotated[str | None, Query()] = None,
) -> FeedPageResponse:
    return container.feed_service.get_feed(
        viewer_id=user_id,
        feed_filter=filter,
        sort=sort,
        limit=limit,
        cursor=cursor,
    )


@router.post("/{note_id}/reactions")
def toggle_reaction(
    note_id: str,
    body: ToggleReactionRequest,
    user_id: CurrentUserIdDep,
    container: FeedContainerDep,
) -> FeedItem:
    try:
        return container.feed_service.toggle_reaction(user_id, note_id, body.reaction)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Note not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden") from exc
