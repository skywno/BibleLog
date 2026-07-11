package com.example.biblelog.simulation.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Top-level configuration for a multi-user simulation run.
 * All fields are immutable so runs can be reproduced from a saved snapshot.
 */
data class SimulationConfig(
    val baseUrl: String = "http://localhost:8000",
    val userCount: Int = 10,
    val duration: Duration = 30.seconds,
    val seed: Long = 42L,
    val actionWeights: ActionWeights = ActionWeights(),
    val delayRangeMs: LongRange = 100L..2_000L,
    val webSocketEnabled: Boolean = true,
    val network: NetworkConfig = NetworkConfig(),
    val quiescenceTimeout: Duration = 30.seconds,
    val quiescencePollInterval: Duration = 500.milliseconds,
    val logLevel: SimulationLogLevel = SimulationLogLevel.INFO,
)

enum class SimulationLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * Weighted action probabilities. Values are normalized at runtime if they do not sum to 1.
 */
data class ActionWeights(
    val createPost: Double = 0.10,
    val like: Double = 0.35,
    val comment: Double = 0.20,
    val follow: Double = 0.10,
    val refreshFeed: Double = 0.25,
) {
    fun normalized(): List<Pair<String, Double>> {
        val entries = listOf(
            "createPost" to createPost,
            "like" to like,
            "comment" to comment,
            "follow" to follow,
            "refreshFeed" to refreshFeed,
        )
        val total = entries.sumOf { it.second }
        require(total > 0) { "Action weights must sum to a positive value" }
        return entries.map { (name, weight) -> name to weight / total }
    }
}

data class NetworkConfig(
    val latencyMs: LongRange = 0L..0L,
    val packetLossRate: Double = 0.0,
    val requestTimeoutMs: Long = 30_000L,
    val maxRetries: Int = 3,
    val retryBackoffMs: Long = 200L,
    val webSocketDisconnectProbability: Double = 0.0,
    val webSocketReconnectDelayMs: LongRange = 500L..2_000L,
)

/** Preset for the example 100-user / 5-minute scenario from the specification. */
fun communityLoadConfig(seed: Long = 42L) = SimulationConfig(
    userCount = 100,
    duration = 5.minutes,
    seed = seed,
    actionWeights = ActionWeights(
        createPost = 0.10,
        like = 0.35,
        comment = 0.20,
        follow = 0.10,
        refreshFeed = 0.25,
    ),
    webSocketEnabled = true,
)

/** Fast CI-friendly preset excluded from default `./gradlew :simulation:test`. */
fun ciSmokeConfig(seed: Long = 7L) = SimulationConfig(
    userCount = 10,
    duration = 30.seconds,
    seed = seed,
    webSocketEnabled = true,
)
