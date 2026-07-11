from fastapi import APIRouter, Depends, HTTPException, Query, status

from common.deps import get_current_user_id
from common.models import (
    Church,
    CreateChurchRequest,
    CreateSmallGroupRequest,
    SmallGroup,
    UserMembershipsResponse,
)
from user_service.deps import CurrentUserIdDep, UserContainerDep

router = APIRouter(tags=["organizations"], dependencies=[Depends(get_current_user_id)])


@router.post("/churches", status_code=status.HTTP_201_CREATED)
def create_church(
    body: CreateChurchRequest,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> Church:
    return container.organization_service.create_church(user_id, body)


@router.get("/churches/{church_id}")
def get_church(church_id: str, container: UserContainerDep) -> Church:
    try:
        return container.organization_service.get_church(church_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Church not found") from exc


@router.post("/churches/{church_id}/members", status_code=status.HTTP_204_NO_CONTENT)
def join_church(
    church_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    try:
        container.organization_service.join_church(user_id, church_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Church not found") from exc


@router.delete("/churches/{church_id}/members/me", status_code=status.HTTP_204_NO_CONTENT)
def leave_church(
    church_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    try:
        container.organization_service.leave_church(user_id, church_id)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.post("/small-groups", status_code=status.HTTP_201_CREATED)
def create_small_group(
    body: CreateSmallGroupRequest,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> SmallGroup:
    return container.organization_service.create_small_group(user_id, body)


@router.get("/small-groups/{group_id}")
def get_small_group(group_id: str, container: UserContainerDep) -> SmallGroup:
    try:
        return container.organization_service.get_small_group(group_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Group not found") from exc


@router.post("/small-groups/{group_id}/members", status_code=status.HTTP_204_NO_CONTENT)
def join_small_group(
    group_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    try:
        container.organization_service.join_small_group(user_id, group_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Group not found") from exc


@router.delete("/small-groups/{group_id}/members/me", status_code=status.HTTP_204_NO_CONTENT)
def leave_small_group(
    group_id: str,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> None:
    container.organization_service.leave_small_group(user_id, group_id)


@router.get("/churches/search")
def search_churches(
    container: UserContainerDep,
    q: str = Query(min_length=1),
    limit: int = Query(default=20, ge=1, le=50),
) -> list[Church]:
    return container.organization_service.search_churches(q, limit)


@router.get("/small-groups/search")
def search_small_groups(
    container: UserContainerDep,
    q: str = Query(min_length=1),
    limit: int = Query(default=20, ge=1, le=50),
) -> list[SmallGroup]:
    return container.organization_service.search_small_groups(q, limit)


@router.get("/me/memberships")
def get_my_memberships(user_id: CurrentUserIdDep, container: UserContainerDep) -> UserMembershipsResponse:
    membership = container.organization_service.get_memberships(user_id)
    return UserMembershipsResponse(
        church_id=membership.church_id,
        group_ids=sorted(membership.group_ids),
    )
