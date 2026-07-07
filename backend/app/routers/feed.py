from typing import Annotated

from fastapi import APIRouter, Depends, Query

from app.deps import CurrentUserIdDep, get_current_user_id
from app.models import FeedFilter, FeedItem, FeedSort, ToggleReactionRequest
from app.store import store

router = APIRouter(
    prefix="/feed",
    tags=["feed"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("")
def list_feed(
    user_id: CurrentUserIdDep,
    filter: Annotated[FeedFilter, Query()] = "all",
    sort: Annotated[FeedSort, Query()] = "latest",
) -> list[FeedItem]:
    items = store.list_feed()
    if filter != "all":
        items = [item for item in items if item.note.visibility == filter]
    if sort == "popular":
        items = sorted(items, key=lambda item: item.comment_count, reverse=True)
    return items


@router.post("/{note_id}/reactions")
def toggle_reaction(
    note_id: str,
    body: ToggleReactionRequest,
    user_id: CurrentUserIdDep,
) -> FeedItem:
    for item in store.list_feed():
        if item.note.id == note_id:
            return item
    return FeedItem(
        note=store.list_notes(user_id)[0],
        reactions=[],
        comment_count=0,
    )
