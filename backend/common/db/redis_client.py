from __future__ import annotations

import logging

import redis

from common.config import Settings

logger = logging.getLogger(__name__)

_client: redis.Redis | None = None
_memory_fallback: dict[str, object] = {}


class MemoryRedis:
    """Minimal Redis Sorted Set + key-value fallback for memory storage mode."""

    def __init__(self) -> None:
        self._kv: dict[str, str | int] = {}
        self._zsets: dict[str, dict[str, float]] = {}

    def ping(self) -> bool:
        return True

    def get(self, key: str) -> str | None:
        value = self._kv.get(key)
        return str(value) if value is not None else None

    def set(self, key: str, value: str, ex: int | None = None) -> bool:
        self._kv[key] = value
        return True

    def delete(self, *keys: str) -> int:
        removed = 0
        for key in keys:
            if key in self._kv:
                del self._kv[key]
                removed += 1
            if key in self._zsets:
                del self._zsets[key]
                removed += 1
        return removed

    def keys(self, pattern: str) -> list[str]:
        prefix = pattern.rstrip("*")
        result = set(self._kv) | set(self._zsets)
        return [key for key in result if key.startswith(prefix)]

    def zadd(self, key: str, mapping: dict[str, float]) -> int:
        zset = self._zsets.setdefault(key, {})
        added = 0
        for member, score in mapping.items():
            if member not in zset:
                added += 1
            zset[member] = score
        return added

    def zrevrange(self, key: str, start: int, end: int, withscores: bool = False):
        zset = self._zsets.get(key, {})
        items = sorted(zset.items(), key=lambda item: (item[1], item[0]), reverse=True)
        sliced = items[start : end + 1 if end >= 0 else None]
        if withscores:
            return sliced
        return [member for member, _ in sliced]

    def expire(self, key: str, seconds: int) -> bool:
        return True

    def zrem(self, key: str, *members: str) -> int:
        zset = self._zsets.get(key)
        if not zset:
            return 0
        removed = 0
        for member in members:
            if member in zset:
                del zset[member]
                removed += 1
        return removed


def get_redis(settings: Settings) -> redis.Redis | MemoryRedis:
    global _client, _memory_fallback
    if not settings.redis_enabled:
        if "client" not in _memory_fallback:
            _memory_fallback["client"] = MemoryRedis()
        return _memory_fallback["client"]  # type: ignore[return-value]

    if settings.storage_backend == "memory":
        if "client" not in _memory_fallback:
            _memory_fallback["client"] = MemoryRedis()
        return _memory_fallback["client"]  # type: ignore[return-value]

    if _client is None:
        _client = redis.Redis.from_url(settings.redis_url, decode_responses=True)
        _client.ping()
        logger.info("Connected to Redis at %s", settings.redis_url)
    return _client


def close_redis() -> None:
    global _client, _memory_fallback
    if _client is not None:
        _client.close()
        _client = None
    _memory_fallback.clear()
