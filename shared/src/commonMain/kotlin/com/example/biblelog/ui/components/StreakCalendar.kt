package com.example.biblelog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius
import com.example.biblelog.util.today
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
fun StreakCalendar(
    readingDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val todayDate = today()
    val startDate = todayDate.minus(DatePeriod(days = weeks * 7 - 1))
    val isInteractive = onDateSelected != null

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(weeks) { weekIndex ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(7) { dayIndex ->
                        val date = startDate.plus(DatePeriod(days = weekIndex * 7 + dayIndex))
                        val hasReading = readingDates.contains(date)
                        val minutesRead = if (hasReading) 1 else 0
                        val color = streakColor(minutesRead, hasReading)
                        val isSelected = selectedDate == date

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = WantedColors.Primary,
                                            shape = RoundedCornerShape(2.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (isInteractive) {
                                        Modifier.clickable { onDateSelected.invoke(date) }
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "적음",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 4.dp),
            )
            listOf(
                WantedColors.StreakLevel0,
                WantedColors.StreakLevel1,
                WantedColors.StreakLevel2,
                WantedColors.StreakLevel3,
                WantedColors.StreakLevel4,
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
            Text(
                text = "많음",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

private fun streakColor(minutesRead: Int, hasReading: Boolean): androidx.compose.ui.graphics.Color {
    if (!hasReading) return WantedColors.StreakLevel0
    return when {
        minutesRead >= 30 -> WantedColors.StreakLevel4
        minutesRead >= 20 -> WantedColors.StreakLevel3
        minutesRead >= 10 -> WantedColors.StreakLevel2
        else -> WantedColors.StreakLevel1
    }
}

@Composable
fun StreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WantedRadius.Lg.dp))
            .background(WantedColors.Primary.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "${streakDays}일 연속",
                    style = MaterialTheme.typography.titleLarge,
                    color = WantedColors.Primary,
                )
                Text(
                    text = "성경 읽기 Streak",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
