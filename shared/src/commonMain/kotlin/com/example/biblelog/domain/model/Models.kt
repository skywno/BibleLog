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
    val endBookId: Int = bookId,
    val endChapter: Int,
    val endVerse: Int,
) {
    fun displayName(books: Map<Int, BibleBook>): String {
        val startBook = books[bookId] ?: return "알 수 없음"
        val endBook = books[endBookId] ?: startBook
        val sameBook = bookId == endBookId
        return when {
            sameBook && startChapter == endChapter && startVerse == endVerse ->
                "${startBook.nameKo} ${startChapter}:${startVerse}"
            sameBook && startChapter == endChapter ->
                "${startBook.nameKo} ${startChapter}:${startVerse}-${endVerse}"
            sameBook ->
                "${startBook.nameKo} ${startChapter}:${startVerse} ~ ${endChapter}:${endVerse}"
            startChapter == endChapter && startVerse == endVerse ->
                "${startBook.nameKo} ${startChapter}:${startVerse} ~ ${endBook.nameKo} ${endChapter}:${endVerse}"
            else ->
                "${startBook.nameKo} ${startChapter}:${startVerse} ~ ${endBook.nameKo} ${endChapter}:${endVerse}"
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

enum class ProfileVisibility {
    PUBLIC,
    PRIVATE,
}

data class UserProfile(
    val id: String,
    val nickname: String,
    val bio: String,
    val photoUrl: String = "",
    val profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
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
    val eventType: String,
    val payload: Map<String, String>,
    val isRead: Boolean,
    val createdAt: Instant,
) {
    val title: String
        get() = when (eventType) {
            "friend_request" -> "친구 요청"
            "friend_accepted" -> "친구 수락"
            "follow_request" -> "팔로우 요청"
            "follow_request_accepted" -> "팔로우 수락"
            "follow" -> "새 팔로워"
            "note_published" -> "새 묵상"
            "comment_created" -> "새 댓글"
            "reaction_toggled" -> "새 반응"
            else -> "알림"
        }

    val body: String
        get() = when (eventType) {
            "friend_request" -> "친구 요청이 도착했습니다."
            "friend_accepted" -> "친구 요청이 수락되었습니다."
            "follow_request" -> "팔로우 요청이 도착했습니다."
            "follow_request_accepted" -> "팔로우 요청이 수락되었습니다."
            "follow" -> "새로운 팔로워가 있습니다."
            else -> payload["note_id"]?.let { "새 활동이 있습니다." } ?: "새 알림이 있습니다."
        }

    val actionRequestId: String?
        get() = payload["request_id"]
}

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
    val photoUrl: String = "",
)

data class FollowRequest(
    val id: String,
    val fromUserId: String,
    val fromUserNickname: String,
    val toUserId: String,
    val status: String,
    val createdAt: Instant,
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
