from typing import Annotated

from fastapi import APIRouter, Depends, Query

from common.internal import verify_internal_token
from common.models import (
    CommentCountBatchRequest,
    CommentCountBatchResponse,
    NoteCommentCount,
    ReactionsBatchRequest,
    ReactionsBatchResponse,
    ToggleReactionRequest,
)
from social_service.deps import SocialContainerDep

router = APIRouter(prefix="/internal", tags=["internal"], include_in_schema=False)


@router.post("/reactions/batch", dependencies=[Depends(verify_internal_token)])
def reactions_batch_internal(
    payload: ReactionsBatchRequest,
    container: SocialContainerDep,
) -> ReactionsBatchResponse:
    items = container.social_service.reactions_batch_response(payload.note_ids, payload.viewer_id)
    return ReactionsBatchResponse(items=items)


@router.post("/comments/count/batch", dependencies=[Depends(verify_internal_token)])
def comment_counts_internal(
    payload: CommentCountBatchRequest,
    container: SocialContainerDep,
) -> CommentCountBatchResponse:
    counts = container.social_service.get_comment_counts_batch(payload.note_ids)
    return CommentCountBatchResponse(
        items=[NoteCommentCount(note_id=note_id, count=count) for note_id, count in counts.items()]
    )


@router.post("/reactions/{note_id}/toggle", dependencies=[Depends(verify_internal_token)])
async def toggle_reaction_internal(
    note_id: str,
    payload: ToggleReactionRequest,
    container: SocialContainerDep,
    user_id: Annotated[str, Query()],
) -> dict:
    active = await container.social_service.toggle_reaction(note_id, user_id, payload.reaction)
    return {"active_reaction": active}


@router.get("/engagement/{note_id}", dependencies=[Depends(verify_internal_token)])
def engagement_internal(note_id: str, container: SocialContainerDep) -> dict[str, int]:
    reaction_total, comment_count = container.social_service.total_engagement(note_id)
    return {"reaction_total": reaction_total, "comment_count": comment_count}
