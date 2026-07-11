package com.example.biblelog.simulation.runner

import com.example.biblelog.data.remote.AuthTokenHolder
import com.example.biblelog.simulation.action.BuiltinActions
import com.example.biblelog.simulation.action.ActionRegistry
import com.example.biblelog.simulation.api.SimulationClientFactory
import com.example.biblelog.simulation.api.SimulatedApiClient
import com.example.biblelog.simulation.api.SimulatedWebSocketClient
import com.example.biblelog.simulation.assertion.AssertionSuite
import com.example.biblelog.simulation.config.SimulationConfig
import com.example.biblelog.simulation.network.FaultInjector
import com.example.biblelog.simulation.recording.EventRecorder
import com.example.biblelog.simulation.report.SimulationReport
import com.example.biblelog.simulation.scenario.UserScenario
import com.example.biblelog.simulation.user.TestUser
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlin.random.Random

/**
 * Launches [SimulationConfig.userCount] users concurrently using structured concurrency.
 * Each user runs in its own coroutine and is cancelled when the configured duration elapses.
 */
class SimulationRunner(
    private val config: SimulationConfig,
    private val registry: ActionRegistry = BuiltinActions.defaultRegistry(),
) {
    private val random = Random(config.seed)
    private val recorder = EventRecorder()

    suspend fun run(): SimulationReport = coroutineScope {
        val httpClient = SimulationClientFactory.createHttpClient(config, random)
        try {
            val users = (1..config.userCount).map { index ->
                createTestUser(httpClient, index, this)
            }
            users.forEach { it.login() }
            supervisorScope {
                val scenario = UserScenario(config, registry, recorder, Random(config.seed))
                users.map { user ->
                    async {
                        scenario.run(user, users, config.duration)
                    }
                }.awaitAll()
            }
            users.forEach { it.refreshFeed() }
            delay(config.quiescencePollInterval)
            val violations = AssertionSuite(config, recorder).verify(users)
            SimulationReport.from(config, recorder.snapshot(), users, violations)
        } finally {
            httpClient.close()
        }
    }

    private fun createTestUser(
        httpClient: HttpClient,
        index: Int,
        scope: CoroutineScope,
    ): TestUser {
        val email = "sim-user-$index@biblelog.test"
        val tokenHolder = AuthTokenHolder()
        val rawClient = SimulationClientFactory.createApiClient(httpClient, tokenHolder)
        val api = SimulatedApiClient(userId = "pending-$index", apiClient = rawClient, recorder = recorder)
        val wsClient = if (config.webSocketEnabled) {
            val faultInjector = FaultInjector(config.network, Random(config.seed + index))
            SimulatedWebSocketClient(
                userId = "pending-$index",
                wsClient = SimulationClientFactory.createWebSocketClient(httpClient, config),
                recorder = recorder,
                faultInjector = faultInjector,
                scope = scope,
            )
        } else {
            null
        }
        return TestUser(
            userId = "pending-$index",
            email = email,
            api = api,
            rawClient = rawClient,
            webSocket = wsClient,
        ).also {
            // userId is resolved after login from /users/me in assertions via feed ownership;
            // store email-based identity for reporting.
        }
    }
}
