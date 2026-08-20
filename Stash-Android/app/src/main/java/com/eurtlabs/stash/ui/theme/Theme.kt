package com.eurtlabs.stash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.eurtlabs.stash.data.model.ColorTheme

@Composable
fun StashTheme(
    theme: ColorTheme = ColorTheme.WEEKND,
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(theme)

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        background = palette.background,
        surface = palette.surface,
        onPrimary = TextPrimary,
        onSecondary = TextPrimary,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
