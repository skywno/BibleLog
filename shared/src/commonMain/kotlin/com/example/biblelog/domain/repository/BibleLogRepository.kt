package com.example.biblelog.domain.repository

import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedSort
import com.example.biblelog.domain.model.Comment
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface BibleLogRepository {
    val currentUser: StateFlow<UserProfile>
    val readingRecords: StateFlow<List<ReadingRecord>>
    val notes: StateFlow<List<MeditationNote>>
    val feed: StateFlow<List<FeedItem>>
    val aiMessages: StateFlow<List<AiMessage>>
    val notifications: StateFlow<List<NotificationItem>>

    fun getReadingProgress(): ReadingProgress
    fun getReadingStats(): ReadingStats
    fun getReadingDates(): Set<LocalDate>

    suspend fun addReadingRecord(
        reference: BibleReference,
        minutesRead: Int,
        date: LocalDate,
    ): Result<Unit>

    suspend fun saveNote(
        content: String,
        prayerTopic: String?,
        emotion: Emotion?,
        reference: BibleReference?,
        visibility: NoteVisibility,
        noteId: String? = null,
    ): Result<MeditationNote>

    suspend fun deleteNote(noteId: String): Result<Unit>

    suspend fun addComment(noteId: String, content: String): Result<Comment>

    suspend fun loadFeed(
        filter: FeedFilter = FeedFilter.ALL,
        sort: FeedSort = FeedSort.LATEST,
    ): Result<Unit>

    suspend fun toggleReaction(noteId: String, reaction: FaithReaction): Result<Unit>

    suspend fun sendAiMessage(content: String, mode: AiConversationMode = AiConversationMode.CHAT): Result<AiMessage>

    suspend fun updateProfile(nickname: String, bio: String): Result<Unit>
}
