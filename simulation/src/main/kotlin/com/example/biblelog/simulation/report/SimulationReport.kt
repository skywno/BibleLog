package com.example.biblelog.simulation.report

import com.example.biblelog.simulation.assertion.ConsistencyViolation
import com.example.biblelog.simulation.config.SimulationConfig
import com.example.biblelog.simulation.recording.EventType
import com.example.biblelog.simulation.recording.RecordedEvent
import com.example.biblelog.simulation.user.TestUser
import kotlin.time.Duration

data class SimulationReport(
    val config: SimulationConfig,
    val totalRequests: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageLatency: Duration,
    val p95Latency: Duration,
    val webSocketReconnectCount: Int,
    val actionBreakdown: Map<String, Int>,
    val violations: List<ConsistencyViolation>,
    val events: List<RecordedEvent>,
) {
    val successRate: Double
        get() = if (totalRequests == 0) 1.0 else successCount.toDouble() / totalRequests

    companion object {
        fun from(
            config: SimulationConfig,
            events: List<RecordedEvent>,
            users: List<TestUser>,
            violations: List<ConsistencyViolation>,
        ): SimulationReport {
            val responses = events.filter { it.type == EventType.HTTP_RESPONSE }
            val errors = events.filter { it.type == EventType.HTTP_ERROR }
            val latencies = responses.mapNotNull { it.latency }.sorted()
            val avg = if (latencies.isEmpty()) {
                Duration.ZERO
            } else {
                latencies.reduce { acc, duration -> acc + duration } / latencies.size
            }
            val p95 = latencies.getOrElse((latencies.size * 0.95).toInt().coerceAtMost(latencies.lastIndex)) {
                Duration.ZERO
            }
            val actionBreakdown = events
                .filter { it.type == EventType.ACTION_EXECUTED }
                .groupingBy { it.action.orEmpty() }
                .eachCount()
            return SimulationReport(
                config = config,
                totalRequests = responses.size + errors.size,
                successCount = responses.size,
                failureCount = errors.size,
                averageLatency = avg,
                p95Latency = p95,
                webSocketReconnectCount = users.sumOf { it.webSocketReconnectCount() },
                actionBreakdown = actionBreakdown,
                violations = violations,
                events = events,
            )
        }
    }
}
