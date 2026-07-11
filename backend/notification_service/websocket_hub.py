from __future__ import annotations

import asyncio
import json
import logging
from collections import defaultdict

from fastapi import WebSocket

logger = logging.getLogger(__name__)


class WebSocketHub:
    """In-memory hub mapping user IDs to active WebSocket connections."""

    def __init__(self) -> None:
        self._connections: dict[str, set[WebSocket]] = defaultdict(set)
        self._lock = asyncio.Lock()
        self.reconnect_count = 0

    async def connect(self, user_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        async with self._lock:
            if user_id in self._connections and self._connections[user_id]:
                self.reconnect_count += 1
            self._connections[user_id].add(websocket)

    async def disconnect(self, user_id: str, websocket: WebSocket) -> None:
        async with self._lock:
            connections = self._connections.get(user_id)
            if not connections:
                return
            connections.discard(websocket)
            if not connections:
                del self._connections[user_id]

    async def push(self, user_id: str, event_type: str, payload: dict) -> int:
        message = json.dumps({"type": event_type, "payload": payload})
        delivered = 0
        async with self._lock:
            connections = list(self._connections.get(user_id, set()))
        for websocket in connections:
            try:
                await websocket.send_text(message)
                delivered += 1
            except Exception:
                logger.exception("Failed to push websocket message to %s", user_id)
        return delivered

    def active_connection_count(self) -> int:
        return sum(len(conns) for conns in self._connections.values())
