from fastapi import APIRouter, Depends, HTTPException, Query, status

from common.deps import get_current_user_id
from common.models import Comment, CommentPageResponse, CreateCommentRequest, UpdateCommentRequest
from social_service.deps import CurrentUserIdDep, SocialContainerDep

router = APIRouter(prefix="/social", tags=["social"], dependencies=[Depends(get_current_user_id)])


@router.get("/notes/{note_id}/comments")
async def list_comments(
    note_id: str,
    container: SocialContainerDep,
    cursor: str | None = None,
    limit: int = Query(default=20, ge=1, le=50),
) -> CommentPageResponse:
    return await container.social_service.list_comments(note_id, cursor=cursor, limit=limit)


@router.post("/notes/{note_id}/comments", status_code=status.HTTP_201_CREATED)
async def create_comment(
    note_id: str,
    body: CreateCommentRequest,
    user_id: CurrentUserIdDep,
    container: SocialContainerDep,
) -> Comment:
    return await container.social_service.create_comment(note_id, user_id, body)


@router.patch("/comments/{comment_id}")
async def update_comment(
    comment_id: str,
    body: UpdateCommentRequest,
    user_id: CurrentUserIdDep,
    container: SocialContainerDep,
) -> Comment:
    try:
        return await container.social_service.update_comment(comment_id, user_id, body)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Comment not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not allowed") from exc


@router.delete("/comments/{comment_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_comment(
    comment_id: str,
    user_id: CurrentUserIdDep,
    container: SocialContainerDep,
) -> None:
    try:
        container.social_service.delete_comment(comment_id, user_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Comment not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not allowed") from exc
