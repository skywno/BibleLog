package com.example.biblelog.simulation.api

import com.example.biblelog.data.remote.ApiUpsertJournalNoteRequestDto
import com.example.biblelog.data.remote.AuthTokenHolder
import com.example.biblelog.data.remote.BibleLogApiClient
import com.example.biblelog.data.remote.BibleLogWebSocketClient
import com.example.biblelog.data.remote.WebSocketMessage
import com.example.biblelog.data.remote.createJsonHttpClient
import com.example.biblelog.simulation.config.SimulationConfig
import com.example.biblelog.simulation.network.FaultInjector
import com.example.biblelog.simulation.network.installNetworkSimulation
import com.example.biblelog.simulation.recording.EventRecorder
import com.example.biblelog.simulation.recording.EventType
import io.ktor.client.HttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class SimulatedApiClient(
    val userId: String,
    private val apiClient: BibleLogApiClient,
    private val recorder: EventRecorder,
) {
    suspend fun <T> tracked(action: String, method: String, path: String, block: suspend () -> T): T {
        val started = Clock.System.now()
        recorder.recordNow(EventType.HTTP_REQUEST, userId = userId, action = action, method = method, path = path)
        return try {
            val result = block()
            val latency = Clock.System.now() - started
            recorder.recordNow(
                EventType.HTTP_RESPONSE,
                userId = userId,
                action = action,
                method = method,
                path = path,
                statusCode = 200,
                latency = latency,
            )
            result
        } catch (error: Throwable) {
            val latency = Clock.System.now() - started
            recorder.recordNow(
                EventType.HTTP_ERROR,
                userId = userId,
                action = action,
                method = method,
                path = path,
                latency = latency,
                error = error.message,
            )
            throw error
        }
    }
}

class SimulatedWebSocketClient(
    private val userId: String,
    private val wsClient: BibleLogWebSocketClient,
    private val recorder: EventRecorder,
    private val faultInjector: FaultInjector,
    private val scope: CoroutineScope,
) {
    private val _messages = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 256)
    val messages: SharedFlow<WebSocketMessage> = _messages
    var reconnectCount: Int = 0
        private set

    private var listenerJob: Job? = null

    fun connect(accessToken: String) {
        listenerJob?.cancel()
        listenerJob = scope.launch {
            while (true) {
                try {
                    wsClient.connect(accessToken).collect { message ->
                        recorder.recordNow(
                            EventType.WEBSOCKET_RECEIVED,
                            userId = userId,
                            metadata = mapOf("type" to message.type),
                        )
                        _messages.emit(message)
                        if (faultInjector.shouldDisconnect()) {
                            recorder.recordNow(EventType.WEBSOCKET_DISCONNECTED, userId = userId)
                            wsClient.disconnect()
                            reconnectCount++
                            kotlinx.coroutines.delay(faultInjector.reconnectDelayMs())
                            recorder.recordNow(EventType.WEBSOCKET_RECONNECTED, userId = userId)
                            throw kotlinx.coroutines.CancellationException("Simulated websocket disconnect")
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    if (scope.coroutineContext[Job]?.isCancelled == true) return@launch
                } catch (error: Throwable) {
                    recorder.recordNow(
                        EventType.HTTP_ERROR,
                        userId = userId,
                        error = error.message,
                        metadata = mapOf("source" to "websocket"),
                    )
                    reconnectCount++
                    kotlinx.coroutines.delay(faultInjector.reconnectDelayMs())
                }
            }
        }
    }

    suspend fun disconnect() {
        listenerJob?.cancel()
        wsClient.disconnect()
    }
}

object SimulationClientFactory {
    fun createHttpClient(config: SimulationConfig, random: kotlin.random.Random): HttpClient {
        return createJsonHttpClient(baseUrl = config.baseUrl).config {
            installNetworkSimulation(config.network, random)
        }
    }

    fun createApiClient(httpClient: HttpClient, tokenHolder: AuthTokenHolder): BibleLogApiClient =
        BibleLogApiClient(httpClient, tokenHolder)

    fun createWebSocketClient(httpClient: HttpClient, config: SimulationConfig): BibleLogWebSocketClient =
        BibleLogWebSocketClient(httpClient, config.baseUrl)
}
