package com.example.biblelog.data

import com.example.biblelog.data.mapper.toApi
import com.example.biblelog.data.mapper.toDomain
import com.example.biblelog.data.remote.ApiCreateCommentRequestDto
import com.example.biblelog.data.mapper.toDto
import com.example.biblelog.data.remote.ApiCreateReadingRecordRequestDto
import com.example.biblelog.data.remote.ApiSendAiMessageRequestDto
import com.example.biblelog.data.remote.ApiToggleReactionRequestDto
import com.example.biblelog.data.remote.ApiUpsertJournalNoteRequestDto
import com.example.biblelog.data.remote.BibleLogApiClient
import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.Comment
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.FeedSort
import com.example.biblelog.domain.model.FollowRequest
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserMemberships
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.model.UserSearchResult
import com.example.biblelog.domain.repository.BibleLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class ApiBackedBibleLogRepository(
    private val apiClient: BibleLogApiClient,
) : BibleLogRepository {

    private val _currentUser = MutableStateFlow(UserProfile("", "", "", "", ProfileVisibility.PUBLIC, false))
    private val _readingRecords = MutableStateFlow<List<ReadingRecord>>(emptyList())
    private val _readingProgress = MutableStateFlow(ReadingProgress(0f, 0f, 0f, emptyMap()))
    private val _readingStats = MutableStateFlow(ReadingStats(0, 0f, emptyMap(), 0, 0))
    private val _notes = MutableStateFlow<List<MeditationNote>>(emptyList())
    private val _feed = MutableStateFlow<List<FeedItem>>(emptyList())
    private val _feedHasMore = MutableStateFlow(false)
    private val _feedNextCursor = MutableStateFlow<String?>(null)
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())

    private var aiConversationId: String? = null
    private var feedFilter = FeedFilter.ALL
    private var feedSort = FeedSort.LATEST

    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()
    override val readingRecords: StateFlow<List<ReadingRecord>> = _readingRecords.asStateFlow()
    override val readingProgress: StateFlow<ReadingProgress> = _readingProgress.asStateFlow()
    override val readingStats: StateFlow<ReadingStats> = _readingStats.asStateFlow()
    override val notes: StateFlow<List<MeditationNote>> = _notes.asStateFlow()
    override val feed: StateFlow<List<FeedItem>> = _feed.asStateFlow()
    override val feedHasMore: StateFlow<Boolean> = _feedHasMore.asStateFlow()
    override val feedNextCursor: StateFlow<String?> = _feedNextCursor.asStateFlow()
    override val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    override suspend fun refreshAll() {
        _currentUser.value = apiClient.getCurrentUser().toDomain()
        _readingRecords.value = apiClient.listReadingRecords().map { it.toDomain() }
        _notes.value = apiClient.listJournalNotes().map { it.toDomain() }
        loadFeed(feedFilter, feedSort, reset = true).getOrThrow()
        _readingProgress.value = apiClient.getReadingProgress().toDomain()
        _readingStats.value = apiClient.getReadingStats().toDomain()
        ensureAiConversation()
        aiConversationId?.let { id ->
            _aiMessages.value = apiClient.listAiMessages(id).map { it.toDomain() }
        }
        _notifications.value = apiClient.listNotifications().items.map { it.toDomain() }
    }

    override fun clearState() {
        _currentUser.value = UserProfile("", "", "", "", ProfileVisibility.PUBLIC, false)
        _readingRecords.value = emptyList()
        _readingProgress.value = ReadingProgress(0f, 0f, 0f, emptyMap())
        _readingStats.value = ReadingStats(0, 0f, emptyMap(), 0, 0)
        _notes.value = emptyList()
        _feed.value = emptyList()
        _feedHasMore.value = false
        _feedNextCursor.value = null
        _aiMessages.value = emptyList()
        _notifications.value = emptyList()
        aiConversationId = null
        feedFilter = FeedFilter.ALL
        feedSort = FeedSort.LATEST
    }

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
        refreshFeed(reset = true)
        note
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        apiClient.deleteJournalNote(noteId)
        _notes.value = apiClient.listJournalNotes().map { it.toDomain() }
        refreshFeed(reset = true)
    }

    override suspend fun addComment(noteId: String, content: String): Result<Comment> = runCatching {
        val comment = apiClient.createComment(noteId, ApiCreateCommentRequestDto(content)).toDomain()
        refreshFeed(reset = true)
        comment
    }

    override suspend fun loadFeed(
        filter: FeedFilter,
        sort: FeedSort,
        reset: Boolean,
    ): Result<Unit> = runCatching {
        if (reset) {
            feedFilter = filter
            feedSort = sort
            refreshFeed(reset = true)
        } else {
            refreshFeed(reset = false)
        }
    }

    override suspend fun toggleReaction(noteId: String, reaction: FaithReaction): Result<Unit> =
        runCatching {
            apiClient.toggleReaction(
                noteId,
                ApiToggleReactionRequestDto(reaction.toApi()),
            )
            refreshFeed(reset = true)
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

    override suspend fun updateProfile(
        nickname: String?,
        bio: String?,
        photoUrl: String?,
        profileVisibility: ProfileVisibility?,
    ): Result<Unit> = runCatching {
        _currentUser.value = apiClient.updateCurrentUser(
            nickname = nickname,
            bio = bio,
            photoUrl = photoUrl,
            profileVisibility = profileVisibility?.toApi(),
        ).toDomain()
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile> = runCatching {
        apiClient.getUserProfile(userId).toDomain()
    }

    override suspend fun getUserNotes(userId: String): Result<List<MeditationNote>> = runCatching {
        apiClient.listUserNotes(userId).map { it.toDomain() }
    }

    override suspend fun searchUsers(query: String): Result<List<UserSearchResult>> = runCatching {
        apiClient.searchUsers(query).map { it.toDomain() }
    }

    override suspend fun searchChurches(query: String): Result<List<ChurchSummary>> = runCatching {
        apiClient.searchChurches(query).map { it.toDomain() }
    }

    override suspend fun searchSmallGroups(query: String): Result<List<SmallGroupSummary>> = runCatching {
        apiClient.searchSmallGroups(query).map { it.toDomain() }
    }

    override suspend fun sendFriendRequest(userId: String): Result<Unit> = runCatching {
        apiClient.sendFriendRequest(userId)
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = runCatching {
        apiClient.acceptFriendRequest(requestId)
    }

    override suspend fun followUser(userId: String): Result<Unit> = runCatching {
        apiClient.followUser(userId)
    }

    override suspend fun acceptFollowRequest(requestId: String): Result<Unit> = runCatching {
        apiClient.acceptFollowRequest(requestId)
    }

    override suspend fun rejectFollowRequest(requestId: String): Result<Unit> = runCatching {
        apiClient.rejectFollowRequest(requestId)
    }

    override suspend fun listIncomingFollowRequests(): Result<List<FollowRequest>> = runCatching {
        apiClient.listIncomingFollowRequests().map { it.toDomain() }
    }

    override suspend fun joinChurch(churchId: String): Result<Unit> = runCatching {
        apiClient.joinChurch(churchId)
    }

    override suspend fun joinSmallGroup(groupId: String): Result<Unit> = runCatching {
        apiClient.joinSmallGroup(groupId)
    }

    override suspend fun getFriendIds(): Result<Set<String>> = runCatching {
        apiClient.listFriends().map { it.id }.toSet()
    }

    override suspend fun getFollowingIds(): Result<Set<String>> = runCatching {
        apiClient.listFollowing().map { it.id }.toSet()
    }

    override suspend fun getMemberships(): Result<UserMemberships> = runCatching {
        apiClient.getMemberships().toDomain()
    }

    private suspend fun refreshReading() {
        _readingRecords.value = apiClient.listReadingRecords().map { it.toDomain() }
        _readingProgress.value = apiClient.getReadingProgress().toDomain()
        _readingStats.value = apiClient.getReadingStats().toDomain()
    }

    private suspend fun refreshFeed(reset: Boolean) {
        val cursor = if (reset) null else _feedNextCursor.value
        val page = apiClient.listFeed(feedFilter.toApi(), feedSort.toApi(), cursor)
        val items = page.items.map { it.toDomain() }
        _feed.value = if (reset) items else _feed.value + items
        _feedNextCursor.value = page.nextCursor
        _feedHasMore.value = page.hasMore
    }

    private suspend fun ensureAiConversation() {
        if (aiConversationId != null) return
        aiConversationId = runCatching {
            apiClient.listAiConversations().firstOrNull()?.id
                ?: apiClient.createAiConversation().id
        }.getOrNull()
    }
}
