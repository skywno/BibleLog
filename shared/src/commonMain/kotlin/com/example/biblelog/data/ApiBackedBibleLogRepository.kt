package com.example.biblelog.data

import com.example.biblelog.data.remote.ApiBibleReferenceDto
import com.example.biblelog.data.remote.ApiCreateReadingRecordRequestDto
import com.example.biblelog.data.remote.ApiFeedItemDto
import com.example.biblelog.data.remote.ApiMeditationNoteDto
import com.example.biblelog.data.remote.ApiReadingProgressDto
import com.example.biblelog.data.remote.ApiReadingRecordDto
import com.example.biblelog.data.remote.ApiReadingStatsDto
import com.example.biblelog.data.remote.ApiSendAiMessageRequestDto
import com.example.biblelog.data.remote.ApiUpsertJournalNoteRequestDto
import com.example.biblelog.data.remote.BibleLogApiClient
import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.Comment
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.FeedSort
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.model.ReactionCount
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.repository.BibleLogRepository
import com.example.biblelog.util.currentInstant
import com.example.biblelog.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class ApiBackedBibleLogRepository(
    private val apiClient: BibleLogApiClient,
) : BibleLogRepository {

    private val _currentUser = MutableStateFlow(UserProfile("", "", "", false))
    private val _readingRecords = MutableStateFlow<List<ReadingRecord>>(emptyList())
    private val _notes = MutableStateFlow<List<MeditationNote>>(emptyList())
    private val _feed = MutableStateFlow<List<FeedItem>>(emptyList())
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())

    private var cachedProgress = ReadingProgress(0f, 0f, 0f, emptyMap())
    private var cachedStats = ReadingStats(0, 0f, emptyMap(), 0, 0)
    private var aiConversationId: String? = null
    private var feedFilter = FeedFilter.ALL
    private var feedSort = FeedSort.LATEST

    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()
    override val readingRecords: StateFlow<List<ReadingRecord>> = _readingRecords.asStateFlow()
    override val notes: StateFlow<List<MeditationNote>> = _notes.asStateFlow()
    override val feed: StateFlow<List<FeedItem>> = _feed.asStateFlow()
    override val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun seedCurrentUser(user: UserProfile) {
        _currentUser.value = user
    }

    suspend fun refreshAll() {
        _currentUser.value = apiClient.getCurrentUser().toDomain()
        _readingRecords.value = apiClient.listReadingRecords().map { it.toDomain() }
        _notes.value = apiClient.listJournalNotes().map { it.toDomain() }
        _feed.value = apiClient.listFeed(feedFilter.toApi(), feedSort.toApi()).items.map { it.toDomain() }
        cachedProgress = apiClient.getReadingProgress().toDomain()
        cachedStats = apiClient.getReadingStats().toDomain()
        ensureAiConversation()
        aiConversationId?.let { id ->
            _aiMessages.value = apiClient.listAiMessages(id).map { it.toDomain() }
        }
    }

    override fun getReadingProgress(): ReadingProgress = cachedProgress

    override fun getReadingStats(): ReadingStats = cachedStats

    override fun getReadingDates(): Set<LocalDate> =
        _readingRecords.value.map { it.date }.toSet()

    override suspend fun addReadingRecord(
        reference: BibleReference,
        minutesRead: Int,
        date: LocalDate,
    ): Result<Unit> = runCatching {
        apiClient.createReadingRecord(
            ApiCreateReadingRecordRequestDto(
                date = date.toString(),
                reference = reference.toDto(),
                minutesRead = minutesRead,
            ),
        )
        refreshReading()
    }

    override suspend fun saveNote(
        content: String,
        prayerTopic: String?,
        emotion: Emotion?,
        reference: BibleReference?,
        visibility: NoteVisibility,
        noteId: String?,
    ): Result<MeditationNote> = runCatching {
        val body = ApiUpsertJournalNoteRequestDto(
            content = content,
            prayerTopic = prayerTopic,
            emotion = emotion?.toApi(),
            reference = reference?.toDto(),
            visibility = visibility.toApi(),
        )
        val note = if (noteId == null) {
            apiClient.createJournalNote(body)
        } else {
            apiClient.updateJournalNote(noteId, body)
        }.toDomain()
        _notes.value = apiClient.listJournalNotes().map { it.toDomain() }
        _feed.value = apiClient.listFeed().items.map { it.toDomain() }
        note
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        apiClient.deleteJournalNote(noteId)
        _notes.value = apiClient.listJournalNotes().map { it.toDomain() }
        _feed.value = apiClient.listFeed().items.map { it.toDomain() }
    }

    override suspend fun addComment(noteId: String, content: String): Result<Comment> =
        Result.failure(UnsupportedOperationException("댓글 API는 다음 단계에서 연결됩니다."))

    override suspend fun loadFeed(
        filter: FeedFilter,
        sort: FeedSort,
    ): Result<Unit> = runCatching {
        feedFilter = filter
        feedSort = sort
        refreshFeed()
    }

    override suspend fun toggleReaction(noteId: String, reaction: FaithReaction): Result<Unit> =
        runCatching {
            apiClient.toggleReaction(
                noteId,
                com.example.biblelog.data.remote.ApiToggleReactionRequestDto(reaction.toApi()),
            )
            refreshFeed()
        }

    override suspend fun sendAiMessage(
        content: String,
        mode: AiConversationMode,
    ): Result<AiMessage> = runCatching {
        ensureAiConversation()
        val conversationId = aiConversationId ?: error("AI conversation unavailable")
        val response = apiClient.sendAiMessage(
            conversationId,
            ApiSendAiMessageRequestDto(content = content, mode = mode.toApi()),
        )
        _aiMessages.value = apiClient.listAiMessages(conversationId).map { it.toDomain() }
        response.assistantMessage.toDomain()
    }

    override suspend fun updateProfile(nickname: String, bio: String): Result<Unit> = runCatching {
        _currentUser.value = apiClient.updateCurrentUser(nickname, bio).toDomain()
    }

    private suspend fun refreshReading() {
        _readingRecords.value = apiClient.listReadingRecords().map { it.toDomain() }
        cachedProgress = apiClient.getReadingProgress().toDomain()
        cachedStats = apiClient.getReadingStats().toDomain()
    }

    private suspend fun refreshFeed() {
        _feed.value = apiClient.listFeed(feedFilter.toApi(), feedSort.toApi()).items.map { it.toDomain() }
    }

    private suspend fun ensureAiConversation() {
        if (aiConversationId != null) return
        aiConversationId = runCatching {
            apiClient.listAiConversations().firstOrNull()?.id
                ?: apiClient.createAiConversation().id
        }.getOrNull()
    }
}

