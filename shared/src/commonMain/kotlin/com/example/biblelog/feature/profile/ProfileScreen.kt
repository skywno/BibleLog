package com.example.biblelog.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.di.AppContainer
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.navigation.LogoutIcon
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = viewModel {
        ProfileViewModel(
            AppContainer.repository,
            AppContainer.sessionCoordinator,
        )
    }
    val user by viewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val communityState by viewModel.communityState.collectAsState()
    val notes by viewModel.notes.collectAsState()
    var nickname by remember(user) { mutableStateOf(user.nickname) }
    var bio by remember(user) { mutableStateOf(user.bio) }
    var photoUrl by remember(user) { mutableStateOf(user.photoUrl) }
    var visibilityPrivate by remember(user) { mutableStateOf(user.profileVisibility == ProfileVisibility.PRIVATE) }
    var isEditing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.isRead }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("프로필", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSettings = !showSettings }) {
                    Box {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = if (showSettings) WantedColors.Primary else WantedColors.Heading,
                        )
                        if (unreadCount > 0 && !showSettings) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(WantedColors.Primary),
                            )
                        }
                    }
                }
                WantedButton(text = "닫기", onClick = onBack, variant = WantedButtonVariant.Text)
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Lg.dp))

        if (!isEditing) {
            ProfileHeaderSection(
                nickname = user.nickname,
                bio = user.bio,
                photoUrl = user.photoUrl,
                metaLine = formatProfileMetaLine(communityState),
                onEditClick = { isEditing = true },
            )
            Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))
            ProfileNotesSection(notes = notes)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                WantedTextField(value = nickname, onValueChange = { nickname = it }, label = "닉네임")
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedTextField(value = bio, onValueChange = { bio = it }, label = "한 줄 소개")
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedTextField(value = photoUrl, onValueChange = { photoUrl = it }, label = "프로필 사진 URL")
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
                    WantedFilterChip(
                        label = "공개",
                        selected = !visibilityPrivate,
                        onClick = { visibilityPrivate = false },
                    )
                    WantedFilterChip(
                        label = "비공개",
                        selected = visibilityPrivate,
                        onClick = { visibilityPrivate = true },
                    )
                }
                Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
                Row {
                    WantedButton(
                        text = "취소",
                        onClick = {
                            nickname = user.nickname
                            bio = user.bio
                            photoUrl = user.photoUrl
                            visibilityPrivate = user.profileVisibility == ProfileVisibility.PRIVATE
                            isEditing = false
                        },
                        variant = WantedButtonVariant.Outlined,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = WantedSpacing.Sm.dp))
                    WantedButton(
                        text = "저장",
                        onClick = {
                            viewModel.updateProfile(
                                nickname = nickname,
                                bio = bio,
                                photoUrl = photoUrl,
                                profileVisibility = if (visibilityPrivate) {
                                    ProfileVisibility.PRIVATE
                                } else {
                                    ProfileVisibility.PUBLIC
                                },
                            ) {
                                isEditing = false
                            }
                        },
                    )
                }
            }
        }

        AnimatedVisibility(visible = showSettings) {
            Column {
                Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))
                ProfileSettingsSection(
                    notifications = notifications,
                    onAcceptFriendRequest = viewModel::acceptFriendRequest,
                    onAcceptFollowRequest = viewModel::acceptFollowRequest,
                    onRejectFollowRequest = viewModel::rejectFollowRequest,
                    onLogout = viewModel::logout,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    nickname: String,
    bio: String,
    photoUrl: String,
    metaLine: String,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Md.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(name = nickname, photoUrl = photoUrl)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                WantedButton(
                    text = "수정",
                    onClick = onEditClick,
                    variant = WantedButtonVariant.Text,
                )
            }
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = WantedColors.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

    WantedCard {
        Text(
            text = "소개",
            style = MaterialTheme.typography.labelMedium,
            color = WantedColors.Secondary,
        )
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        Text(
            text = bio.ifBlank { "소개를 입력해 보세요." },
            style = MaterialTheme.typography.bodyMedium,
            color = if (bio.isBlank()) WantedColors.Secondary else WantedColors.Heading,
        )
    }
}

private fun formatProfileMetaLine(state: ProfileCommunityState): String =
    "${formatAffiliationLine(state)} · 친구 ${state.friends.size}명"

