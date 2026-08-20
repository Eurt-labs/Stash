package com.eurtlabs.stash.ui.theme

import androidx.compose.ui.graphics.Color
import com.eurtlabs.stash.data.model.ColorTheme

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF0E0E0E)
val SurfaceCard = Color(0xFF18181B)
val BorderSubtle = Color(0xFF27272A)
val GreenSuccess = Color(0xFF10B981)
val RedError = Color(0xFFEF4444)

data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val pillActive: Color,
    val pillOnActive: Color,
    val success: Color = Color(0xFF10B981),
    val error: Color = Color(0xFFEF4444)
)

fun getThemePalette(theme: ColorTheme): ThemePalette {
    return when (theme) {
        ColorTheme.OBSIDIAN -> ThemePalette(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFFA1A1AA),
            background = Color(0xFF000000),
            surface = Color(0xFF0E0E0E),
            surfaceVariant = Color(0xFF18181B),
            border = Color(0xFF27272A),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            pillActive = Color(0xFFFFFFFF),
            pillOnActive = Color(0xFF000000)
        )
        ColorTheme.TITANIUM -> ThemePalette(
            primary = Color(0xFFE2E8F0),
            onPrimary = Color(0xFF0F172A),
            secondary = Color(0xFF94A3B8),
            background = Color(0xFF0A0F1D),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1E293B),
            border = Color(0xFF334155),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            pillActive = Color(0xFFE2E8F0),
            pillOnActive = Color(0xFF0F172A)
        )
        ColorTheme.GRAPHITE -> ThemePalette(
            primary = Color(0xFFF4F4F5),
            onPrimary = Color(0xFF18181B),
            secondary = Color(0xFFA1A1AA),
            background = Color(0xFF0C0C0E),
            surface = Color(0xFF141416),
            surfaceVariant = Color(0xFF202024),
            border = Color(0xFF2E2E33),
            textPrimary = Color(0xFFFAFAFA),
            textSecondary = Color(0xFFA1A1AA),
            pillActive = Color(0xFFF4F4F5),
            pillOnActive = Color(0xFF18181B)
        )
        ColorTheme.NORD -> ThemePalette(
            primary = Color(0xFFECEFF4),
            onPrimary = Color(0xFF2E3440),
            secondary = Color(0xFF88C0D0),
            background = Color(0xFF0F141C),
            surface = Color(0xFF18202C),
            surfaceVariant = Color(0xFF242E3E),
            border = Color(0xFF354256),
            textPrimary = Color(0xFFECEFF4),
            textSecondary = Color(0xFF9BA7B8),
            pillActive = Color(0xFFECEFF4),
            pillOnActive = Color(0xFF2E3440)
        )
        ColorTheme.SAGE -> ThemePalette(
            primary = Color(0xFFDCFCE7),
            onPrimary = Color(0xFF14532D),
            secondary = Color(0xFF86EFAC),
            background = Color(0xFF0B100D),
            surface = Color(0xFF131A15),
            surfaceVariant = Color(0xFF1C261F),
            border = Color(0xFF2A382F),
            textPrimary = Color(0xFFF0FDF4),
            textSecondary = Color(0xFF8DA695),
            pillActive = Color(0xFFDCFCE7),
            pillOnActive = Color(0xFF14532D)
        )
        ColorTheme.ESPRESSO -> ThemePalette(
            primary = Color(0xFFFEF3C7),
            onPrimary = Color(0xFF78350F),
            secondary = Color(0xFFFDE68A),
            background = Color(0xFF110E0B),
            surface = Color(0xFF1C1713),
            surfaceVariant = Color(0xFF28211C),
            border = Color(0xFF3B312A),
            textPrimary = Color(0xFFFFFBEB),
            textSecondary = Color(0xFFA6988E),
            pillActive = Color(0xFFFEF3C7),
            pillOnActive = Color(0xFF78350F)
        )
        ColorTheme.MIDNIGHT -> ThemePalette(
            primary = Color(0xFFE0E7FF),
            onPrimary = Color(0xFF1E1B4B),
            secondary = Color(0xFF818CF8),
            background = Color(0xFF070A12),
            surface = Color(0xFF0F1322),
            surfaceVariant = Color(0xFF182038),
            border = Color(0xFF253052),
            textPrimary = Color(0xFFEEF2FF),
            textSecondary = Color(0xFF8B9BB8),
            pillActive = Color(0xFFE0E7FF),
            pillOnActive = Color(0xFF1E1B4B)
        )
    }
}
