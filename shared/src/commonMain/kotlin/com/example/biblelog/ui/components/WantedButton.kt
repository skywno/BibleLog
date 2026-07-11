package com.example.biblelog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedRadius

enum class WantedButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Text,
}

@Composable
fun WantedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WantedButtonVariant = WantedButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(WantedRadius.Md.dp)
    val contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)

    when (variant) {
        WantedButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 40.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = WantedColors.Primary,
                contentColor = Color.White,
                disabledContainerColor = WantedColors.Disabled,
                disabledContentColor = Color.White,
            ),
            contentPadding = contentPadding,
        ) {
            Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }

        WantedButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 40.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = WantedColors.SurfaceSubtle,
                contentColor = WantedColors.Heading,
                disabledContainerColor = WantedColors.SurfaceSubtle,
                disabledContentColor = WantedColors.Secondary,
            ),
            contentPadding = contentPadding,
        ) {
            Text(text)
        }

        WantedButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 40.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, WantedColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = WantedColors.Heading,
                disabledContentColor = WantedColors.Secondary,
            ),
            contentPadding = contentPadding,
        ) {
            Text(text)
        }

        WantedButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = WantedColors.Primary),
        ) {
            Text(text)
        }
    }
}

@Composable
fun WantedStatusButton(
    text: String,
    modifier: Modifier = Modifier,
    variant: WantedButtonVariant = WantedButtonVariant.Outlined,
) {
    val shape = RoundedCornerShape(WantedRadius.Md.dp)
    val contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)

    when (variant) {
        WantedButtonVariant.Secondary -> Surface(
            modifier = modifier.heightIn(min = 40.dp),
            shape = shape,
            color = WantedColors.SurfaceSubtle,
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = WantedColors.Secondary,
                )
            }
        }

        else -> Surface(
            modifier = modifier.heightIn(min = 40.dp),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, WantedColors.Border),
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = WantedColors.Secondary,
                )
            }
        }
    }
}
