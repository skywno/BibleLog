package com.example.biblelog.simulation.network

import com.example.biblelog.simulation.config.NetworkConfig
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestPipeline
import kotlinx.coroutines.delay
import kotlin.random.Random

class FaultInjector(
    private val config: NetworkConfig,
    private val random: Random,
) {
    fun shouldDisconnect(): Boolean =
        config.webSocketDisconnectProbability > 0 &&
            random.nextDouble() < config.webSocketDisconnectProbability

    suspend fun reconnectDelayMs(): Long =
        if (config.webSocketReconnectDelayMs.first == config.webSocketReconnectDelayMs.last) {
            config.webSocketReconnectDelayMs.first
        } else {
            random.nextLong(
                config.webSocketReconnectDelayMs.first,
                config.webSocketReconnectDelayMs.last + 1,
            )
        }
}

fun <T : io.ktor.client.engine.HttpClientEngineConfig> HttpClientConfig<T>.installNetworkSimulation(
    config: NetworkConfig,
    random: Random,
) {
    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMs
        connectTimeoutMillis = config.requestTimeoutMs
        socketTimeoutMillis = config.requestTimeoutMs
    }
    install(HttpRequestRetry) {
        maxRetries = config.maxRetries
        retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)
        exponentialDelay(
            base = config.retryBackoffMs,
            maxDelayMs = config.retryBackoffMs * 8,
        )
    }
    requestPipeline.intercept(HttpRequestPipeline.Before) {
        if (config.packetLossRate > 0 && random.nextDouble() < config.packetLossRate) {
            throw java.io.IOException("Simulated packet loss for ${context.url}")
        }
        val latency = when {
            config.latencyMs.first == config.latencyMs.last -> config.latencyMs.first
            else -> random.nextLong(config.latencyMs.first, config.latencyMs.last + 1)
        }
        if (latency > 0) delay(latency)
    }
}
