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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.LocalBibleLogRepository
import com.example.biblelog.domain.model.FaithReaction
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedSegmentedControl
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(modifier: Modifier = Modifier) {
    val repository = LocalBibleLogRepository.current
    val feed by repository.feed.collectAsState()
    var filterIndex by remember { mutableIntStateOf(0) }
    var sortIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val filters = listOf("전체", "소그룹", "교회", "친구")
    val filteredFeed = when (filterIndex) {
        1 -> feed.filter { it.note.visibility == NoteVisibility.SMALL_GROUP }
        2 -> feed.filter { it.note.visibility == NoteVisibility.CHURCH }
        3 -> feed.filter { it.note.visibility == NoteVisibility.FRIENDS }
        else -> feed
    }.let { items ->
        when (sortIndex) {
            1 -> items.sortedByDescending { item -> item.reactions.sumOf { it.count } }
            else -> items.sortedByDescending { it.note.createdAt }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WantedSpacing.Base.dp),
    ) {
        Text("공동체 피드", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            filters.forEachIndexed { index, label ->
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(WantedSpacing.Base.dp)) {
            items(filteredFeed, key = { it.note.id }) { item ->
                FeedCard(
                    item = item,
                    onReaction = { reaction ->
                        scope.launch {
                            repository.toggleReaction(item.note.id, reaction)
                        }
                    },
                )
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
            item.reactions.filter { it.count > 0 || it.reactedByMe }.forEach { reaction ->
                WantedFilterChip(
                    label = "${reaction.type.toLabel()} ${reaction.count}",
                    selected = reaction.reactedByMe,
                    onClick = { onReaction(reaction.type) },
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