private fun FaithReaction.toApi(): String = when (this) {
    FaithReaction.EMPATHY -> "empathy"
    FaithReaction.PRAY_TOGETHER -> "pray_together"
    FaithReaction.AMEN -> "amen"
    FaithReaction.GRACE -> "grace"
}

private fun FeedFilter.toApi(): String = when (this) {
    FeedFilter.ALL -> "all"
    FeedFilter.SMALL_GROUP -> "small_group"
    FeedFilter.CHURCH -> "church"
    FeedFilter.FRIENDS -> "friends"
}

private fun FeedSort.toApi(): String = when (this) {
    FeedSort.LATEST -> "latest"
    FeedSort.POPULAR -> "popular"
}

private fun AiConversationMode.toApi(): String = when (this) {
    AiConversationMode.CHAT -> "chat"
    AiConversationMode.PRAYER -> "prayer"
}

private fun ApiReadingRecordDto.toDomain() = ReadingRecord(
    id = id,
    date = LocalDate.parse(date),
    reference = reference.toDomain(),
    minutesRead = minutesRead,
    createdAt = createdAt.toInstant(),
)

private fun ApiBibleReferenceDto.toDomain() = BibleReference(
    bookId = bookId,
    startChapter = startChapter,
    startVerse = startVerse,
    endChapter = endChapter,
    endVerse = endVerse,
)

