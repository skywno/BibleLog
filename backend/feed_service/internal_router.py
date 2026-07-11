from fastapi import APIRouter, Depends

from feed_service.deps import FeedContainerDep
from common.internal import verify_internal_token

router = APIRouter(prefix="/internal", tags=["internal"], include_in_schema=False)


@router.post("/cache/invalidate-all", dependencies=[Depends(verify_internal_token)])
def invalidate_all_feed_cache(container: FeedContainerDep) -> dict[str, str]:
    container.feed_cache.invalidate_all_feeds()
    return {"status": "ok"}
