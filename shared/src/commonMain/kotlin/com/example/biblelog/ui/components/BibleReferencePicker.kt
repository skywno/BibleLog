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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.data.BibleReferenceParser
import com.example.biblelog.data.BibleVerseCounts
import com.example.biblelog.data.ParseError
import com.example.biblelog.domain.model.BibleBook
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius
import com.example.biblelog.ui.theme.WantedSpacing

data class BiblePickerPoint(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
)

private enum class ExpandedPicker {
    NONE,
    BOOK,
    CHAPTER,
    VERSE,
}

@Composable
fun BibleQuickReferenceInput(
    onApply: (BiblePickerPoint) -> Unit,
    isPointAllowed: (BiblePickerPoint) -> Boolean = { true },
    modifier: Modifier = Modifier,
) {
    var quickRef by remember { mutableStateOf("") }
    var quickRefError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(true) }

    fun applyQuickRef() {
        val result = BibleReferenceParser.parse(quickRef)
        val parsed = result.point
        if (parsed == null) {
            quickRefError = when (result.error) {
                ParseError.EmptyInput -> "구절을 입력해 주세요."
                ParseError.UnknownBook -> "성경 권을 찾을 수 없습니다."
                ParseError.InvalidFormat -> "형식을 확인해 주세요."
                ParseError.OutOfRange -> "존재하지 않는 장·절입니다."
                null -> "형식을 확인해 주세요."
            }
            return
        }
        val candidate = BiblePickerPoint(parsed.bookId, parsed.chapter, parsed.verse)
        if (!isPointAllowed(candidate)) {
            quickRefError = "선택 가능한 범위를 벗어났습니다."
            return
        }
        onApply(candidate)
        quickRef = ""
        quickRefError = null
        visible = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (visible) {
            WantedTextField(
                value = quickRef,
                onValueChange = {
                    quickRef = it
                    quickRefError = null
                },
                label = "빠른 입력",
                placeholder = "창 1:1 / 창세기 1장 30절",
                imeAction = ImeAction.Done,
                onImeAction = { if (quickRef.isNotBlank()) applyQuickRef() },
            )
            if (quickRef.isNotBlank()) {
                Spacer(modifier = Modifier.height(WantedSpacing.Xs.dp))
                WantedButton(
                    text = "적용",
                    onClick = ::applyQuickRef,
                    variant = WantedButtonVariant.Outlined,
                )
            }
            quickRefError?.let {
                Spacer(modifier = Modifier.height(WantedSpacing.Xs.dp))
                Text(it, color = WantedColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                WantedButton(
                    text = "다시 입력",
                    onClick = { visible = true },
                    variant = WantedButtonVariant.Text,
                )
            }
        }
    }
}

