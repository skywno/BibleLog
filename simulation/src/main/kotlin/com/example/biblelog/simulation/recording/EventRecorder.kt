package com.example.biblelog.simulation.recording

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * Thread-safe append-only event log used by assertions and reporting.
 */
class EventRecorder {
    private val mutex = Mutex()
    private val _events = mutableListOf<RecordedEvent>()

    suspend fun record(event: RecordedEvent) {
        mutex.withLock { _events += event }
    }

    suspend fun recordNow(
        type: EventType,
        userId: String? = null,
        action: String? = null,
        method: String? = null,
        path: String? = null,
        statusCode: Int? = null,
        latency: kotlin.time.Duration? = null,
        error: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ) {
        record(
            RecordedEvent(
                type = type,
                timestamp = Clock.System.now(),
                userId = userId,
                action = action,
                method = method,
                path = path,
                statusCode = statusCode,
                latency = latency,
                error = error,
                metadata = metadata,
            ),
        )
    }

    suspend fun snapshot(): List<RecordedEvent> = mutex.withLock { _events.toList() }
}
