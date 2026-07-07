package com.example.biblelog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius

@Composable
fun WantedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) WantedColors.Canvas else Color.Transparent
    val borderColor = if (selected) WantedColors.Primary else WantedColors.Border
    val textColor = if (selected) WantedColors.Primary else WantedColors.Heading

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WantedRadius.Chip.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(WantedRadius.Chip.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}

@Composable
fun WantedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WantedRadius.Chip.dp))
            .background(WantedColors.SurfaceSubtle)
            .padding(4.dp),
    ) {
        androidx.compose.foundation.layout.Row {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(WantedRadius.Md.dp))
                        .background(if (selected) WantedColors.Canvas else Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) WantedColors.Body else WantedColors.Secondary,
                    )
                }
            }
        }
    }
}
