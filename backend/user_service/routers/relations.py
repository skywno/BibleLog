from fastapi import APIRouter, Depends, HTTPException, Query, status

from common.deps import get_current_user_id
from common.models import FollowUserSummary, FriendRequest, SendFriendRequestBody, UserSearchResult
from user_service.deps import CurrentUserIdDep, UserContainerDep

router = APIRouter(tags=["relations"], dependencies=[Depends(get_current_user_id)])


@router.get("/users/search")
def search_users(
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
    q: str = Query(min_length=1),
    limit: int = Query(default=20, ge=1, le=50),
) -> list[UserSearchResult]:
    return container.relation_service.search_users(q, user_id, limit)


@router.get("/friends")
def list_friends(user_id: CurrentUserIdDep, container: UserContainerDep) -> list[UserSearchResult]:
    return container.relation_service.list_friends(user_id)


@router.post("/friends/requests", status_code=status.HTTP_201_CREATED)
async def send_friend_request(
    body: SendFriendRequestBody,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> FriendRequest:
    try:
        return await container.relation_service.send_friend_request(user_id, body)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/friends/requests/incoming")
def list_incoming_friend_requests(
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> list[FriendRequest]:
    return container.relation_service.list_incoming_friend_requests(user_id)


@router.post("/friends/requests/{request_id}/accept")
async def accept_friend_request(
    request_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> FriendRequest:
    try:
        return await container.relation_service.accept_friend_request(request_id, user_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Request not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not allowed") from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.post("/friends/requests/{request_id}/reject")
def reject_friend_request(
    request_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> FriendRequest:
    try:
        return container.relation_service.reject_friend_request(request_id, user_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Request not found") from exc
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not allowed") from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/friends/{friend_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_friend(
    friend_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    container.relation_service.remove_friendship(user_id, friend_id)


@router.post("/follows/{followee_id}", status_code=status.HTTP_204_NO_CONTENT)
async def follow_user(
    followee_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    try:
        await container.relation_service.follow(user_id, followee_id)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/follows/{followee_id}", status_code=status.HTTP_204_NO_CONTENT)
def unfollow_user(
    followee_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    container.relation_service.unfollow(user_id, followee_id)


@router.get("/follows")
def list_following(user_id: CurrentUserIdDep, container: UserContainerDep) -> list[FollowUserSummary]:
    return container.relation_service.list_following(user_id)


@router.get("/followers")
def list_followers(user_id: CurrentUserIdDep, container: UserContainerDep) -> list[FollowUserSummary]:
    return container.relation_service.list_followers(user_id)
