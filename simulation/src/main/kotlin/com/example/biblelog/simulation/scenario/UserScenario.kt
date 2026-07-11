package com.example.biblelog.simulation.scenario

import com.example.biblelog.simulation.action.ActionContext
import com.example.biblelog.simulation.action.ActionRegistry
import com.example.biblelog.simulation.config.ActionWeights
import com.example.biblelog.simulation.config.SimulationConfig
import com.example.biblelog.simulation.recording.EventRecorder
import com.example.biblelog.simulation.recording.EventType
import com.example.biblelog.simulation.user.TestUser
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Drives weighted-random user behavior with a seeded [Random] for reproducibility.
 */
class UserScenario(
    private val config: SimulationConfig,
    private val registry: ActionRegistry,
    private val recorder: EventRecorder,
    private val random: Random,
) {
    private val weights = config.actionWeights.normalized()

    suspend fun run(user: TestUser, peers: List<TestUser>, duration: Duration) {
        val deadline = kotlin.time.Clock.System.now() + duration
        while (kotlin.time.Clock.System.now() < deadline) {
            val actionName = pickAction()
            val action = registry.get(actionName)
            val context = ActionContext(
                random = random,
                knownNoteIds = peers.flatMap { it.knownNoteIds() }.distinct(),
                knownUserIds = peers.map { it.userId },
            )
            val result = runCatching { action.execute(user, context) }
            recorder.recordNow(
                EventType.ACTION_EXECUTED,
                userId = user.userId,
                action = actionName,
                error = result.exceptionOrNull()?.message,
                metadata = result.getOrNull()?.metadata ?: emptyMap(),
            )
            val delayMs = random.nextLong(config.delayRangeMs.first, config.delayRangeMs.last + 1)
            delay(delayMs)
        }
    }

    private fun pickAction(): String {
        val roll = random.nextDouble()
        var cumulative = 0.0
        for ((name, weight) in weights) {
            cumulative += weight
            if (roll <= cumulative) return name
        }
        return weights.last().first
    }
}
