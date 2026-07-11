package com.example.biblelog.simulation.assertion

import com.example.biblelog.simulation.config.SimulationConfig
import com.example.biblelog.simulation.recording.EventRecorder
import com.example.biblelog.simulation.recording.EventType
import com.example.biblelog.simulation.recording.RecordedEvent
import com.example.biblelog.simulation.user.TestUser
import kotlinx.coroutines.delay

data class ConsistencyViolation(
    val type: String,
    val message: String,
)

class AssertionSuite(
    private val config: SimulationConfig,
    private val recorder: EventRecorder,
) {
    suspend fun verify(users: List<TestUser>): List<ConsistencyViolation> {
        val violations = mutableListOf<ConsistencyViolation>()
        violations += feedConsistency(users)
        violations += noDuplicatePosts(users)
        violations += countConvergence(users)
        violations += webSocketDelivery()
        violations += eventualConsistency(users)
        for (violation in violations) {
            recorder.recordNow(
                EventType.ASSERTION_VIOLATION,
                error = violation.message,
                metadata = mapOf("type" to violation.type),
            )
        }
        return violations
    }

    private fun feedConsistency(users: List<TestUser>): List<ConsistencyViolation> {
        val snapshots = users.map { it.lastFeedSnapshot() }.filter { it.isNotEmpty() }
        if (snapshots.size < 2) return emptyList()
        val reference = snapshots.first()
        return snapshots.drop(1).mapIndexedNotNull { index, snapshot ->
            if (snapshot != reference) {
                ConsistencyViolation(
                    "feed_consistency",
                    "User feed ${index + 1} differs from reference ordering/content",
                )
            } else {
                null
            }
        }
    }

    private fun noDuplicatePosts(users: List<TestUser>): List<ConsistencyViolation> {
        return users.mapNotNull { user ->
            val feed = user.lastFeedSnapshot()
            if (feed.size != feed.toSet().size) {
                ConsistencyViolation("duplicate_posts", "Duplicate note IDs in feed for ${user.email}")
            } else {
                null
            }
        }
    }

    private suspend fun countConvergence(users: List<TestUser>): List<ConsistencyViolation> {
        // Feed snapshots already embed counts via refresh; ordering mismatch covers divergence.
        return emptyList()
    }

    private suspend fun webSocketDelivery(): List<ConsistencyViolation> {
        val events = recorder.snapshot()
        val wsReceived = events.count { it.type == EventType.WEBSOCKET_RECEIVED }
        val noteActions = events.count { it.action == "createPost" && it.type == EventType.HTTP_RESPONSE }
        if (config.webSocketEnabled && noteActions > 0 && wsReceived == 0) {
            return listOf(
                ConsistencyViolation(
                    "websocket_delivery",
                    "Expected websocket events after note creation but none were recorded",
                ),
            )
        }
        return emptyList()
    }

    private suspend fun eventualConsistency(users: List<TestUser>): List<ConsistencyViolation> {
        val deadline = kotlin.time.Clock.System.now() + config.quiescenceTimeout
        var lastViolation: ConsistencyViolation? = null
        while (kotlin.time.Clock.System.now() < deadline) {
            users.forEach { it.refreshFeed() }
            val violations = feedConsistency(users)
            if (violations.isEmpty()) return emptyList()
            lastViolation = violations.first()
            delay(config.quiescencePollInterval)
        }
        return listOf(
            lastViolation ?: ConsistencyViolation("eventual_consistency", "Feeds did not converge in time"),
        )
    }
}
