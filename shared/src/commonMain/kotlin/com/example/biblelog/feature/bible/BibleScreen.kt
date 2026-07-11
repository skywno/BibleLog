package com.example.biblelog.feature.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biblelog.data.BibleCatalog
import com.example.biblelog.di.AppContainer
import com.example.biblelog.domain.model.BibleReference
import com.example.biblelog.navigation.BibleSubRoute
import com.example.biblelog.ui.components.BiblePickerPoint
import com.example.biblelog.ui.components.BibleQuickReferenceInput
import com.example.biblelog.ui.components.BibleReferencePicker
import com.example.biblelog.ui.components.StreakCalendar
import com.example.biblelog.ui.components.WantedButton
import com.example.biblelog.ui.components.WantedButtonVariant
import com.example.biblelog.ui.components.WantedCard
import com.example.biblelog.ui.components.WantedProgressBar
import com.example.biblelog.ui.components.WantedSegmentedControl
import com.example.biblelog.ui.components.WantedTextField
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedSpacing
import com.example.biblelog.util.formatKoreanWithWeekday
import com.example.biblelog.util.today
import kotlinx.datetime.LocalDate

@Composable
fun BibleScreen(
    subRoute: BibleSubRoute,
    onSubRouteChange: (BibleSubRoute) -> Unit,
    onNavigateToJournalWrite: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BibleViewModel = viewModel {
        BibleViewModel(AppContainer.repository)
    }

    when (subRoute) {
        BibleSubRoute.Dashboard -> BibleDashboardScreen(
            viewModel = viewModel,
            onAddRecord = { onSubRouteChange(BibleSubRoute.AddRecord) },
            onViewStats = { onSubRouteChange(BibleSubRoute.Stats) },
            modifier = modifier,
        )
        BibleSubRoute.AddRecord -> AddReadingRecordScreen(
            viewModel = viewModel,
            onBack = { onSubRouteChange(BibleSubRoute.Dashboard) },
            onNavigateToJournalWrite = { reference ->
                onNavigateToJournalWrite(reference)
                onSubRouteChange(BibleSubRoute.Dashboard)
            },
            modifier = modifier,
        )
        BibleSubRoute.Stats -> BibleStatsScreen(
            viewModel = viewModel,
            onBack = { onSubRouteChange(BibleSubRoute.Dashboard) },
            modifier = modifier,
        )
    }
}

