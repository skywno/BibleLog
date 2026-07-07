package com.example.biblelog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius

@Composable
fun WantedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WantedRadius.Lg.dp))
            .background(WantedColors.Canvas)
            .border(1.dp, WantedColors.Border, RoundedCornerShape(WantedRadius.Lg.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun WantedSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WantedRadius.Lg.dp))
            .background(WantedColors.SurfaceSubtle)
            .padding(16.dp),
        content = content,
    )
}
