from fastapi import APIRouter, Depends, HTTPException, status

from common.models import UpdateUserProfileRequest, UserProfile
from user_service.deps import CurrentUserIdDep, UserContainerDep, get_current_user_id
from user_service.container import UserContainer

router = APIRouter(
    prefix="/users",
    tags=["users"],
    dependencies=[Depends(get_current_user_id)],
)


def can_view_profile(viewer_id: str, profile: UserProfile, container: UserContainer) -> bool:
    if viewer_id == profile.id:
        return True
    if profile.profile_visibility == "public":
        return True
    if profile.id in container.relations.list_friend_ids(viewer_id):
        return True
    return container.relations.is_approved_follower(viewer_id, profile.id)


@router.get("/me")
def get_current_user(user_id: CurrentUserIdDep, container: UserContainerDep) -> UserProfile:
    return container.users.get_user(user_id)


@router.get("/{user_id}")
def get_user_profile(
    user_id: str,
    viewer_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> UserProfile:
    try:
        profile = container.users.get_user(user_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found") from exc
    if not can_view_profile(viewer_id, profile, container):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return profile.model_copy(update={"is_logged_in": viewer_id == user_id})


@router.patch("/me")
def update_current_user(
    body: UpdateUserProfileRequest,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> UserProfile:
    return container.users.update_user(
        user_id,
        body.nickname,
        body.bio,
        body.photo_url,
        body.profile_visibility,
    )