@Composable
private fun BibleDashboardScreen(
    viewModel: BibleViewModel,
    onAddRecord: () -> Unit,
    onViewStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by viewModel.readingProgress.collectAsState()
    val stats by viewModel.readingStats.collectAsState()
    val readingDates = viewModel.getReadingDates()
    val records by viewModel.readingRecords.collectAsState()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var hasUserSelectedDate by remember { mutableStateOf(false) }

    LaunchedEffect(records) {
        if (!hasUserSelectedDate) {
            selectedDate = records.maxByOrNull { it.date }?.date ?: today()
        }
    }

    val activeDate = selectedDate ?: records.maxByOrNull { it.date }?.date ?: today()
    val dayRecords = records
        .filter { it.date == activeDate }
        .sortedByDescending { it.createdAt }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WantedSpacing.Base.dp),
    ) {
        Text("성경 읽기", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("${stats.currentStreak}일", style = MaterialTheme.typography.headlineLarge, color = WantedColors.Primary)
                    Text("연속 읽기", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("${stats.totalMinutes}분", style = MaterialTheme.typography.headlineLarge)
                    Text("누적 읽기 시간", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedProgressBar(progress = progress.overall, label = "전체 진행률")

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedCard {
            Text("읽기 캘린더", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))
            StreakCalendar(
                readingDates = readingDates,
                selectedDate = activeDate,
                onDateSelected = { date ->
                    selectedDate = date
                    hasUserSelectedDate = true
                },
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp),
        ) {
            WantedButton(text = "기록 추가", onClick = onAddRecord, modifier = Modifier.weight(1f))
            WantedButton(
                text = "통계 보기",
                onClick = onViewStats,
                modifier = Modifier.weight(1f),
                variant = WantedButtonVariant.Outlined,
            )
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))
        Text(
            "이날의 기록 - ${activeDate.formatKoreanWithWeekday()}",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))

        if (dayRecords.isEmpty()) {
            Text(
                "이 날짜에 기록이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = WantedColors.Secondary,
            )
        } else {
            dayRecords.forEach { record ->
                WantedCard(modifier = Modifier.padding(bottom = WantedSpacing.Sm.dp)) {
                    Text(
                        record.reference.displayName(BibleCatalog.bookMap),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${record.minutesRead}분",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddReadingRecordScreen(
    viewModel: BibleViewModel,
    onBack: () -> Unit,
    onNavigateToJournalWrite: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMessage by viewModel.errorMessage.collectAsState()
    val readingSaveResult by viewModel.readingSaveResult.collectAsState()
    var showMeditationPrompt by remember { mutableStateOf(false) }
    var savedReference by remember { mutableStateOf<BibleReference?>(null) }
    var pendingJournalReference by remember { mutableStateOf<BibleReference?>(null) }

    LaunchedEffect(readingSaveResult) {
        readingSaveResult?.let { result ->
            savedReference = result.reference
            showMeditationPrompt = true
            viewModel.consumeReadingSaveResult()
        }
    }

    LaunchedEffect(pendingJournalReference) {
        val reference = pendingJournalReference ?: return@LaunchedEffect
        pendingJournalReference = null
        onNavigateToJournalWrite(reference)
    }

    if (showMeditationPrompt && savedReference != null) {
        val referenceLabel = savedReference!!.displayName(BibleCatalog.bookMap)
        AlertDialog(
            onDismissRequest = {
                showMeditationPrompt = false
                savedReference = null
                onBack()
            },
            title = { Text("읽기 기록을 저장했어요") },
            text = { Text("오늘 읽은 ${referenceLabel}에 대한 묵상도 남길까요?") },
            confirmButton = {
                WantedButton(
                    text = "묵상 작성",
                    onClick = {
                        pendingJournalReference = savedReference
                        showMeditationPrompt = false
                        savedReference = null
                    },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMeditationPrompt = false
                        savedReference = null
                        onBack()
                    },
                ) {
                    Text("나중에", color = WantedColors.Secondary)
                }
            },
        )
    }

    var startPoint by remember {
        mutableStateOf(BiblePickerPoint(bookId = 1, chapter = 1, verse = 1))
    }
    var endPoint by remember {
        mutableStateOf(BiblePickerPoint(bookId = 1, chapter = 1, verse = 10))
    }
    var minutes by remember { mutableStateOf("15") }

    LaunchedEffect(startPoint) {
        val (endBookId, endChapter, endVerse) = BibleCatalog.clampEnd(
            startPoint.bookId,
            startPoint.chapter,
            startPoint.verse,
            endPoint.bookId,
            endPoint.chapter,
            endPoint.verse,
        )
        if (endBookId != endPoint.bookId || endChapter != endPoint.chapter || endVerse != endPoint.verse) {
            endPoint = BiblePickerPoint(endBookId, endChapter, endVerse)
        }
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
            Text("읽기 기록 입력", style = MaterialTheme.typography.headlineMedium)
            WantedButton(text = "닫기", onClick = onBack, variant = WantedButtonVariant.Text)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        BibleQuickReferenceInput(
            onApply = { startPoint = it },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        BibleReferencePicker(
            label = "시작 구절",
            point = startPoint,
            onPointChange = { startPoint = it },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        BibleReferencePicker(
            label = "끝 구절",
            point = endPoint,
            onPointChange = { endPoint = it },
            minBookId = startPoint.bookId,
            minPoint = startPoint,
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Md.dp))

        WantedTextField(
            value = minutes,
            onValueChange = { minutes = it },
            label = "읽은 시간 (분)",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(WantedSpacing.Sm.dp))
            Text(it, color = WantedColors.Error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Xl.dp))

        WantedButton(
            text = "저장",
            onClick = {
                val reference = BibleReference(
                    bookId = startPoint.bookId,
                    startChapter = startPoint.chapter,
                    startVerse = startPoint.verse,
                    endBookId = endPoint.bookId,
                    endChapter = endPoint.chapter,
                    endVerse = endPoint.verse,
                )
                if (!BibleCatalog.isValidRange(
                        startPoint.bookId,
                        startPoint.chapter,
                        startPoint.verse,
                        endPoint.bookId,
                        endPoint.chapter,
                        endPoint.verse,
                    )
                ) {
                    return@WantedButton
                }
                viewModel.addReadingRecord(
                    reference = reference,
                    minutesRead = minutes.toIntOrNull() ?: 0,
                    date = today(),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BibleStatsScreen(
    viewModel: BibleViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by viewModel.readingProgress.collectAsState()
    val stats by viewModel.readingStats.collectAsState()
    var testamentFilter by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WantedSpacing.Base.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("읽기 통계", style = MaterialTheme.typography.headlineMedium)
            WantedButton(text = "닫기", onClick = onBack, variant = WantedButtonVariant.Text)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedCard {
            Text("누적 ${stats.totalMinutes}분 · 평균 ${stats.averageDailyMinutes.toInt()}분/일")
            Text("최고 Streak ${stats.bestStreak}일", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        WantedSegmentedControl(
            options = listOf("전체", "구약", "신약"),
            selectedIndex = testamentFilter,
            onSelected = { testamentFilter = it },
        )

        Spacer(modifier = Modifier.height(WantedSpacing.Base.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(WantedSpacing.Sm.dp)) {
            val filteredBooks = when (testamentFilter) {
                1 -> BibleCatalog.books.filter { it.id <= 39 }
                2 -> BibleCatalog.books.filter { it.id >= 40 }
                else -> BibleCatalog.books
            }
            items(filteredBooks) { book ->
                val bookProgress = progress.byBook[book.id] ?: 0f
                WantedCard {
                    WantedProgressBar(
                        progress = bookProgress,
                        label = book.nameKo,
                    )
                }
            }
        }
    }
}
