package com.example.biblelog.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiUserProfileDto(
    val id: String,
    val nickname: String,
    val bio: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    @SerialName("profile_visibility") val profileVisibility: String = "public",
    @SerialName("is_logged_in") val isLoggedIn: Boolean = true,
    @SerialName("viewer_can_view_content") val viewerCanViewContent: Boolean = true,
    @SerialName("viewer_follow_pending") val viewerFollowPending: Boolean = false,
)

@Serializable
data class ApiRefreshTokenRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class ApiAuthTokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    val user: ApiUserProfileDto,
)

@Serializable
data class ApiOAuthAuthorizeResponseDto(
    @SerialName("authorization_url") val authorizationUrl: String,
    val state: String,
)

@Serializable
data class ApiBibleReferenceDto(
    @SerialName("book_id") val bookId: Int,
    @SerialName("start_chapter") val startChapter: Int,
    @SerialName("start_verse") val startVerse: Int,
    @SerialName("end_book_id") val endBookId: Int? = null,
    @SerialName("end_chapter") val endChapter: Int,
    @SerialName("end_verse") val endVerse: Int,
)

@Serializable
data class ApiReadingRecordDto(
    val id: String,
    val date: String,
    val reference: ApiBibleReferenceDto,
    @SerialName("minutes_read") val minutesRead: Int,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiCreateReadingRecordRequestDto(
    val date: String,
    val reference: ApiBibleReferenceDto,
    @SerialName("minutes_read") val minutesRead: Int,
)

@Serializable
data class ApiReadingProgressDto(
    val overall: Float,
    @SerialName("old_testament") val oldTestament: Float,
    @SerialName("new_testament") val newTestament: Float,
    @SerialName("by_book") val byBook: Map<String, Float>,
)

@Serializable
data class ApiReadingStatsDto(
    @SerialName("total_minutes") val totalMinutes: Int,
    @SerialName("average_daily_minutes") val averageDailyMinutes: Float,
    @SerialName("current_streak") val currentStreak: Int,
    @SerialName("best_streak") val bestStreak: Int,
    @SerialName("monthly_reading_days") val monthlyReadingDays: Map<Int, Int> = emptyMap(),
)

@Serializable
data class ApiMeditationNoteDto(
    val id: String,
    val content: String,
    @SerialName("prayer_topic") val prayerTopic: String? = null,
    val emotion: String? = null,
    val reference: ApiBibleReferenceDto? = null,
    val visibility: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ApiUpsertJournalNoteRequestDto(
    val content: String,
    @SerialName("prayer_topic") val prayerTopic: String? = null,
    val emotion: String? = null,
    val reference: ApiBibleReferenceDto? = null,
    val visibility: String,
)

@Serializable
data class ApiReactionCountDto(
    val type: String,
    val count: Int,
    @SerialName("reacted_by_me") val reactedByMe: Boolean,
)

@Serializable
data class ApiFeedItemDto(
    val note: ApiMeditationNoteDto,
    val reactions: List<ApiReactionCountDto>,
    @SerialName("comment_count") val commentCount: Int,
)

@Serializable
data class ApiFeedPageResponseDto(
    val items: List<ApiFeedItemDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ApiAiMessageDto(
    val id: String,
    val content: String,
    @SerialName("is_from_user") val isFromUser: Boolean,
    val timestamp: String,
    @SerialName("suggested_reference") val suggestedReference: ApiBibleReferenceDto? = null,
)

@Serializable
data class ApiSendAiMessageRequestDto(
    val content: String,
    val mode: String = "chat",
)

@Serializable
data class ApiSendAiMessageResponseDto(
    @SerialName("user_message") val userMessage: ApiAiMessageDto,
    @SerialName("assistant_message") val assistantMessage: ApiAiMessageDto,
    val provider: String? = null,
)

@Serializable
data class ApiAiConversationSummaryDto(
    val id: String,
    val mode: String,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ApiToggleReactionRequestDto(
    val reaction: String,
)

@Serializable
data class ApiUpdateUserProfileRequestDto(
    val nickname: String? = null,
    val bio: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("profile_visibility") val profileVisibility: String? = null,
)

@Serializable
data class ApiErrorResponseDto(
    val detail: String,
)

@Serializable
data class ApiUserSearchResultDto(
    val id: String,
    val nickname: String,
    val bio: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
)

@Serializable
data class ApiFollowRequestDto(
    val id: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("from_user_nickname") val fromUserNickname: String,
    @SerialName("to_user_id") val toUserId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiFriendRequestDto(
    val id: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("from_user_nickname") val fromUserNickname: String,
    @SerialName("to_user_id") val toUserId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiSendFriendRequestDto(
    @SerialName("to_user_id") val toUserId: String,
)

@Serializable
data class ApiFollowUserDto(
    val id: String,
    val nickname: String,
    val bio: String = "",
)

@Serializable
data class ApiChurchDto(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiCreateChurchRequestDto(
    val name: String,
    val description: String = "",
)

@Serializable
data class ApiSmallGroupDto(
    val id: String,
    @SerialName("church_id") val churchId: String? = null,
    val name: String,
    @SerialName("leader_id") val leaderId: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiCreateSmallGroupRequestDto(
    val name: String,
    @SerialName("church_id") val churchId: String? = null,
)

@Serializable
data class ApiUserMembershipsDto(
    @SerialName("church_id") val churchId: String? = null,
    @SerialName("group_ids") val groupIds: List<String> = emptyList(),
)

@Serializable
data class ApiCommentDto(
    val id: String,
    @SerialName("note_id") val noteId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ApiCreateCommentRequestDto(
    val content: String,
)

@Serializable
data class ApiCommentPageResponseDto(
    val items: List<ApiCommentDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ApiNotificationItemDto(
    val id: String,
    @SerialName("event_type") val eventType: String,
    val payload: Map<String, String> = emptyMap(),
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiNotificationPageResponseDto(
    val items: List<ApiNotificationItemDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)
