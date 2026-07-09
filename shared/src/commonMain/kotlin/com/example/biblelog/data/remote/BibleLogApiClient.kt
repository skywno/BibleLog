package com.example.biblelog.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient

fun createJsonHttpClient(baseUrl: String = ApiConfig.BASE_URL): HttpClient {
    return createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP: $message")
                }
            }
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
}

class BibleLogApiClient(
    private val httpClient: HttpClient,
) {
    private var accessToken: String? = null

    fun setAccessToken(token: String?) {
        accessToken = token
    }

    suspend fun devLogin(email: String = "demo@biblelog.app"): ApiAuthTokenResponseDto =
        httpClient.post("/auth/dev/login?email=$email").body()

    suspend fun logout() {
        httpClient.post("/auth/logout") {
            authHeader()
        }
    }

    suspend fun getOAuthAuthorizeUrl(
        provider: String,
        redirectUri: String,
    ): ApiOAuthAuthorizeResponseDto =
        httpClient.get("/auth/$provider/authorize?redirect_uri=$redirectUri").body()

    suspend fun getCurrentUser(): ApiUserProfileDto =
        authorizedGet("/users/me")

    suspend fun updateCurrentUser(nickname: String?, bio: String?): ApiUserProfileDto =
        authorizedPatch("/users/me", ApiUpdateUserProfileRequestDto(nickname, bio))

    suspend fun listReadingRecords(): List<ApiReadingRecordDto> =
        authorizedGet("/reading/records")

    suspend fun createReadingRecord(body: ApiCreateReadingRecordRequestDto): ApiReadingRecordDto =
        authorizedPost("/reading/records", body)

    suspend fun getReadingProgress(): ApiReadingProgressDto =
        authorizedGet("/reading/progress")

    suspend fun getReadingStats(): ApiReadingStatsDto =
        authorizedGet("/reading/stats")

    suspend fun listJournalNotes(): List<ApiMeditationNoteDto> =
        authorizedGet("/journal/notes")

    suspend fun createJournalNote(body: ApiUpsertJournalNoteRequestDto): ApiMeditationNoteDto =
        authorizedPost("/journal/notes", body)

    suspend fun updateJournalNote(noteId: String, body: ApiUpsertJournalNoteRequestDto): ApiMeditationNoteDto =
        authorizedPatch("/journal/notes/$noteId", body)

    suspend fun deleteJournalNote(noteId: String) {
        authorizedDelete("/journal/notes/$noteId")
    }

    suspend fun listFeed(filter: String = "all", sort: String = "latest"): ApiFeedPageResponseDto =
        authorizedGet("/feed?filter=$filter&sort=$sort")

    suspend fun toggleReaction(noteId: String, body: ApiToggleReactionRequestDto): ApiFeedItemDto =
        authorizedPost("/feed/$noteId/reactions", body)

    suspend fun listAiConversations(): List<ApiAiConversationSummaryDto> =
        authorizedGet("/ai/conversations")

    suspend fun createAiConversation(): ApiAiConversationSummaryDto =
        httpClient.post("/ai/conversations") {
            authHeader()
        }.body()

    suspend fun listAiMessages(conversationId: String): List<ApiAiMessageDto> =
        authorizedGet("/ai/conversations/$conversationId/messages")

    suspend fun sendAiMessage(
        conversationId: String,
        body: ApiSendAiMessageRequestDto,
    ): ApiSendAiMessageResponseDto =
        authorizedPost("/ai/conversations/$conversationId/messages", body)

    private suspend inline fun <reified T> authorizedGet(path: String): T =
        httpClient.get(path) {
            authHeader()
        }.body()

    private suspend inline fun <reified T, reified B> authorizedPost(path: String, body: B): T =
        httpClient.post(path) {
            authHeader()
            setBody(body)
        }.body()

    private suspend inline fun <reified T, reified B> authorizedPatch(path: String, body: B): T =
        httpClient.patch(path) {
            authHeader()
            setBody(body)
        }.body()

    private suspend fun authorizedDelete(path: String) {
        httpClient.delete(path) {
            authHeader()
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeader() {
        accessToken?.let { header("Authorization", "Bearer $it") }
    }
}
