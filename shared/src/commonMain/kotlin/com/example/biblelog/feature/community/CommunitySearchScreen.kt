package com.example.biblelog.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.biblelog.di.AppContainer
import com.example.biblelog.domain.model.ChurchSummary
import com.example.biblelog.domain.model.CommunitySearchCategory
import com.example.biblelog.domain.model.SmallGroupSummary
import com.example.biblelog.domain.model.UserSearchResult
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunitySearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CommunitySearchViewModel = viewModel {
        CommunitySearchViewModel(AppContainer.repository)
    }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("검색", style = MaterialTheme.typography.headlineMedium)
            WantedButton(
                text = "닫기",
                onClick = onBack,
                variant = WantedButtonVariant.Text,
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedTextField(
            value = uiState.query,
            onValueChange = viewModel::updateQuery,
            label = "검색",
            placeholder = "이름으로 검색",
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            CommunitySearchCategory.entries.forEach { category ->
                WantedFilterChip(
                    label = category.toLabel(),
                    selected = uiState.category == category,
                    onClick = { viewModel.updateCategory(category) },
                )
            }
        }

        uiState.actionMessage?.let { message ->
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = WantedColors.Secondary,
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = WantedColors.Primary)
                }
            }
            uiState.query.isBlank() -> {
                Text(
                    text = "사용자, 교회, 소그룹을 검색해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WantedColors.Secondary,
                )
            }
            uiState.hasSearched && uiState.isResultEmpty() -> {
                Text(
                    text = "검색 결과가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WantedColors.Secondary,
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(WantedSpacing.Base.dp)) {
                    when (uiState.category) {
                        CommunitySearchCategory.USERS -> {
                            items(uiState.users, key = { it.id }) { user ->
                                UserSearchResultCard(
                                    user = user,
                                    isFriend = user.id in uiState.friendIds,
                                    isFollowing = user.id in uiState.followingIds,
                                    isPending = user.id in uiState.pendingFriendRequestIds,
                                    onFriendRequest = { viewModel.sendFriendRequest(user.id) },
                                    onFollow = { viewModel.followUser(user.id) },
                                )
                            }
                        }
                        CommunitySearchCategory.CHURCHES -> {
                            items(uiState.churches, key = { it.id }) { church ->
                                ChurchSearchResultCard(
                                    church = church,
                                    isJoined = uiState.joinedChurchId == church.id,
                                    onJoin = { viewModel.joinChurch(church.id) },
                                )
                            }
                        }
                        CommunitySearchCategory.SMALL_GROUPS -> {
                            items(uiState.smallGroups, key = { it.id }) { group ->
                                SmallGroupSearchResultCard(
                                    group = group,
                                    isJoined = group.id in uiState.joinedGroupIds,
                                    onJoin = { viewModel.joinSmallGroup(group.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CommunitySearchUiState.isResultEmpty(): Boolean = when (category) {
    CommunitySearchCategory.USERS -> users.isEmpty()
    CommunitySearchCategory.CHURCHES -> churches.isEmpty()
    CommunitySearchCategory.SMALL_GROUPS -> smallGroups.isEmpty()
}

private fun CommunitySearchCategory.toLabel(): String = when (this) {
    CommunitySearchCategory.USERS -> "사용자"
    CommunitySearchCategory.CHURCHES -> "교회"
    CommunitySearchCategory.SMALL_GROUPS -> "소그룹"
}

@Composable
private fun UserSearchResultCard(
    user: UserSearchResult,
    isFriend: Boolean,
    isFollowing: Boolean,
    isPending: Boolean,
    onFriendRequest: () -> Unit,
    onFollow: () -> Unit,
) {
    WantedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WantedColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.nickname.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = WantedColors.Primary,
                    )
                }
                Column {
                    Text(user.nickname, style = MaterialTheme.typography.titleMedium)
                    if (user.bio.isNotBlank()) {
                        Text(
                            user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = WantedColors.Secondary,
                        )
                    }
                }
            }
            UserActionButtons(
                isFriend = isFriend,
                isFollowing = isFollowing,
                isPending = isPending,
                onFriendRequest = onFriendRequest,
                onFollow = onFollow,
            )
        }
    }
}

@Composable
private fun UserActionButtons(
    isFriend: Boolean,
    isFollowing: Boolean,
    isPending: Boolean,
    onFriendRequest: () -> Unit,
    onFollow: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        when {
            isFriend -> Text("친구", style = MaterialTheme.typography.labelMedium, color = WantedColors.Secondary)
            isPending -> Text("요청됨", style = MaterialTheme.typography.labelMedium, color = WantedColors.Secondary)
            else -> WantedButton(
                text = "친구 요청",
                onClick = onFriendRequest,
                variant = WantedButtonVariant.Outlined,
            )
        }
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        when {
            isFollowing -> Text("팔로잉", style = MaterialTheme.typography.labelMedium, color = WantedColors.Secondary)
            else -> WantedButton(
                text = "팔로우",
                onClick = onFollow,
                variant = WantedButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun ChurchSearchResultCard(
    church: ChurchSummary,
    isJoined: Boolean,
    onJoin: () -> Unit,
) {
    WantedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(church.name, style = MaterialTheme.typography.titleMedium)
                if (church.description.isNotBlank()) {
                    Text(
                        church.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = WantedColors.Secondary,
                    )
                }
            }
            if (isJoined) {
                Text("가입됨", style = MaterialTheme.typography.labelMedium, color = WantedColors.Secondary)
            } else {
                WantedButton(text = "가입", onClick = onJoin)
            }
        }
    }
}

@Composable
private fun SmallGroupSearchResultCard(
    group: SmallGroupSummary,
    isJoined: Boolean,
    onJoin: () -> Unit,
) {
    WantedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Text("소그룹", style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
            }
            if (isJoined) {
                Text("가입됨", style = MaterialTheme.typography.labelMedium, color = WantedColors.Secondary)
            } else {
                WantedButton(text = "가입", onClick = onJoin)
            }
        }
    }
}
