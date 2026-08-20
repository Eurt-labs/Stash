package com.eurtlabs.stash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.eurtlabs.stash.data.model.ColorTheme

val LocalStashPalette = staticCompositionLocalOf { getThemePalette(ColorTheme.OBSIDIAN) }

@Composable
fun StashTheme(
    theme: ColorTheme = ColorTheme.OBSIDIAN,
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(theme)

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        secondary = palette.secondary,
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.surfaceVariant,
        outline = palette.border,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary
    )

    CompositionLocalProvider(LocalStashPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
