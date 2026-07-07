package com.example.biblelog.feature.home

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.biblelog.di.LocalBibleLogRepository
import com.example.biblelog.ui.components.StreakBadge
import com.example.biblelog.ui.components.StreakCalendar
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedProgressBar
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
import com.example.biblelog.util.avatarInitial

@Composable
fun HomeScreen(
    onNavigateToBible: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = LocalBibleLogRepository.current
    val user by repository.currentUser.collectAsState()
    val progress = repository.getReadingProgress()
    val stats = repository.getReadingStats()
    val readingDates = repository.getReadingDates()
    val feed by repository.feed.collectAsState()
    val notifications by repository.notifications.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (user.nickname.isBlank()) "안녕하세요" else "안녕하세요, ${user.nickname}님",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WantedColors.Secondary,
                )
            }
            val unreadCount = notifications.count { !it.isRead }
            if (unreadCount > 0) {
                Text(
                    text = "🔔 $unreadCount",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(WantedColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.avatarInitial(),
                    style = MaterialTheme.typography.titleLarge,
                    color = WantedColors.Primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))

        StreakBadge(streakDays = stats.currentStreak)

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedCard {
            Text("오늘의 읽기 현황", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
            WantedProgressBar(progress = progress.overall, label = "전체 성경")
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Md.dp),
            ) {
                WantedProgressBar(
                    progress = progress.oldTestament,
                    label = "구약",
                    modifier = Modifier.weight(1f),
                )
                WantedProgressBar(
                    progress = progress.newTestament,
                    label = "신약",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedCard {
            Text("읽기 캘린더", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
            StreakCalendar(readingDates = readingDates)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        Text("빠른 액션", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            WantedButton(
                text = "읽기 기록",
                onClick = onNavigateToBible,
                modifier = Modifier.weight(1f),
            )
            WantedButton(
                text = "묵상 작성",
                onClick = onNavigateToJournal,
                modifier = Modifier.weight(1f),
                variant = WantedButtonVariant.Secondary,
            )
        }
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        WantedButton(
            text = "AI 영적 동반자",
            onClick = onNavigateToAi,
            modifier = Modifier.fillMaxWidth(),
            variant = WantedButtonVariant.Outlined,
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        Text("최근 공동체 활동", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        feed.take(3).forEach { item ->
            WantedCard(modifier = Modifier.padding(bottom = WantedSpacing.Sm.dp)) {
                Text(item.note.authorName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                )
            }
        }
    }
}
