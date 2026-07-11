package com.example.biblelog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.data.BibleReferenceParser
import com.example.biblelog.domain.model.BibleBook
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius
import com.example.biblelog.ui.theme.WantedSpacing

data class BiblePickerPoint(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
)

@Composable
fun BibleReferencePicker(
    label: String,
    point: BiblePickerPoint,
    onPointChange: (BiblePickerPoint) -> Unit,
    modifier: Modifier = Modifier,
    minBookId: Int = 1,
    minPoint: BiblePickerPoint? = null,
) {
    var bookPickerExpanded by remember { mutableStateOf(false) }
    var bookSearch by remember { mutableStateOf("") }
    var quickRef by remember { mutableStateOf("") }
    var quickRefError by remember { mutableStateOf<String?>(null) }

    val selectedBook = BibleCatalog.book(point.bookId)
    val availableBooks = remember(minBookId, bookSearch) {
        BibleCatalog.books
            .filter { it.id >= minBookId }
            .filter { book ->
                if (bookSearch.isBlank()) true
                else {
                    val q = bookSearch.trim().lowercase()
                    book.nameKo.lowercase().contains(q) || book.nameEn.lowercase().contains(q)
                }
            }
    }

    val chapterRange = remember(point.bookId) { BibleCatalog.chapters(point.bookId).toList() }
    val verseRange = remember(point.bookId, point.chapter) {
        BibleCatalog.verses(point.bookId, point.chapter).toList()
    }

    val minCompare = minPoint?.let {
        BibleCatalog.comparePoint(it.bookId, it.chapter, it.verse)
    }

    fun isPointAllowed(candidate: BiblePickerPoint): Boolean {
        if (candidate.bookId < minBookId) return false
        if (!BibleCatalog.isValidPoint(candidate.bookId, candidate.chapter, candidate.verse)) return false
        val compare = BibleCatalog.comparePoint(candidate.bookId, candidate.chapter, candidate.verse)
        return minCompare == null || compare >= minCompare
    }

    fun updatePoint(candidate: BiblePickerPoint) {
        if (!isPointAllowed(candidate)) return
        onPointChange(candidate)
    }

    LaunchedEffect(point.bookId, point.chapter, verseRange) {
        if (verseRange.isNotEmpty() && point.verse !in verseRange) {
            val clamped = verseRange.last()
            if (isPointAllowed(point.copy(verse = clamped))) {
                onPointChange(point.copy(verse = clamped))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        WantedTextField(
            value = quickRef,
            onValueChange = {
                quickRef = it
                quickRefError = null
            },
            label = "빠른 입력 (예: 창 1:1)",
            placeholder = "창 1:1, 시 23:1",
        )
        if (quickRef.isNotBlank()) {
            Spacer(modifier = Modifier.height(WantedSpacing.Xs.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
                WantedButton(
                    text = "적용",
                    onClick = {
                        val parsed = BibleReferenceParser.parse(quickRef)
                        if (parsed == null) {
                            quickRefError = "형식을 확인해 주세요."
                        } else {
                            val candidate = BiblePickerPoint(parsed.bookId, parsed.chapter, parsed.verse)
                            if (!isPointAllowed(candidate)) {
                                quickRefError = "선택 가능한 범위를 벗어났습니다."
                            } else {
                                onPointChange(candidate)
                                quickRef = ""
                                quickRefError = null
                                bookPickerExpanded = false
                            }
                        }
                    },
                    variant = WantedButtonVariant.Outlined,
                )
            }
            quickRefError?.let {
                Spacer(modifier = Modifier.height(WantedSpacing.Xs.dp))
                Text(it, color = WantedColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        WantedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bookPickerExpanded = !bookPickerExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("성경 권", style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
                    Text(
                        selectedBook?.nameKo ?: "선택",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    if (bookPickerExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WantedColors.Secondary,
                )
            }

            if (bookPickerExpanded) {
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedTextField(
                    value = bookSearch,
                    onValueChange = { bookSearch = it },
                    label = "권 검색",
                    placeholder = "창세기, 시편, 마태...",
                )
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(availableBooks, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            selected = book.id == point.bookId,
                            onClick = {
                                val newChapter = point.chapter.coerceIn(1, book.totalChapters)
                                val maxVerse = com.example.biblelog.data.BibleVerseCounts.verseCount(book.id, newChapter)
                                val newVerse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                                val candidate = BiblePickerPoint(book.id, newChapter, newVerse)
                                if (isPointAllowed(candidate)) {
                                    onPointChange(candidate)
                                    bookPickerExpanded = false
                                    bookSearch = ""
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        Text(
            "${selectedBook?.nameKo ?: ""} ${point.chapter}:${point.verse}",
            style = MaterialTheme.typography.bodyLarge,
            color = WantedColors.Primary,
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            NumberPickerColumn(
                title = "장",
                values = chapterRange,
                selected = point.chapter,
                modifier = Modifier.weight(1f),
                isEnabled = { chapter ->
                    val maxVerse = com.example.biblelog.data.BibleVerseCounts.verseCount(point.bookId, chapter)
                    val verse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                    isPointAllowed(BiblePickerPoint(point.bookId, chapter, verse))
                },
                onSelect = { chapter ->
                    val maxVerse = com.example.biblelog.data.BibleVerseCounts.verseCount(point.bookId, chapter)
                    val verse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                    updatePoint(BiblePickerPoint(point.bookId, chapter, verse))
                },
            )
            NumberPickerColumn(
                title = "절",
                values = verseRange,
                selected = point.verse,
                modifier = Modifier.weight(1f),
                isEnabled = { verse -> isPointAllowed(point.copy(verse = verse)) },
                onSelect = { verse -> updatePoint(point.copy(verse = verse)) },
            )
        }
    }
}

@Composable
private fun BookListItem(
    book: BibleBook,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) WantedColors.Canvas else WantedColors.SurfaceSubtle
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WantedRadius.Sm.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = book.nameKo,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) WantedColors.Primary else WantedColors.Heading,
        )
    }
}

@Composable
private fun NumberPickerColumn(
    title: String,
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: (Int) -> Boolean = { true },
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, values) {
        val index = values.indexOf(selected)
        if (index >= 0) {
            listState.scrollToItem(index.coerceAtLeast(0))
        }
    }

    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 180.dp)
                .clip(RoundedCornerShape(WantedRadius.Md.dp))
                .background(WantedColors.SurfaceSubtle)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(values, key = { it }) { value ->
                val enabled = isEnabled(value)
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WantedRadius.Sm.dp))
                        .background(
                            when {
                                isSelected -> WantedColors.Primary
                                enabled -> WantedColors.Canvas
                                else -> WantedColors.SurfaceSubtle
                            },
                        )
                        .clickable(enabled = enabled) { onSelect(value) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            isSelected -> androidx.compose.ui.graphics.Color.White
                            enabled -> WantedColors.Heading
                            else -> WantedColors.Disabled
                        },
                    )
                }
            }
        }
    }
}
