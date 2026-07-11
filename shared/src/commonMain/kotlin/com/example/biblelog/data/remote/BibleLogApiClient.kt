package com.example.biblelog.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
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
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient

fun createJsonHttpClient(
    baseUrl: String = ApiConfig.BASE_URL,
    tokenHolder: AuthTokenHolder? = null,
): HttpClient {
    return createPlatformHttpClient().config {
        install(WebSockets)
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
        if (tokenHolder != null) {
            install(AuthRefreshPlugin) {
                onUnauthorized = { tokenHolder.onUnauthorized?.invoke() == true }
                authorizationHeader = { tokenHolder.accessToken }
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
    private val tokenHolder: AuthTokenHolder,
) {
    fun setAccessToken(token: String?) {
        tokenHolder.accessToken = token
    }

    suspend fun devLogin(email: String = "demo@biblelog.app"): ApiAuthTokenResponseDto =
        httpClient.post("/auth/dev/login?email=$email").body()

    suspend fun refreshAuthToken(refreshToken: String): ApiAuthTokenResponseDto =
        httpClient.post("/auth/token/refresh") {
            setBody(ApiRefreshTokenRequestDto(refreshToken))
        }.decodeBody()

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

    suspend fun getUserProfile(userId: String): ApiUserProfileDto =
        authorizedGet("/users/$userId")

    suspend fun updateCurrentUser(
        nickname: String? = null,
        bio: String? = null,
        photoUrl: String? = null,
        profileVisibility: String? = null,
    ): ApiUserProfileDto =
        authorizedPatch(
            "/users/me",
            ApiUpdateUserProfileRequestDto(
                nickname = nickname,
                bio = bio,
                photoUrl = photoUrl,
                profileVisibility = profileVisibility,
            ),
        )

    suspend fun listUserNotes(userId: String): List<ApiMeditationNoteDto> =
        authorizedGet("/journal/users/$userId/notes")

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

    suspend fun listFeed(
        filter: String = "all",
        sort: String = "latest",
        cursor: String? = null,
    ): ApiFeedPageResponseDto {
        val cursorParam = cursor?.let { "&cursor=$it" }.orEmpty()
        return authorizedGet("/feed?filter=$filter&sort=$sort$cursorParam")
    }

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

    suspend fun searchUsers(query: String, limit: Int = 20): List<ApiUserSearchResultDto> =
        authorizedGet("/users/search?q=$query&limit=$limit")

    suspend fun searchChurches(query: String, limit: Int = 20): List<ApiChurchDto> =
        authorizedGet("/churches/search?q=$query&limit=$limit")

    suspend fun searchSmallGroups(query: String, limit: Int = 20): List<ApiSmallGroupDto> =
        authorizedGet("/small-groups/search?q=$query&limit=$limit")

    suspend fun listFriends(): List<ApiUserSearchResultDto> =
        authorizedGet("/friends")

    suspend fun sendFriendRequest(toUserId: String): ApiFriendRequestDto =
        authorizedPost("/friends/requests", ApiSendFriendRequestDto(toUserId))

    suspend fun acceptFriendRequest(requestId: String): ApiFriendRequestDto =
        authorizedPostEmpty("/friends/requests/$requestId/accept")

    suspend fun listIncomingFollowRequests(): List<ApiFollowRequestDto> =
        authorizedGet("/follows/requests/incoming")

    suspend fun acceptFollowRequest(requestId: String): ApiFollowRequestDto =
        authorizedPostEmpty("/follows/requests/$requestId/accept")

    suspend fun rejectFollowRequest(requestId: String): ApiFollowRequestDto =
        authorizedPostEmpty("/follows/requests/$requestId/reject")

    suspend fun followUser(userId: String) {
        httpClient.post("/follows/$userId") { authHeader() }
    }

    suspend fun unfollowUser(userId: String) {
        authorizedDelete("/follows/$userId")
    }

    suspend fun listFollowing(): List<ApiFollowUserDto> =
        authorizedGet("/follows")

    suspend fun listFollowers(): List<ApiFollowUserDto> =
        authorizedGet("/followers")

    suspend fun getChurch(churchId: String): ApiChurchDto =
        authorizedGet("/churches/$churchId")

    suspend fun getSmallGroup(groupId: String): ApiSmallGroupDto =
        authorizedGet("/small-groups/$groupId")

    suspend fun createComment(noteId: String, body: ApiCreateCommentRequestDto): ApiCommentDto =
        authorizedPost("/social/notes/$noteId/comments", body)

    suspend fun listComments(noteId: String, cursor: String? = null): ApiCommentPageResponseDto {
        val cursorParam = cursor?.let { "&cursor=$it" }.orEmpty()
        return authorizedGet("/social/notes/$noteId/comments?limit=20$cursorParam")
    }

    suspend fun listNotifications(cursor: String? = null): ApiNotificationPageResponseDto {
        val cursorParam = cursor?.let { "&cursor=$it" }.orEmpty()
        return authorizedGet("/notifications?limit=20$cursorParam")
    }

    suspend fun createChurch(body: ApiCreateChurchRequestDto): ApiChurchDto =
        authorizedPost("/churches", body)

    suspend fun joinChurch(churchId: String) {
        httpClient.post("/churches/$churchId/members") { authHeader() }
    }

    suspend fun createSmallGroup(body: ApiCreateSmallGroupRequestDto): ApiSmallGroupDto =
        authorizedPost("/small-groups", body)

    suspend fun joinSmallGroup(groupId: String) {
        httpClient.post("/small-groups/$groupId/members") { authHeader() }
    }

    suspend fun getMemberships(): ApiUserMembershipsDto =
        authorizedGet("/me/memberships")

    private suspend inline fun <reified T> authorizedPostEmpty(path: String): T =
        httpClient.post(path) {
            authHeader()
        }.decodeBody()

    private suspend inline fun <reified T> authorizedGet(path: String): T =
        httpClient.get(path) {
            authHeader()
        }.decodeBody()

    private suspend inline fun <reified T, reified B> authorizedPost(path: String, body: B): T =
        httpClient.post(path) {
            authHeader()
            setBody(body)
        }.decodeBody()

    private suspend inline fun <reified T, reified B> authorizedPatch(path: String, body: B): T =
        httpClient.patch(path) {
            authHeader()
            setBody(body)
        }.decodeBody()

    private suspend fun authorizedDelete(path: String) {
        val response = httpClient.delete(path) {
            authHeader()
        }
        if (!response.status.isSuccess()) {
            throw apiHttpException(response)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeader() {
        tokenHolder.accessToken?.let { header("Authorization", "Bearer $it") }
    }
}
