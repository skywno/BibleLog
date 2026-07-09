package com.example.biblelog.data

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.biblelog.util.currentInstant
import com.example.biblelog.util.today
import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InMemoryBibleLogRepository : BibleLogRepository {

    private val currentUserId = "user-1"

    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = currentUserId,
            nickname = "김신앙",
            bio = "매일 말씀과 함께하는 직장인",
            isLoggedIn = true,
        ),
    )
    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _readingRecords = MutableStateFlow(createSampleReadingRecords())
    override val readingRecords: StateFlow<List<ReadingRecord>> = _readingRecords.asStateFlow()

    private val _notes = MutableStateFlow(createSampleNotes())
    override val notes: StateFlow<List<MeditationNote>> = _notes.asStateFlow()

    private val _feed = MutableStateFlow(createSampleFeed())
    override val feed: StateFlow<List<FeedItem>> = _feed.asStateFlow()

    private val _aiMessages = MutableStateFlow(createSampleAiMessages())
    override val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _notifications = MutableStateFlow(createSampleNotifications())
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val reactions = mutableMapOf<String, MutableMap<FaithReaction, MutableSet<String>>>()
    private val comments = mutableMapOf<String, MutableList<Comment>>()

    init {
        reactions["note-1"] = mutableMapOf(
            FaithReaction.EMPATHY to mutableSetOf("user-2", "user-3"),
            FaithReaction.PRAY_TOGETHER to mutableSetOf("user-2"),
            FaithReaction.AMEN to mutableSetOf("user-4"),
            FaithReaction.GRACE to mutableSetOf(),
        )
        comments["note-1"] = mutableListOf(
            Comment(
                id = "comment-1",
                noteId = "note-1",
                authorId = "user-2",
                authorName = "박은혜",
                content = "오늘 묵상 정말 은혜로웠어요. 함께 기도합니다!",
                createdAt = Instant.parse("2026-07-05T03:30:00Z"),
            ),
        )
    }

    override fun getReadingProgress(): ReadingProgress {
        val readVersesByBook = calculateReadVersesByBook()
        val totalRead = readVersesByBook.values.sum()
        val oldRead = readVersesByBook.filterKeys { (it <= 39) }.values.sum()
        val newRead = readVersesByBook.filterKeys { (it >= 40) }.values.sum()

        return ReadingProgress(
            overall = totalRead.toFloat() / BibleCatalog.totalVerses,
            oldTestament = oldRead.toFloat() / BibleCatalog.oldTestamentVerses,
            newTestament = newRead.toFloat() / BibleCatalog.newTestamentVerses,
            byBook = BibleCatalog.books.associate { book ->
                val read = readVersesByBook[book.id] ?: 0
                book.id to read.toFloat() / book.totalVerses
            },
        )
    }

    override fun getReadingStats(): ReadingStats {
        val records = _readingRecords.value
        val dates = records.map { it.date }.toSet()
        val totalMinutes = records.sumOf { it.minutesRead }
        val streak = calculateCurrentStreak(dates)
        val monthlyDays = dates.groupBy { it.monthNumber }.mapValues { (_, dateList) -> dateList.size }

        return ReadingStats(
            totalMinutes = totalMinutes,
            averageDailyMinutes = if (dates.isEmpty()) 0f else totalMinutes.toFloat() / dates.size,
            monthlyReadingDays = monthlyDays,
            currentStreak = streak,
            bestStreak = maxOf(streak, 14),
        )
    }

    override fun getReadingDates(): Set<LocalDate> =
        _readingRecords.value.map { it.date }.toSet()

    override suspend fun addReadingRecord(
        reference: BibleReference,
        minutesRead: Int,
        date: LocalDate,
    ): Result<Unit> {
        val duplicate = _readingRecords.value.any {
            it.date == date &&
                it.reference.bookId == reference.bookId &&
                it.reference.startChapter == reference.startChapter &&
                it.reference.startVerse == reference.startVerse &&
                it.reference.endChapter == reference.endChapter &&
                it.reference.endVerse == reference.endVerse
        }
        if (duplicate) {
            return Result.failure(IllegalStateException("동일한 날짜에 같은 범위의 기록이 이미 있습니다."))
        }

        val record = ReadingRecord(
            id = Uuid.random().toString(),
            date = date,
            reference = reference,
            minutesRead = minutesRead,
            createdAt = currentInstant(),
        )
        _readingRecords.update { it + record }
        return Result.success(Unit)
    }

    override suspend fun saveNote(
        content: String,
        prayerTopic: String?,
        emotion: Emotion?,
        reference: BibleReference?,
        visibility: NoteVisibility,
        noteId: String?,
    ): Result<MeditationNote> {
        val now = currentInstant()
        val note = if (noteId != null) {
            val existing = _notes.value.find { it.id == noteId }
                ?: return Result.failure(NoSuchElementException("노트를 찾을 수 없습니다."))
            existing.copy(
                content = content,
                prayerTopic = prayerTopic,
                emotion = emotion,
                reference = reference,
                visibility = visibility,
                updatedAt = now,
            )
        } else {
            MeditationNote(
                id = Uuid.random().toString(),
                content = content,
                prayerTopic = prayerTopic,
                emotion = emotion,
                reference = reference,
                visibility = visibility,
                authorId = currentUserId,
                authorName = _currentUser.value.nickname,
                createdAt = now,
                updatedAt = now,
            )
        }

        _notes.update { list ->
            if (noteId != null) list.map { if (it.id == noteId) note else it }
            else list + note
        }

        if (visibility != NoteVisibility.PRIVATE) {
            refreshFeed()
        }
        return Result.success(note)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        _notes.update { it.filterNot { note -> note.id == noteId } }
        _feed.update { it.filterNot { item -> item.note.id == noteId } }
        return Result.success(Unit)
    }

    override suspend fun addComment(noteId: String, content: String): Result<Comment> {
        val comment = Comment(
            id = Uuid.random().toString(),
            noteId = noteId,
            authorId = currentUserId,
            authorName = _currentUser.value.nickname,
            content = content,
            createdAt = currentInstant(),
        )
        comments.getOrPut(noteId) { mutableListOf() }.add(comment)
        refreshFeed()
        return Result.success(comment)
    }

    override suspend fun loadFeed(filter: FeedFilter, sort: FeedSort): Result<Unit> {
        refreshFeed(filter, sort)
        return Result.success(Unit)
    }

    override suspend fun toggleReaction(noteId: String, reaction: FaithReaction): Result<Unit> {
        val noteReactions = reactions.getOrPut(noteId) { mutableMapOf() }
        val users = noteReactions.getOrPut(reaction) { mutableSetOf() }
        if (users.contains(currentUserId)) users.remove(currentUserId) else users.add(currentUserId)
        refreshFeed()
        return Result.success(Unit)
    }

    override suspend fun sendAiMessage(
        content: String,
        mode: AiConversationMode,
    ): Result<AiMessage> {
        val userMessage = AiMessage(
            id = Uuid.random().toString(),
            content = content,
            isFromUser = true,
            timestamp = currentInstant(),
        )
        _aiMessages.update { it + userMessage }

        val aiResponse = AiMessage(
            id = Uuid.random().toString(),
            content = buildAiResponse(content, mode),
            isFromUser = false,
            timestamp = currentInstant(),
            suggestedVerse = BibleReference(43, 3, 16, 3, 16),
        )
        _aiMessages.update { it + aiResponse }
        return Result.success(aiResponse)
    }

    override suspend fun updateProfile(nickname: String, bio: String): Result<Unit> {
        _currentUser.update { it.copy(nickname = nickname, bio = bio) }
        return Result.success(Unit)
    }

    private fun refreshFeed(
        filter: FeedFilter = FeedFilter.ALL,
        sort: FeedSort = FeedSort.LATEST,
    ) {
        val filtered = _notes.value
            .filter { it.visibility != NoteVisibility.PRIVATE }
            .filter { note ->
                when (filter) {
                    FeedFilter.ALL -> true
                    FeedFilter.SMALL_GROUP -> note.visibility == NoteVisibility.SMALL_GROUP
                    FeedFilter.CHURCH -> note.visibility == NoteVisibility.CHURCH
                    FeedFilter.FRIENDS -> note.visibility == NoteVisibility.FRIENDS
                }
            }
        val sorted = when (sort) {
            FeedSort.LATEST -> filtered.sortedByDescending { it.createdAt }
            FeedSort.POPULAR -> filtered.sortedByDescending { note ->
                reactions[note.id]?.values?.sumOf { it.size } ?: 0
            }
        }
        _feed.value = sorted.map { note ->
            FeedItem(
                note = note,
                reactions = FaithReaction.entries.map { type ->
                    val users = reactions[note.id]?.get(type).orEmpty()
                    ReactionCount(type, users.size, currentUserId in users)
                },
                commentCount = comments[note.id]?.size ?: 0,
            )
        }
    }

    private fun calculateReadVersesByBook(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        _readingRecords.value.forEach { record ->
            val book = BibleCatalog.bookMap[record.reference.bookId] ?: return@forEach
            val estimatedVerses = estimateVersesRead(record.reference, book.totalChapters, book.totalVerses)
            result[record.reference.bookId] = (result[record.reference.bookId] ?: 0) + estimatedVerses
        }
        return result.mapValues { (bookId, verses) ->
            verses.coerceAtMost(BibleCatalog.bookMap[bookId]?.totalVerses ?: verses)
        }
    }

    private fun estimateVersesRead(
        reference: BibleReference,
        totalChapters: Int,
        totalVerses: Int,
    ): Int {
        val avgVersesPerChapter = totalVerses.toFloat() / totalChapters
        val chaptersRead = if (reference.startChapter == reference.endChapter) {
            1
        } else {
            reference.endChapter - reference.startChapter + 1
        }
        return (chaptersRead * avgVersesPerChapter).toInt().coerceAtLeast(1)
    }

    private fun calculateCurrentStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        var date = today()
        while (dates.contains(date)) {
            streak++
            date = date.minus(DatePeriod(days = 1))
        }
        return streak
    }

    private fun buildAiResponse(userInput: String, mode: AiConversationMode): String = when {
        mode == AiConversationMode.PRAYER ->
            "함께 기도하겠습니다. 주님, 지금 이 자리의 마음을 아시는 주님께 올려 드립니다. " +
                "평안과 위로와 인도하심을 허락해 주세요. 아멘."

        userInput.contains("슬프") || userInput.contains("힘들") ->
            "마음이 무거우시군요. 시편 34:18처럼, 주님은 상한 마음을 가까이 하십니다. " +
                "지금 느끼시는 감정을 있는 그대로 주님께 가져가셔도 괜찮아요. 함께 기도드릴까요?"

        userInput.contains("감사") ->
            "감사의 마음을 나눠 주셔서 기쁩니다. 데살로니가전서 5:18처럼, " +
                "모든 상황 가운데 감사하는 마음은 영적 성장의 아름다운 열매입니다."

        else ->
            "말씀 나눠 주셔서 감사합니다. 오늘 묵상하신 내용을 바탕으로, " +
                "주님께서는 당신의 하루 가운데 함께하고 계십니다. " +
                "더 깊이 나누고 싶은 부분이 있으시면 편하게 말씀해 주세요."
    }

    private fun createSampleReadingRecords(): List<ReadingRecord> {
        val today = today()
        return (0 until 14).map { daysAgo ->
            val date = today.minus(DatePeriod(days = daysAgo))
            ReadingRecord(
                id = "reading-$daysAgo",
                date = date,
                reference = BibleReference(
                    bookId = 1 + (daysAgo % 5),
                    startChapter = 1 + daysAgo,
                    startVerse = 1,
                    endChapter = 1 + daysAgo,
                    endVerse = 20,
                ),
                minutesRead = 10 + Random.nextInt(20),
                createdAt = Instant.parse("2026-07-0${5 - (daysAgo % 5)}T06:00:00Z"),
            )
        }
    }

    private fun createSampleNotes(): List<MeditationNote> = listOf(
        MeditationNote(
            id = "note-1",
            content = "하나님께서 세상을 이처럼 사랑하사 독생자를 주셨다는 말씀이 오늘 새롭게 다가왔습니다.",
            prayerTopic = "가족의 건강",
            emotion = Emotion.MOVED,
            reference = BibleReference(43, 3, 16, 3, 16),
            visibility = NoteVisibility.SMALL_GROUP,
            authorId = currentUserId,
            authorName = "김신앙",
            createdAt = Instant.parse("2026-07-05T01:00:00Z"),
            updatedAt = Instant.parse("2026-07-05T01:00:00Z"),
        ),
        MeditationNote(
            id = "note-2",
            content = "출근길 지하철에서 읽은 시편 23편, 주님이 나의 목자 되심을 다시 확인했습니다.",
            prayerTopic = null,
            emotion = Emotion.PEACE,
            reference = BibleReference(19, 23, 1, 23, 6),
            visibility = NoteVisibility.PRIVATE,
            authorId = currentUserId,
            authorName = "김신앙",
            createdAt = Instant.parse("2026-07-04T01:00:00Z"),
            updatedAt = Instant.parse("2026-07-04T01:00:00Z"),
        ),
    )

    private fun createSampleFeed(): List<FeedItem> = listOf(
        FeedItem(
            note = MeditationNote(
                id = "feed-1",
                content = "오늘 소그룹 말씀 나눔 중 '서로 사랑하라'는 요청이 마음에 남았습니다.",
                prayerTopic = "소그룹의 연합",
                emotion = Emotion.JOY,
                reference = BibleReference(43, 13, 34, 13, 35),
                visibility = NoteVisibility.SMALL_GROUP,
                authorId = "user-2",
                authorName = "박은혜",
                createdAt = Instant.parse("2026-07-05T04:00:00Z"),
                updatedAt = Instant.parse("2026-07-05T04:00:00Z"),
            ),
            reactions = listOf(
                ReactionCount(FaithReaction.EMPATHY, 3, true),
                ReactionCount(FaithReaction.PRAY_TOGETHER, 2, false),
                ReactionCount(FaithReaction.AMEN, 5, true),
                ReactionCount(FaithReaction.GRACE, 1, false),
            ),
            commentCount = 2,
        ),
    )

    private fun createSampleAiMessages(): List<AiMessage> = listOf(
        AiMessage(
            id = "ai-1",
            content = "안녕하세요, 김신앙님. 오늘 말씀 묵상은 어떠셨나요?",
            isFromUser = false,
            timestamp = Instant.parse("2026-07-05T10:00:00Z"),
        ),
    )

    private fun createSampleNotifications(): List<NotificationItem> = listOf(
        NotificationItem(
            id = "notif-1",
            title = "박은혜님이 공감을 남겼습니다",
            body = "오늘 묵상 노트에 공감했습니다.",
            isRead = false,
            createdAt = Instant.parse("2026-07-05T05:00:00Z"),
        ),
        NotificationItem(
            id = "notif-2",
            title = "새 댓글이 달렸습니다",
            body = "박은혜님: 함께 기도합니다!",
            isRead = true,
            createdAt = Instant.parse("2026-07-05T03:30:00Z"),
        ),
    )
}
