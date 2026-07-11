from __future__ import annotations

from typing import Protocol

from common.db.redis_client import MemoryRedis, get_redis
from common.models import NoteSummary


class FeedCacheSettings(Protocol):
    feed_latest_ttl_seconds: int
    feed_popular_ttl_seconds: int
    redis_url: str
    redis_enabled: bool


class FeedCacheService:
    def __init__(self, settings: FeedCacheSettings) -> None:
        self._settings = settings
        self._redis = get_redis(settings)

    def feed_zset_key(self, viewer_id: str, feed_filter: str, sort: str) -> str:
        return f"feed:zset:{viewer_id}:{feed_filter}:{sort}"

    def populate_latest(self, viewer_id: str, feed_filter: str, entries: list[tuple[str, float]]) -> None:
        key = self.feed_zset_key(viewer_id, feed_filter, "latest")
        self._redis.delete(key)
        if entries:
            mapping = {note_id: score for note_id, score in entries}
            self._redis.zadd(key, mapping)
        self._redis.expire(key, self._settings.feed_latest_ttl_seconds)

    def populate_popular(self, viewer_id: str, feed_filter: str, entries: list[tuple[str, float]]) -> None:
        key = self.feed_zset_key(viewer_id, feed_filter, "popular")
        self._redis.delete(key)
        if entries:
            mapping = {note_id: score for note_id, score in entries}
            self._redis.zadd(key, mapping)
        self._redis.expire(key, self._settings.feed_popular_ttl_seconds)

    def get_zset_entries(self, key: str) -> list[tuple[str, float]]:
        raw = self._redis.zrevrange(key, 0, -1, withscores=True)
        if not raw:
            return []
        if isinstance(self._redis, MemoryRedis):
            return [(member, score) for member, score in raw]  # type: ignore[misc]
        return [(member, float(score)) for member, score in raw]

    def has_feed_cache(self, viewer_id: str, feed_filter: str, sort: str) -> bool:
        key = self.feed_zset_key(viewer_id, feed_filter, sort)
        return bool(self.get_zset_entries(key))

    def invalidate_all_feeds(self) -> None:
        keys = self._redis.keys("feed:zset:")
        if keys:
            self._redis.delete(*keys)

    def invalidate_viewer_feeds(self, viewer_id: str) -> None:
        keys = self._redis.keys(f"feed:zset:{viewer_id}:")
        if keys:
            self._redis.delete(*keys)

    def remove_note_from_feeds(self, note_id: str) -> None:
        keys = self._redis.keys("feed:zset:")
        for key in keys:
            self._redis.zrem(key, note_id)

    def set_note_summary(self, summary: NoteSummary) -> None:
        self._redis.set(
            f"note:summary:{summary.id}",
            summary.model_dump_json(),
            ex=1800,
        )

    def get_note_summary(self, note_id: str) -> NoteSummary | None:
        raw = self._redis.get(f"note:summary:{note_id}")
        if not raw:
            return None
        return NoteSummary.model_validate_json(raw)

    def invalidate_note_summary(self, note_id: str) -> None:
        self._redis.delete(f"note:summary:{note_id}")

    def on_note_changed(self, note_id: str) -> None:
        self.invalidate_note_summary(note_id)
        self.invalidate_all_feeds()

    def on_note_deleted(self, note_id: str) -> None:
        self.invalidate_note_summary(note_id)
        self.remove_note_from_feeds(note_id)