private fun formatAffiliationLine(state: ProfileCommunityState): String {
    if (state.isLoading) return "소속 불러오는 중..."
    val parts = buildList {
        state.church?.let { add(it.name) }
        addAll(state.smallGroups.map { it.name })
    }
    return parts.joinToString(" · ").ifBlank { "소속 없음" }
}

@Composable
private fun ProfileAvatar(name: String, photoUrl: String) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(WantedColors.Primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.toString() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
            color = WantedColors.Primary,
        )
    }
}

@Composable
private fun ProfileNotesSection(notes: List<MeditationNote>) {
    Text("내 묵상", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
    if (notes.isEmpty()) {
        Text(
            text = "작성한 묵상이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = WantedColors.Secondary,
        )
    } else {
        notes.forEach { note ->
            ProfileNoteCard(note = note)
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        }
    }
}

@Composable
private fun ProfileNoteCard(note: MeditationNote) {
    WantedCard {
        note.reference?.let { ref ->
            Text(
                ref.displayName(BibleCatalog.bookMap),
                style = MaterialTheme.typography.bodySmall,
                color = WantedColors.Primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(note.content, style = MaterialTheme.typography.bodyLarge, maxLines = 4)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        Text(
            note.visibility.toLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = WantedColors.Secondary,
        )
    }
}

private fun NoteVisibility.toLabel(): String = when (this) {
    NoteVisibility.PUBLIC -> "전체 공개"
    NoteVisibility.FRIENDS -> "친구"
    NoteVisibility.SMALL_GROUP -> "소그룹"
    NoteVisibility.CHURCH -> "교회"
    NoteVisibility.PRIVATE -> "비공개"
}

@Composable
private fun ProfileSettingsSection(
    notifications: List<com.example.biblelog.domain.model.NotificationItem>,
    onAcceptFriendRequest: (String) -> Unit,
    onAcceptFollowRequest: (String) -> Unit,
    onRejectFollowRequest: (String) -> Unit,
    onLogout: () -> Unit,
) {
    WantedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = WantedColors.Primary)
            Text(
                "알림 센터",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = WantedSpacing.Md.dp),
            )
        }
        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
        if (notifications.isEmpty()) {
            Text("새 알림이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = WantedColors.Secondary)
        } else {
            notifications.forEach { notification ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = WantedSpacing.Sm.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            notification.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (notification.isRead) WantedColors.Secondary else WantedColors.Heading,
                        )
                        Text(notification.body, style = MaterialTheme.typography.bodySmall)
                    }
                    notification.actionRequestId?.let { requestId ->
                        when (notification.eventType) {
                            "friend_request" -> {
                                WantedButton(
                                    text = "수락",
                                    onClick = { onAcceptFriendRequest(requestId) },
                                    variant = WantedButtonVariant.Text,
                                )
                            }
                            "follow_request" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    WantedButton(
                                        text = "수락",
                                        onClick = { onAcceptFollowRequest(requestId) },
                                        variant = WantedButtonVariant.Text,
                                    )
                                    WantedButton(
                                        text = "거절",
                                        onClick = { onRejectFollowRequest(requestId) },
                                        variant = WantedButtonVariant.Text,
                                    )
                                }
                            }
                        }
                    }
                    if (!notification.isRead && notification.actionRequestId == null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(WantedColors.Primary),
                        )
                    }
                }
                HorizontalDivider(color = WantedColors.Divider)
            }
        }
    }

    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

    ProfileMenuItem(
        icon = { Icon(Icons.Default.Settings, null) },
        title = "계정 설정",
        subtitle = "알림, 개인정보",
    )
    ProfileMenuItem(
        icon = { Icon(LogoutIcon, null) },
        title = "로그아웃",
        subtitle = "현재 계정에서 로그아웃",
        onClick = onLogout,
    )

    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

    Text(
        "Google · Facebook OAuth는 서버 `.env` 설정 후 사용할 수 있습니다.",
        style = MaterialTheme.typography.bodySmall,
        color = WantedColors.Secondary,
    )
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    WantedCard(modifier = Modifier.padding(bottom = WantedSpacing.Sm.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(modifier = Modifier.padding(start = WantedSpacing.Md.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
            }
            if (onClick != null) {
                WantedButton(text = "실행", onClick = onClick, variant = WantedButtonVariant.Text)
            }
        }
    }
}
