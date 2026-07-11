package com.example.biblelog.feature.profile

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
import com.example.biblelog.navigation.LogoutIcon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.di.AppContainer
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.domain.model.ProfileVisibility
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
import com.example.biblelog.util.avatarInitial

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val viewModel: ProfileViewModel = viewModel {
        ProfileViewModel(
            AppContainer.repository,
            AppContainer.sessionCoordinator,
        )
    }
    val user by viewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    var nickname by remember(user) { mutableStateOf(user.nickname) }
    var bio by remember(user) { mutableStateOf(user.bio) }
    var photoUrl by remember(user) { mutableStateOf(user.photoUrl) }
    var visibilityPrivate by remember(user) { mutableStateOf(user.profileVisibility == ProfileVisibility.PRIVATE) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WantedSpacing.Base.dp),
    ) {
        Text("프로필", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WantedColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.avatarInitial(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = WantedColors.Primary,
                )
            }
            Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
            if (!isEditing) {
                Text(user.nickname, style = MaterialTheme.typography.headlineMedium)
                Text(user.bio, style = MaterialTheme.typography.bodyMedium, color = WantedColors.Secondary)
                Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
                WantedButton(text = "프로필 수정", onClick = { isEditing = true })
            } else {
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

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))

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
                                    onClick = { viewModel.acceptFriendRequest(requestId) },
                                    variant = WantedButtonVariant.Text,
                                )
                            }
                            "follow_request" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    WantedButton(
                                        text = "수락",
                                        onClick = { viewModel.acceptFollowRequest(requestId) },
                                        variant = WantedButtonVariant.Text,
                                    )
                                    WantedButton(
                                        text = "거절",
                                        onClick = { viewModel.rejectFollowRequest(requestId) },
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

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        ProfileMenuItem(icon = { Icon(Icons.Default.Settings, null) }, title = "계정 설정", subtitle = "알림, 개인정보")
        ProfileMenuItem(
            icon = { Icon(LogoutIcon, null) },
            title = "로그아웃",
            subtitle = "현재 계정에서 로그아웃",
            onClick = { viewModel.logout() },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        Text(
            "Google · Facebook OAuth는 서버 `.env` 설정 후 사용할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = WantedColors.Secondary,
        )
    }
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