@Composable
fun BibleReferencePicker(
    label: String,
    point: BiblePickerPoint,
    onPointChange: (BiblePickerPoint) -> Unit,
    modifier: Modifier = Modifier,
    minBookId: Int = 1,
    minPoint: BiblePickerPoint? = null,
) {
    var expandedPicker by remember { mutableStateOf(ExpandedPicker.NONE) }
    var bookSearch by remember { mutableStateOf("") }

    val bookListState = rememberLazyListState()
    val chapterListState = rememberLazyListState()
    val verseListState = rememberLazyListState()

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

    fun collapseAll() {
        expandedPicker = ExpandedPicker.NONE
        bookSearch = ""
    }

    LaunchedEffect(point.bookId, point.chapter, verseRange) {
        if (verseRange.isNotEmpty() && point.verse !in verseRange) {
            val clamped = verseRange.last()
            if (isPointAllowed(point.copy(verse = clamped))) {
                onPointChange(point.copy(verse = clamped))
            }
        }
    }

    LaunchedEffect(expandedPicker, point.bookId, availableBooks) {
        if (expandedPicker == ExpandedPicker.BOOK && availableBooks.isNotEmpty()) {
            val index = availableBooks.indexOfFirst { it.id == point.bookId }.coerceAtLeast(0)
            bookListState.scrollToItem(index)
        }
    }

    LaunchedEffect(expandedPicker, point.chapter, chapterRange) {
        if (expandedPicker == ExpandedPicker.CHAPTER && chapterRange.isNotEmpty()) {
            val index = chapterRange.indexOf(point.chapter).coerceAtLeast(0)
            chapterListState.scrollToItem(index)
        }
    }

    LaunchedEffect(expandedPicker, point.verse, verseRange) {
        if (expandedPicker == ExpandedPicker.VERSE && verseRange.isNotEmpty()) {
            val index = verseRange.indexOf(point.verse).coerceAtLeast(0)
            verseListState.scrollToItem(index)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            CollapsiblePickerChip(
                title = "권",
                value = selectedBook?.nameKo ?: "선택",
                expanded = expandedPicker == ExpandedPicker.BOOK,
                onClick = {
                    expandedPicker = if (expandedPicker == ExpandedPicker.BOOK) {
                        ExpandedPicker.NONE
                    } else {
                        ExpandedPicker.BOOK
                    }
                },
                modifier = Modifier.weight(1f),
            )
            CollapsiblePickerChip(
                title = "장",
                value = "${point.chapter}장",
                expanded = expandedPicker == ExpandedPicker.CHAPTER,
                onClick = {
                    expandedPicker = if (expandedPicker == ExpandedPicker.CHAPTER) {
                        ExpandedPicker.NONE
                    } else {
                        ExpandedPicker.CHAPTER
                    }
                },
                modifier = Modifier.weight(1f),
            )
            CollapsiblePickerChip(
                title = "절",
                value = "${point.verse}절",
                expanded = expandedPicker == ExpandedPicker.VERSE,
                onClick = {
                    expandedPicker = if (expandedPicker == ExpandedPicker.VERSE) {
                        ExpandedPicker.NONE
                    } else {
                        ExpandedPicker.VERSE
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        when (expandedPicker) {
            ExpandedPicker.BOOK -> {
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedCard {
                    WantedTextField(
                        value = bookSearch,
                        onValueChange = { bookSearch = it },
                        label = "권 검색",
                        placeholder = "창세기, 시편, 마태...",
                    )
                    Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                    LazyColumn(
                        state = bookListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(availableBooks, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                selected = book.id == point.bookId,
                                onClick = {
                                    val newChapter = point.chapter.coerceIn(1, book.totalChapters)
                                    val maxVerse = BibleVerseCounts.verseCount(book.id, newChapter)
                                    val newVerse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                                    val candidate = BiblePickerPoint(book.id, newChapter, newVerse)
                                    if (isPointAllowed(candidate)) {
                                        onPointChange(candidate)
                                        collapseAll()
                                    }
                                },
                            )
                        }
                    }
                }
            }
            ExpandedPicker.CHAPTER -> {
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedCard {
                    LazyColumn(
                        state = chapterListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(chapterRange, key = { it }) { chapter ->
                            NumberListItem(
                                label = "${chapter}장",
                                selected = chapter == point.chapter,
                                enabled = {
                                    val maxVerse = BibleVerseCounts.verseCount(point.bookId, chapter)
                                    val verse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                                    isPointAllowed(BiblePickerPoint(point.bookId, chapter, verse))
                                },
                                onClick = {
                                    val maxVerse = BibleVerseCounts.verseCount(point.bookId, chapter)
                                    val verse = point.verse.coerceIn(1, maxVerse.coerceAtLeast(1))
                                    updatePoint(BiblePickerPoint(point.bookId, chapter, verse))
                                    collapseAll()
                                },
                            )
                        }
                    }
                }
            }
            ExpandedPicker.VERSE -> {
                Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
                WantedCard {
                    LazyColumn(
                        state = verseListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(verseRange, key = { it }) { verse ->
                            NumberListItem(
                                label = "${verse}절",
                                selected = verse == point.verse,
                                enabled = { isPointAllowed(point.copy(verse = verse)) },
                                onClick = {
                                    updatePoint(point.copy(verse = verse))
                                    collapseAll()
                                },
                            )
                        }
                    }
                }
            }
            ExpandedPicker.NONE -> Unit
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        Text(
            "${selectedBook?.nameKo ?: ""} ${point.chapter}:${point.verse}",
            style = MaterialTheme.typography.bodyLarge,
            color = WantedColors.Primary,
        )
    }
}

@Composable
private fun CollapsiblePickerChip(
    title: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WantedCard(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = WantedColors.Secondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = WantedColors.Secondary,
                )
            }
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
private fun NumberListItem(
    label: String,
    selected: Boolean,
    enabled: () -> Boolean,
    onClick: () -> Unit,
) {
    val isEnabled = enabled()
    val background = when {
        selected -> WantedColors.Primary
        isEnabled -> WantedColors.Canvas
        else -> WantedColors.SurfaceSubtle
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WantedRadius.Sm.dp))
            .background(background)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                selected -> androidx.compose.ui.graphics.Color.White
                isEnabled -> WantedColors.Heading
                else -> WantedColors.Disabled
            },
        )
    }
}
