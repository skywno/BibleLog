package com.example.biblelog.data.mapper

import com.example.biblelog.data.remote.ApiCommentDto
import com.example.biblelog.data.remote.ApiAiMessageDto
import com.example.biblelog.data.remote.ApiBibleReferenceDto
import com.example.biblelog.data.remote.ApiChurchDto
import com.example.biblelog.data.remote.ApiFeedItemDto
import com.example.biblelog.data.remote.ApiMeditationNoteDto
import com.example.biblelog.data.remote.ApiFollowRequestDto
import com.example.biblelog.data.remote.ApiNotificationItemDto
import com.example.biblelog.data.remote.ApiReadingProgressDto
import com.example.biblelog.data.remote.ApiReadingRecordDto
import com.example.biblelog.data.remote.ApiReadingStatsDto
import com.example.biblelog.data.remote.ApiSmallGroupDto
import com.example.biblelog.data.remote.ApiUserMembershipsDto
import com.example.biblelog.data.remote.ApiUserProfileDto
import com.example.biblelog.data.remote.ApiUserSearchResultDto
import com.example.biblelog.domain.model.AiConversationMode
import com.example.biblelog.domain.model.AiMessage
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedItem
import com.example.biblelog.domain.model.FollowRequest
import com.example.biblelog.domain.model.NotificationItem
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.model.ReadingProgress
import com.example.biblelog.domain.model.ReadingRecord
import com.example.biblelog.domain.model.ReadingStats
import com.example.biblelog.domain.model.ReactionCount
import com.example.biblelog.domain.model.Comment
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserMemberships
import com.example.biblelog.domain.model.UserProfile
import com.example.biblelog.domain.model.UserSearchResult
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

fun ApiReadingRecordDto.toDomain() = ReadingRecord(
    id = id,
    date = LocalDate.parse(date),
    reference = reference.toDomain(),
    minutesRead = minutesRead,
    createdAt = createdAt.toInstant(),
)

fun ApiBibleReferenceDto.toDomain() = BibleReference(
    bookId = bookId,
    startChapter = startChapter,
    startVerse = startVerse,
    endBookId = endBookId ?: bookId,
    endChapter = endChapter,
    endVerse = endVerse,
)

fun BibleReference.toDto() = ApiBibleReferenceDto(
    bookId = bookId,
    startChapter = startChapter,
    startVerse = startVerse,
    endBookId = if (endBookId != bookId) endBookId else null,
    endChapter = endChapter,
    endVerse = endVerse,
)

fun ApiReadingProgressDto.toDomain() = ReadingProgress(
    overall = overall,
    oldTestament = oldTestament,
    newTestament = newTestament,
    byBook = byBook.mapKeys { it.key.toIntOrNull() ?: 0 },
)

fun ApiReadingStatsDto.toDomain() = ReadingStats(
    totalMinutes = totalMinutes,
    averageDailyMinutes = averageDailyMinutes,
    monthlyReadingDays = monthlyReadingDays,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)

