package com.example.biblelog.simulation.recording

import kotlin.time.Duration
import kotlin.time.Instant

enum class EventType {
    HTTP_REQUEST,
    HTTP_RESPONSE,
    HTTP_ERROR,
    WEBSOCKET_RECEIVED,
    WEBSOCKET_DISCONNECTED,
    WEBSOCKET_RECONNECTED,
    ACTION_EXECUTED,
    ASSERTION_VIOLATION,
}

data class RecordedEvent(
    val type: EventType,
    val timestamp: Instant,
    val userId: String?,
    val action: String? = null,
    val method: String? = null,
    val path: String? = null,
    val statusCode: Int? = null,
    val latency: Duration? = null,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)
