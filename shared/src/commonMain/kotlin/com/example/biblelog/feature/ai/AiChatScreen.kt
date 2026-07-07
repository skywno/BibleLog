package com.example.biblelog.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.LocalBibleLogRepository
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius
import com.example.biblelog.ui.theme.WantedSpacing
import kotlinx.coroutines.launch

@Composable
fun AiChatScreen(modifier: Modifier = Modifier) {
    val repository = LocalBibleLogRepository.current
    val messages by repository.aiMessages.collectAsState()
    var input by remember { mutableStateOf("") }
    var isPrayerMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
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
            Column {
                Text("AI 영적 동반자", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (isPrayerMode) "기도 모드" else "대화 모드",
                    style = MaterialTheme.typography.bodySmall,
                    color = WantedColors.Primary,
                )
            }
            WantedButton(
                text = if (isPrayerMode) "대화 모드" else "기도 모드",
                onClick = { isPrayerMode = !isPrayerMode },
            )
        }

        Spacer(modifier = Modifier.padding(WantedSpacing.Sm.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            WantedTextField(
                value = input,
                onValueChange = { input = it },
                label = if (isPrayerMode) "기도 제목이나 마음" else "메시지",
                singleLine = false,
                minLines = 1,
                modifier = Modifier.weight(1f),
            )
            WantedButton(
                text = "전송",
                onClick = {
                    if (input.isBlank()) return@WantedButton
                    val text = if (isPrayerMode) "함께 기도해 주세요: $input" else input
                    scope.launch {
                        repository.sendAiMessage(text)
                        input = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatBubble(message: com.example.biblelog.domain.model.AiMessage) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromUser) WantedColors.Primary else WantedColors.SurfaceSubtle
    val textColor = if (message.isFromUser) WantedColors.Canvas else WantedColors.Body

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(WantedRadius.Lg.dp))
                .background(backgroundColor)
                .padding(WantedSpacing.Md.dp),
        ) {
            Text(message.content, style = MaterialTheme.typography.bodyLarge, color = textColor)
            message.suggestedVerse?.let { ref ->
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    "📖 ${ref.displayName(BibleCatalog.bookMap)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isFromUser) WantedColors.Canvas.copy(alpha = 0.8f) else WantedColors.Primary,
                )
            }
        }
    }
}