fun ApiMeditationNoteDto.toDomain() = MeditationNote(
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

fun ApiFeedItemDto.toDomain() = FeedItem(
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

fun ApiAiMessageDto.toDomain() = AiMessage(
    id = id,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp.toInstant(),
    suggestedVerse = suggestedReference?.toDomain(),
)

fun ApiUserProfileDto.toDomain() = UserProfile(
    id = id,
    nickname = nickname,
    bio = bio,
    photoUrl = photoUrl,
    profileVisibility = profileVisibility.toProfileVisibility(),
    isLoggedIn = isLoggedIn,
)

fun ApiUserSearchResultDto.toDomain() = UserSearchResult(
    id = id,
    nickname = nickname,
    bio = bio,
    photoUrl = photoUrl,
)

fun ApiFollowRequestDto.toDomain() = FollowRequest(
    id = id,
    fromUserId = fromUserId,
    fromUserNickname = fromUserNickname,
    toUserId = toUserId,
    status = status,
    createdAt = createdAt.toInstant(),
)

fun ApiNotificationItemDto.toDomain() = NotificationItem(
    id = id,
    eventType = eventType,
    payload = payload,
    isRead = read,
    createdAt = createdAt.toInstant(),
)

private fun String.toProfileVisibility(): ProfileVisibility = when (this) {
    "private" -> ProfileVisibility.PRIVATE
    else -> ProfileVisibility.PUBLIC
}

fun ProfileVisibility.toApi(): String = when (this) {
    ProfileVisibility.PUBLIC -> "public"
    ProfileVisibility.PRIVATE -> "private"
}

fun ApiChurchDto.toDomain() = ChurchSummary(
    id = id,
    name = name,
    description = description,
)

fun ApiSmallGroupDto.toDomain() = SmallGroupSummary(
    id = id,
    name = name,
    churchId = churchId,
)

fun ApiUserMembershipsDto.toDomain() = UserMemberships(
    churchId = churchId,
    groupIds = groupIds.toSet(),
)

fun FaithReaction.toApi(): String = when (this) {
    FaithReaction.EMPATHY -> "empathy"
    FaithReaction.PRAY_TOGETHER -> "pray_together"
    FaithReaction.AMEN -> "amen"
    FaithReaction.GRACE -> "grace"
}

fun FeedFilter.toApi(): String = when (this) {
    FeedFilter.ALL -> "all"
    FeedFilter.SMALL_GROUP -> "small_group"
    FeedFilter.CHURCH -> "church"
    FeedFilter.FRIENDS -> "friends"
    FeedFilter.FOLLOWING -> "following"
}

fun FeedSort.toApi(): String = when (this) {
    FeedSort.LATEST -> "latest"
    FeedSort.POPULAR -> "popular"
}

fun AiConversationMode.toApi(): String = when (this) {
    AiConversationMode.CHAT -> "chat"
    AiConversationMode.PRAYER -> "prayer"
}

fun Emotion.toApi(): String = when (this) {
    Emotion.GRATITUDE -> "gratitude"
    Emotion.JOY -> "joy"
    Emotion.PEACE -> "peace"
    Emotion.SADNESS -> "sadness"
    Emotion.MOVED -> "moved"
}

fun NoteVisibility.toApi(): String = when (this) {
    NoteVisibility.PUBLIC -> "public"
    NoteVisibility.FRIENDS -> "friends"
    NoteVisibility.SMALL_GROUP -> "small_group"
    NoteVisibility.CHURCH -> "church"
    NoteVisibility.PRIVATE -> "private"
}

fun String.toEmotion(): Emotion? = when (this) {
    "gratitude" -> Emotion.GRATITUDE
    "joy" -> Emotion.JOY
    "peace" -> Emotion.PEACE
    "sadness" -> Emotion.SADNESS
    "moved" -> Emotion.MOVED
    else -> null
}

fun String.toVisibility(): NoteVisibility = when (this) {
    "public" -> NoteVisibility.PUBLIC
    "friends" -> NoteVisibility.FRIENDS
    "small_group" -> NoteVisibility.SMALL_GROUP
    "church" -> NoteVisibility.CHURCH
    else -> NoteVisibility.PRIVATE
}

fun String.toReaction(): FaithReaction = when (this) {
    "empathy" -> FaithReaction.EMPATHY
    "pray_together" -> FaithReaction.PRAY_TOGETHER
    "amen" -> FaithReaction.AMEN
    else -> FaithReaction.GRACE
}

fun ApiCommentDto.toDomain() = Comment(
    id = id,
    noteId = noteId,
    authorId = authorId,
    authorName = authorName,
    content = content,
    createdAt = createdAt.toInstant(),
)

fun String.toInstant(): Instant {
    val normalized = when {
        endsWith('Z') -> this
        length > 10 && this[10] == 'T' && (substringAfter('T').contains('+') || substringAfter('T').contains('-')) -> this
        else -> "${this}Z"
    }
    return Instant.parse(normalized)
}
