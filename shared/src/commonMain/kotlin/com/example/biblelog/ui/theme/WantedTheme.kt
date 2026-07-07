package com.example.biblelog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val WantedLightColorScheme = lightColorScheme(
    primary = WantedColors.Primary,
    onPrimary = Color.White,
    primaryContainer = WantedColors.StreakLevel1,
    onPrimaryContainer = WantedColors.Primary,
    secondary = WantedColors.AccentViolet,
    onSecondary = Color.White,
    background = WantedColors.Canvas,
    onBackground = WantedColors.Heading,
    surface = WantedColors.Canvas,
    onSurface = WantedColors.Heading,
    surfaceVariant = WantedColors.SurfaceSubtle,
    onSurfaceVariant = WantedColors.Body,
    outline = WantedColors.Border,
    outlineVariant = WantedColors.Divider,
    error = WantedColors.Error,
    onError = Color.White,
)

@Composable
fun WantedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Wanted DS is light-mode first; dark theme deferred until Montage tokens are mapped.
    val fontFamily = rememberWantedFontFamily()
    val typography = remember(fontFamily) { wantedTypography(fontFamily) }
    MaterialTheme(
        colorScheme = WantedLightColorScheme,
        typography = typography,
        content = content,
    )
}

object WantedSpacing {
    const val Xs = 4
    const val Sm = 8
    const val Md = 12
    const val Base = 16
    const val Lg = 20
    const val Xl = 24
    const val Xxl = 32
    const val Section = 64
}

object WantedRadius {
    const val Sm = 6
    const val Md = 8
    const val Lg = 12
    const val Chip = 10
    const val Full = 9999
}
