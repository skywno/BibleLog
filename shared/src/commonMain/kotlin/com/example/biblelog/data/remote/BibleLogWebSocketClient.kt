package com.example.biblelog.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * WebSocket client for real-time notification delivery.
 * Authenticates via JWT query parameter as documented in notification_service.
 */
class BibleLogWebSocketClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(accessToken: String): Flow<WebSocketMessage> = flow {
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
        session = httpClient.webSocketSession("$wsUrl/notifications/ws?token=$accessToken")
        val activeSession = session ?: return@flow
        try {
            while (activeSession.isActive) {
                val frame = activeSession.incoming.receive()
                if (frame is Frame.Text) {
                    val element = json.parseToJsonElement(frame.readText()).jsonObject
                    val type = element["type"]?.jsonPrimitive?.content ?: "unknown"
                    val payload = element["payload"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
                    emit(WebSocketMessage(type = type, payload = payload))
                }
            }
        } finally {
            disconnect()
        }
    }

    suspend fun disconnect() {
        session?.cancel()
        session = null
    }
}

data class WebSocketMessage(
    val type: String,
    val payload: Map<String, String> = emptyMap(),
)