private fun BibleReference.toDto() = ApiBibleReferenceDto(
    bookId = bookId,
    startChapter = startChapter,
    startVerse = startVerse,
    endChapter = endChapter,
    endVerse = endVerse,
)

private fun ApiReadingProgressDto.toDomain() = ReadingProgress(
    overall = overall,
    oldTestament = oldTestament,
    newTestament = newTestament,
    byBook = byBook.mapKeys { it.key.toIntOrNull() ?: 0 },
)

private fun ApiReadingStatsDto.toDomain() = ReadingStats(
    totalMinutes = totalMinutes,
    averageDailyMinutes = averageDailyMinutes,
    monthlyReadingDays = monthlyReadingDays,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)

private fun ApiMeditationNoteDto.toDomain() = MeditationNote(
    id = id,
    content = content,
    prayerTopic = prayerTopic,
    emotion = emotion?.toEmotion(),
    reference = reference?.toDomain(),
    visibility = visibility.toVisibility(),
    authorId = authorId,
    authorName = authorName,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

private fun ApiFeedItemDto.toDomain() = FeedItem(
    note = note.toDomain(),
    reactions = reactions.map {
        ReactionCount(
            type = it.type.toReaction(),
            count = it.count,
            reactedByMe = it.reactedByMe,
        )
    },
    commentCount = commentCount,
)

private fun com.example.biblelog.data.remote.ApiAiMessageDto.toDomain() = AiMessage(
    id = id,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp.toInstant(),
    suggestedVerse = suggestedReference?.toDomain(),
)

private fun com.example.biblelog.data.remote.ApiUserProfileDto.toDomain() = UserProfile(
    id = id,
    nickname = nickname,
    bio = bio,
    isLoggedIn = isLoggedIn,
)

private fun Emotion.toApi(): String = when (this) {
    Emotion.GRATITUDE -> "gratitude"
    Emotion.JOY -> "joy"
    Emotion.PEACE -> "peace"
    Emotion.SADNESS -> "sadness"
    Emotion.MOVED -> "moved"
}

private fun String.toEmotion(): Emotion? = when (this) {
    "gratitude" -> Emotion.GRATITUDE
    "joy" -> Emotion.JOY
    "peace" -> Emotion.PEACE
    "sadness" -> Emotion.SADNESS
    "moved" -> Emotion.MOVED
    else -> null
}

private fun NoteVisibility.toApi(): String = when (this) {
    NoteVisibility.PUBLIC -> "public"
    NoteVisibility.FRIENDS -> "friends"
    NoteVisibility.SMALL_GROUP -> "small_group"
    NoteVisibility.CHURCH -> "church"
    NoteVisibility.PRIVATE -> "private"
}

private fun String.toVisibility(): NoteVisibility = when (this) {
    "public" -> NoteVisibility.PUBLIC
    "friends" -> NoteVisibility.FRIENDS
    "small_group" -> NoteVisibility.SMALL_GROUP
    "church" -> NoteVisibility.CHURCH
    else -> NoteVisibility.PRIVATE
}

private fun String.toReaction(): FaithReaction = when (this) {
    "empathy" -> FaithReaction.EMPATHY
    "pray_together" -> FaithReaction.PRAY_TOGETHER
    "amen" -> FaithReaction.AMEN
    else -> FaithReaction.GRACE
}

private fun String.toInstant(): Instant {
    val normalized = when {
        endsWith('Z') -> this
        length > 10 && this[10] == 'T' && (substringAfter('T').contains('+') || substringAfter('T').contains('-')) -> this
        else -> "${this}Z"
    }
    return Instant.parse(normalized)
}
