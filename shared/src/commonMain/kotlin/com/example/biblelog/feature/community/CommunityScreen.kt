package com.example.biblelog.feature.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.AppContainer
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.FeedFilter
import com.example.biblelog.domain.model.FeedSort
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.navigation.CommunityNavState
import com.example.biblelog.navigation.CommunitySubRoute
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedSegmentedControl
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing

@Composable
fun CommunityScreen(
    navState: CommunityNavState,
    onNavStateChange: (CommunityNavState) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (navState.route) {
        CommunitySubRoute.Feed -> CommunityFeedScreen(
            onSearch = { onNavStateChange(CommunityNavState(CommunitySubRoute.Search)) },
            modifier = modifier,
        )
        CommunitySubRoute.Search -> CommunitySearchScreen(
            onBack = { onNavStateChange(CommunityNavState(CommunitySubRoute.Feed)) },
            onUserClick = { userId ->
                onNavStateChange(
                    CommunityNavState(
                        route = CommunitySubRoute.UserProfile,
                        profileUserId = userId,
                    ),
                )
            },
            modifier = modifier,
        )
        CommunitySubRoute.UserProfile -> {
            val userId = navState.profileUserId
            if (userId != null) {
                com.example.biblelog.feature.profile.UserProfileScreen(
                    userId = userId,
                    onBack = { onNavStateChange(CommunityNavState(CommunitySubRoute.Feed)) },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun CommunityFeedScreen(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CommunityViewModel = viewModel {
        CommunityViewModel(AppContainer.repository)
    }
    val feed by viewModel.feed.collectAsState()
    val feedHasMore by viewModel.feedHasMore.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    var filterIndex by remember { mutableIntStateOf(0) }
    var sortIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val filters = listOf(
        FeedFilter.ALL to "전체",
        FeedFilter.SMALL_GROUP to "소그룹",
        FeedFilter.CHURCH to "교회",
        FeedFilter.FRIENDS to "친구",
        FeedFilter.FOLLOWING to "팔로잉",
    )
    val feedFilter = filters[filterIndex].first
    val feedSort = if (sortIndex == 1) FeedSort.POPULAR else FeedSort.LATEST

    LaunchedEffect(feedFilter, feedSort) {
        viewModel.loadFeed(feedFilter, feedSort)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= feed.size - 3 && feedHasMore && !isLoadingMore && feed.isNotEmpty()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

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
            Text("공동체 피드", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색",
                    tint = WantedColors.Primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            filters.forEachIndexed { index, (_, label) ->
                WantedFilterChip(
                    label = label,
                    selected = filterIndex == index,
                    onClick = { filterIndex = index },
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        WantedSegmentedControl(
            options = listOf("최신순", "인기순"),
            selectedIndex = sortIndex,
            onSelected = { sortIndex = it },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(WantedSpacing.Base.dp),
        ) {
            items(feed, key = { it.note.id }) { item ->
                FeedCard(
                    item = item,
                    onReaction = { reaction ->
                        viewModel.toggleReaction(item.note.id, reaction)
                    },
                )
            }
            if (isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            color = WantedColors.Primary,
                            modifier = Modifier.padding(WantedSpacing.Base.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedCard(
    item: com.example.biblelog.domain.model.FeedItem,
    onReaction: (FaithReaction) -> Unit,
) {
    WantedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.note.authorName, style = MaterialTheme.typography.titleMedium)
            Text(item.note.visibility.toFeedLabel(), style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        item.note.reference?.let { ref ->
            Text(
                ref.displayName(BibleCatalog.bookMap),
                style = MaterialTheme.typography.bodySmall,
                color = WantedColors.Primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(item.note.content, style = MaterialTheme.typography.bodyLarge)

        item.note.prayerTopic?.let {
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
            Text("🙏 $it", style = MaterialTheme.typography.bodyMedium, color = WantedColors.Secondary)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            FaithReaction.entries.forEach { reactionType ->
                val reaction = item.reactions.find { it.type == reactionType }
                WantedFilterChip(
                    label = "${reactionType.toLabel()} ${reaction?.count ?: 0}",
                    selected = reaction?.reactedByMe == true,
                    onClick = { onReaction(reactionType) },
                )
            }
        }

        if (item.commentCount > 0) {
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
            Text("💬 댓글 ${item.commentCount}개", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun FaithReaction.toLabel(): String = when (this) {
    FaithReaction.EMPATHY -> "공감"
    FaithReaction.PRAY_TOGETHER -> "함께 기도"
    FaithReaction.AMEN -> "아멘"
    FaithReaction.GRACE -> "은혜"
}

private fun NoteVisibility.toFeedLabel(): String = when (this) {
    NoteVisibility.PUBLIC -> "전체 공개"
    NoteVisibility.FRIENDS -> "친구"
    NoteVisibility.SMALL_GROUP -> "소그룹"
    NoteVisibility.CHURCH -> "교회"
    NoteVisibility.PRIVATE -> "비공개"
}
