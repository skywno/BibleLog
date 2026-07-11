package com.example.biblelog.simulation.user

import com.example.biblelog.data.remote.ApiCreateCommentRequestDto
import com.example.biblelog.data.remote.ApiMeditationNoteDto
import com.example.biblelog.data.remote.ApiToggleReactionRequestDto
import com.example.biblelog.data.remote.ApiUpsertJournalNoteRequestDto
import com.example.biblelog.simulation.api.SimulatedApiClient
import com.example.biblelog.simulation.api.SimulatedWebSocketClient

/**
 * Represents one simulated end-user with authenticated API access.
 */
class TestUser(
    val userId: String,
    val email: String,
    private val api: SimulatedApiClient,
    private val rawClient: com.example.biblelog.data.remote.BibleLogApiClient,
    val webSocket: SimulatedWebSocketClient?,
) {
    private val createdNoteIds = mutableListOf<String>()
    private var feedSnapshot: List<String> = emptyList()

    suspend fun login() {
        val tokens = api.tracked("login", "POST", "/auth/dev/login") {
            rawClient.devLogin(email)
        }
        rawClient.setAccessToken(tokens.accessToken)
        webSocket?.connect(tokens.accessToken)
    }

    suspend fun logout() {
        webSocket?.disconnect()
        api.tracked("logout", "POST", "/auth/logout") { rawClient.logout() }
        rawClient.setAccessToken(null)
    }

    suspend fun createPost(content: String): ApiMeditationNoteDto {
        val note = api.tracked("createPost", "POST", "/journal/notes") {
            rawClient.createJournalNote(
                ApiUpsertJournalNoteRequestDto(
                    content = content,
                    visibility = "public",
                ),
            )
        }
        createdNoteIds += note.id
        return note
    }

    suspend fun editPost(noteId: String, content: String): ApiMeditationNoteDto =
        api.tracked("editPost", "PATCH", "/journal/notes/$noteId") {
            rawClient.updateJournalNote(
                noteId,
                ApiUpsertJournalNoteRequestDto(content = content, visibility = "public"),
            )
        }

    suspend fun deletePost(noteId: String) {
        api.tracked("deletePost", "DELETE", "/journal/notes/$noteId") {
            rawClient.deleteJournalNote(noteId)
        }
        createdNoteIds.remove(noteId)
    }

    suspend fun likeNote(noteId: String) {
        api.tracked("like", "POST", "/feed/$noteId/reactions") {
            rawClient.toggleReaction(noteId, ApiToggleReactionRequestDto("empathy"))
        }
    }

    suspend fun commentOnNote(noteId: String, content: String) =
        api.tracked("comment", "POST", "/social/notes/$noteId/comments") {
            rawClient.createComment(noteId, ApiCreateCommentRequestDto(content))
        }

    suspend fun follow(targetUserId: String) {
        api.tracked("follow", "POST", "/follows/$targetUserId") {
            rawClient.followUser(targetUserId)
        }
    }

    suspend fun unfollow(targetUserId: String) {
        api.tracked("unfollow", "DELETE", "/follows/$targetUserId") {
            rawClient.unfollowUser(targetUserId)
        }
    }

    suspend fun refreshFeed(): List<String> {
        val page = api.tracked("refreshFeed", "GET", "/feed") {
            rawClient.listFeed()
        }
        feedSnapshot = page.items.map { it.note.id }
        return feedSnapshot
    }

    fun knownNoteIds(): List<String> = createdNoteIds.toList()
    fun lastFeedSnapshot(): List<String> = feedSnapshot
    fun webSocketReconnectCount(): Int = webSocket?.reconnectCount ?: 0
}
