from __future__ import annotations

import httpx

_client: httpx.AsyncClient | None = None


def get_async_http_client() -> httpx.AsyncClient:
    global _client
    if _client is None:
        _client = httpx.AsyncClient(timeout=15.0)
    return _client


async def close_async_http_client() -> None:
    global _client
    if _client is not None:
        await _client.aclose()
        _client = None
