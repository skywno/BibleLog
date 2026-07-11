from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from common.internal import verify_internal_token
from common.models import UserProfile
from user_service.deps import UserContainerDep

router = APIRouter(prefix="/internal", tags=["internal"], include_in_schema=False)


@router.get("/users/{user_id}", dependencies=[Depends(verify_internal_token)])
def get_user_internal(user_id: str, container: UserContainerDep) -> UserProfile:
    try:
        return container.users.get_user(user_id)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail="User not found") from exc


@router.get("/relations/friends/{user_id}", dependencies=[Depends(verify_internal_token)])
def list_friends_internal(user_id: str, container: UserContainerDep) -> list[str]:
    return container.relations.list_friend_ids(user_id)


@router.get("/relations/memberships/{user_id}", dependencies=[Depends(verify_internal_token)])
def get_membership_internal(user_id: str, container: UserContainerDep) -> dict:
    membership = container.relations.get_membership(user_id)
    return {"church_id": membership.church_id, "group_ids": sorted(membership.group_ids)}


class GroupMembersRequest(BaseModel):
    group_ids: list[str]


@router.post("/relations/group-members", dependencies=[Depends(verify_internal_token)])
def list_group_members_internal(
    payload: GroupMembersRequest,
    container: UserContainerDep,
) -> list[str]:
    return container.relations.list_group_member_ids(set(payload.group_ids))


@router.get("/relations/church-members/{church_id}", dependencies=[Depends(verify_internal_token)])
def list_church_members_internal(church_id: str, container: UserContainerDep) -> list[str]:
    return container.relations.list_church_member_ids(church_id)
