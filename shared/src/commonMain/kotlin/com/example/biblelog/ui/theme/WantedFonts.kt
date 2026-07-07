package com.example.biblelog.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import biblelog.shared.generated.resources.Res
import biblelog.shared.generated.resources.WantedSans_Bold
import biblelog.shared.generated.resources.WantedSans_Medium
import biblelog.shared.generated.resources.WantedSans_Regular
import biblelog.shared.generated.resources.WantedSans_SemiBold
import org.jetbrains.compose.resources.Font

@Composable
fun rememberWantedFontFamily(): FontFamily = FontFamily(
    Font(Res.font.WantedSans_Regular, FontWeight.Normal),
    Font(Res.font.WantedSans_Medium, FontWeight.Medium),
    Font(Res.font.WantedSans_SemiBold, FontWeight.SemiBold),
    Font(Res.font.WantedSans_Bold, FontWeight.Bold),
)
