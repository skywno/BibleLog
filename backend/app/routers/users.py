from fastapi import APIRouter, Depends

from app.deps import CurrentUserIdDep, get_current_user_id
from app.models import UpdateUserProfileRequest, UserProfile
from app.store import store

router = APIRouter(
    prefix="/users",
    tags=["users"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/me")
def get_current_user(user_id: CurrentUserIdDep) -> UserProfile:
    return store.get_user(user_id)


@router.patch("/me")
def update_current_user(
    body: UpdateUserProfileRequest,
    user_id: CurrentUserIdDep,
) -> UserProfile:
    return store.update_user(user_id, body.nickname, body.bio)
