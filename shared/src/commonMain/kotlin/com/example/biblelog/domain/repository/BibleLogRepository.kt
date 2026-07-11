package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.Comment
import com.example.biblelog.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

interface BibleLogRepository :
    UserRepository,
    ReadingRepository,
    JournalRepository,
    FeedRepository,
    AiRepository,
    RelationRepository {
    val notifications: StateFlow<List<NotificationItem>>

    suspend fun addComment(noteId: String, content: String): Result<Comment>

    suspend fun refreshAll()

    fun clearState()
}
