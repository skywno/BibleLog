package com.example.biblelog.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

enum class Testament {
    OLD,
    NEW,
}

enum class NoteVisibility {
    PUBLIC,
    FRIENDS,
    SMALL_GROUP,
    CHURCH,
    PRIVATE,
}

enum class Emotion {
    GRATITUDE,
    JOY,
    PEACE,
    SADNESS,
    MOVED,
}

enum class FaithReaction {
    EMPATHY,
    PRAY_TOGETHER,
    AMEN,
    GRACE,
}

enum class FeedFilter {
    ALL,
    SMALL_GROUP,
    CHURCH,
    FRIENDS,
    FOLLOWING,
}

enum class FeedSort {
    LATEST,
    POPULAR,
}

enum class AiConversationMode {
    CHAT,
    PRAYER,
}

data class BibleBook(
    val id: Int,
    val nameKo: String,
    val nameEn: String,
    val testament: Testament,
    val totalChapters: Int,
    val totalVerses: Int,
)

data class BibleReference(
    val bookId: Int,
    val startChapter: Int,
    val startVerse: Int,
    val endChapter: Int,
    val endVerse: Int,
) {
    fun displayName(books: Map<Int, BibleBook>): String {
        val book = books[bookId] ?: return "알 수 없음"
        return if (startChapter == endChapter && startVerse == endVerse) {
            "${book.nameKo} ${startChapter}:${startVerse}"
        } else if (startChapter == endChapter) {
            "${book.nameKo} ${startChapter}:${startVerse}-${endVerse}"
        } else {
            "${book.nameKo} ${startChapter}:${startVerse} ~ ${endChapter}:${endVerse}"
        }
    }
}

data class ReadingRecord(
    val id: String,
    val date: LocalDate,
    val reference: BibleReference,
    val minutesRead: Int,
    val createdAt: Instant,
)

data class ReadingProgress(
    val overall: Float,
    val oldTestament: Float,
    val newTestament: Float,
    val byBook: Map<Int, Float>,
)

data class MeditationNote(
    val id: String,
    val content: String,
    val prayerTopic: String?,
    val emotion: Emotion?,
    val reference: BibleReference?,
    val visibility: NoteVisibility,
    val authorId: String,
    val authorName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Comment(
    val id: String,
    val noteId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
)

data class ReactionCount(
    val type: FaithReaction,
    val count: Int,
    val reactedByMe: Boolean,
)

data class FeedItem(
    val note: MeditationNote,
    val reactions: List<ReactionCount>,
    val commentCount: Int,
)

data class UserProfile(
    val id: String,
    val nickname: String,
    val bio: String,
    val isLoggedIn: Boolean,
)

data class AiMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Instant,
    val suggestedVerse: BibleReference? = null,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: Instant,
)

data class ReadingStats(
    val totalMinutes: Int,
    val averageDailyMinutes: Float,
    val monthlyReadingDays: Map<Int, Int>,
    val currentStreak: Int,
    val bestStreak: Int,
)

enum class CommunitySearchCategory {
    USERS,
    CHURCHES,
    SMALL_GROUPS,
}

data class UserSearchResult(
    val id: String,
    val nickname: String,
    val bio: String,
)

data class ChurchSummary(
    val id: String,
    val name: String,
    val description: String,
)

data class SmallGroupSummary(
    val id: String,
    val name: String,
    val churchId: String?,
)

data class UserMemberships(
    val churchId: String?,
    val groupIds: Set<String>,
)
