package com.example.biblelog.feature.journal

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.AppContainer
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.domain.model.Emotion
import com.example.biblelog.domain.model.NoteVisibility
import com.example.biblelog.navigation.JournalNavState
import com.example.biblelog.navigation.JournalSubRoute
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedFilterChip
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing

@Composable
fun JournalScreen(
    navState: JournalNavState,
    onNavStateChange: (JournalNavState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: JournalViewModel = viewModel {
        JournalViewModel(AppContainer.repository)
    }

    when (navState.route) {
        JournalSubRoute.List -> JournalListScreen(
            viewModel = viewModel,
            onWrite = { onNavStateChange(JournalNavState(JournalSubRoute.Write)) },
            onEdit = { id -> onNavStateChange(JournalNavState(JournalSubRoute.Write, editingNoteId = id)) },
            modifier = modifier,
        )
        JournalSubRoute.Write -> JournalWriteScreen(
            viewModel = viewModel,
            noteId = navState.editingNoteId,
            prefillReference = navState.prefillReference,
            onBack = { onNavStateChange(JournalNavState(JournalSubRoute.List)) },
            onPrefillConsumed = {
                if (navState.prefillReference != null) {
                    onNavStateChange(navState.copy(prefillReference = null))
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun JournalListScreen(
    viewModel: JournalViewModel,
    onWrite: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notes by viewModel.notes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterEmotion by remember { mutableStateOf<Emotion?>(null) }

    val filtered = notes.filter { note ->
        (searchQuery.isBlank() || note.content.contains(searchQuery)) &&
            (filterEmotion == null || note.emotion == filterEmotion)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("묵상 노트", style = MaterialTheme.typography.headlineMedium)
            WantedButton(text = "작성", onClick = onWrite)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "검색",
            placeholder = "키워드로 검색",
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        EmotionFilterRow(
            selected = filterEmotion,
            onSelected = { filterEmotion = it },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            items(filtered, key = { it.id }) { note ->
                WantedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(note.emotion?.toLabel().orEmpty(), style = MaterialTheme.typography.bodySmall)
                        Text(note.visibility.toLabel(), style = MaterialTheme.typography.bodySmall, color = WantedColors.Primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(note.content, style = MaterialTheme.typography.bodyLarge, maxLines = 3)
                    note.reference?.let { ref ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            ref.displayName(BibleCatalog.bookMap),
                            style = MaterialTheme.typography.bodySmall,
                            color = WantedColors.Primary,
                        )
                    }
                    note.prayerTopic?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🙏 $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                    WantedButton(
                        text = "수정",
                        onClick = { onEdit(note.id) },
                        variant = WantedButtonVariant.Text,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmotionFilterRow(
    selected: Emotion?,
    onSelected: (Emotion?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
        WantedFilterChip(
            label = "전체",
            selected = selected == null,
            onClick = { onSelected(null) },
        )
        Emotion.entries.forEach { emotion ->
            WantedFilterChip(
                label = emotion.toLabel(),
                selected = selected == emotion,
                onClick = { onSelected(emotion) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalWriteScreen(
    viewModel: JournalViewModel,
    noteId: String?,
    prefillReference: BibleReference?,
    onBack: () -> Unit,
    onPrefillConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notes by viewModel.notes.collectAsState()
    val existing = noteId?.let { id -> notes.find { it.id == id } }
    val initialReference = existing?.reference ?: prefillReference

    LaunchedEffect(prefillReference) {
        if (prefillReference != null) {
            onPrefillConsumed()
        }
    }

    var content by remember(existing, prefillReference) { mutableStateOf(existing?.content.orEmpty()) }
    var prayerTopic by remember(existing, prefillReference) { mutableStateOf(existing?.prayerTopic.orEmpty()) }
    var selectedEmotion by remember(existing, prefillReference) { mutableStateOf(existing?.emotion) }
    var selectedBookIndex by remember(existing, prefillReference) {
        mutableIntStateOf((initialReference?.bookId ?: 43) - 1)
    }
    var chapter by remember(existing, prefillReference) {
        mutableStateOf(initialReference?.startChapter?.toString() ?: "3")
    }
    var verse by remember(existing, prefillReference) {
        mutableStateOf(initialReference?.startVerse?.toString() ?: "16")
    }
    var savedReference by remember(existing, prefillReference) {
        mutableStateOf(initialReference)
    }
    var visibilityIndex by remember(existing, prefillReference) {
        mutableIntStateOf(
            when {
                existing != null -> NoteVisibility.entries.indexOf(existing.visibility)
                prefillReference != null -> NoteVisibility.entries.indexOf(NoteVisibility.PRIVATE)
                else -> NoteVisibility.entries.indexOf(NoteVisibility.PRIVATE)
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (noteId == null) "묵상 작성" else "묵상 수정",
                style = MaterialTheme.typography.headlineMedium,
            )
            WantedButton(text = "닫기", onClick = onBack, variant = WantedButtonVariant.Text)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedTextField(
            value = content,
            onValueChange = { content = it },
            label = "묵상 내용",
            singleLine = false,
            minLines = 5,
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        WantedTextField(
            value = prayerTopic,
            onValueChange = { prayerTopic = it },
            label = "기도 제목",
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        Text("감정", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            Emotion.entries.forEach { emotion ->
                WantedFilterChip(
                    label = emotion.toLabel(),
                    selected = selectedEmotion == emotion,
                    onClick = { selectedEmotion = emotion },
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        Text("연결 구절", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            WantedTextField(
                value = BibleCatalog.books[selectedBookIndex.coerceIn(BibleCatalog.books.indices)].nameKo,
                onValueChange = {},
                label = "책",
                modifier = Modifier.weight(2f),
            )
            WantedTextField(
                value = chapter,
                onValueChange = {
                    chapter = it
                    savedReference = null
                },
                label = "장",
                modifier = Modifier.weight(1f),
            )
            WantedTextField(
                value = verse,
                onValueChange = {
                    verse = it
                    savedReference = null
                },
                label = "절",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        Text("공개 범위", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            NoteVisibility.entries.forEachIndexed { index, vis ->
                WantedFilterChip(
                    label = vis.toLabel(),
                    selected = visibilityIndex == index,
                    onClick = { visibilityIndex = index },
                )
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            if (noteId != null) {
                WantedButton(
                    text = "삭제",
                    onClick = {
                        viewModel.deleteNote(noteId, onComplete = onBack)
                    },
                    variant = WantedButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
            WantedButton(
                text = "저장",
                onClick = {
                    val book = BibleCatalog.books[selectedBookIndex.coerceIn(BibleCatalog.books.indices)]
                    val reference = savedReference ?: BibleReference(
                        bookId = book.id,
                        startChapter = chapter.toIntOrNull() ?: 1,
                        startVerse = verse.toIntOrNull() ?: 1,
                        endChapter = chapter.toIntOrNull() ?: 1,
                        endVerse = verse.toIntOrNull() ?: 1,
                    )
                    viewModel.saveNote(
                        content = content,
                        prayerTopic = prayerTopic.ifBlank { null },
                        emotion = selectedEmotion,
                        reference = reference,
                        visibility = NoteVisibility.entries[visibilityIndex],
                        noteId = noteId,
                        onComplete = onBack,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun Emotion.toLabel(): String = when (this) {
    Emotion.GRATITUDE -> "감사"
    Emotion.JOY -> "기쁨"
    Emotion.PEACE -> "평안"
    Emotion.SADNESS -> "슬픔"
    Emotion.MOVED -> "감동"
}

private fun NoteVisibility.toLabel(): String = when (this) {
    NoteVisibility.PUBLIC -> "전체"
    NoteVisibility.FRIENDS -> "친구"
    NoteVisibility.SMALL_GROUP -> "소그룹"
    NoteVisibility.CHURCH -> "교회"
    NoteVisibility.PRIVATE -> "나만 보기"
}
