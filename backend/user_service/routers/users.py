from fastapi import APIRouter, Depends

from common.models import UpdateUserProfileRequest, UserProfile
from user_service.deps import CurrentUserIdDep, UserContainerDep, get_current_user_id

router = APIRouter(
    prefix="/users",
    tags=["users"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/me")
def get_current_user(user_id: CurrentUserIdDep, container: UserContainerDep) -> UserProfile:
    return container.users.get_user(user_id)


@router.patch("/me")
def update_current_user(
    body: UpdateUserProfileRequest,
    user_id: CurrentUserIdDep,
    container: UserContainerDep,
) -> UserProfile:
    return container.users.update_user(user_id, body.nickname, body.bio)
