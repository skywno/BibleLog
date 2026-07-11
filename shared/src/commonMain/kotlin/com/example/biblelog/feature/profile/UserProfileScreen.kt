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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.AppContainer
import com.example.biblelog.domain.model.MeditationNote
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: UserProfileViewModel = viewModel(key = userId) {
        UserProfileViewModel(AppContainer.repository, userId)
    }
    val uiState by viewModel.uiState.collectAsState()

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
            WantedButton(text = "닫기", onClick = onBack, variant = WantedButtonVariant.Text)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WantedColors.Primary)
                }
            }
            uiState.errorMessage != null -> {
                Text(uiState.errorMessage!!, color = WantedColors.Error)
            }
            uiState.profile != null -> {
                val profile = uiState.profile!!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(name = profile.nickname, photoUrl = profile.photoUrl)
                    Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
                    Text(profile.nickname, style = MaterialTheme.typography.headlineMedium)
                    if (profile.bio.isNotBlank()) {
                        Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = WantedColors.Secondary)
                    }
                }

                Spacer(modifier = Modifier.height(WantedSpacing.Lg.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
                    when {
                        uiState.isFriend -> Text("친구", color = WantedColors.Secondary)
                        uiState.isPendingFriend -> Text("친구 요청됨", color = WantedColors.Secondary)
                        else -> WantedButton(
                            text = "친구 요청",
                            onClick = viewModel::sendFriendRequest,
                            variant = WantedButtonVariant.Outlined,
                        )
                    }
                    when {
                        uiState.isFollowing -> Text("팔로잉", color = WantedColors.Secondary)
                        uiState.isPendingFollow -> Text("팔로우 요청됨", color = WantedColors.Secondary)
                        else -> WantedButton(
                            text = "팔로우",
                            onClick = viewModel::followUser,
                            variant = WantedButtonVariant.Secondary,
                        )
                    }
                }

                uiState.actionMessage?.let {
                    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
                }

                Spacer(modifier = Modifier.height(WantedSpacing.Lg.dp))
                Text("공개 묵상", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

                if (uiState.notes.isEmpty()) {
                    Text("표시할 묵상이 없습니다.", color = WantedColors.Secondary)
                } else {
                    uiState.notes.forEach { note ->
                        PublicNoteCard(note = note)
                        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, photoUrl: String) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(WantedColors.Primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.toString() ?: "?",
            style = MaterialTheme.typography.headlineLarge,
            color = WantedColors.Primary,
        )
    }
}

@Composable
private fun PublicNoteCard(note: MeditationNote) {
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
