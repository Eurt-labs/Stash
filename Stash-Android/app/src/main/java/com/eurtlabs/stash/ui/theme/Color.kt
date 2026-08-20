package com.eurtlabs.stash.ui.theme

import androidx.compose.ui.graphics.Color
import com.eurtlabs.stash.data.model.ColorTheme

val BackgroundDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF101524)
val SurfaceCard = Color(0xFF161C2E)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val BorderSubtle = Color(0xFF1E293B)
val GreenSuccess = Color(0xFF10B981)
val RedError = Color(0xFFEF4444)

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val background: Color = BackgroundDark,
    val surface: Color = SurfaceDark
)

fun getThemePalette(theme: ColorTheme): ThemePalette {
    return when (theme) {
        ColorTheme.INDIGO -> ThemePalette(Color(0xFF6366F1), Color(0xFF818CF8))
        ColorTheme.EMERALD -> ThemePalette(Color(0xFF10B981), Color(0xFF34D399))
        ColorTheme.SUNSET -> ThemePalette(Color(0xFFF97316), Color(0xFFFB923C))
        ColorTheme.SAPPHIRE -> ThemePalette(Color(0xFF0EA5E9), Color(0xFF38BDF8))
        ColorTheme.AMBER -> ThemePalette(Color(0xFFF59E0B), Color(0xFFFBBF24))
        ColorTheme.CRIMSON -> ThemePalette(Color(0xFFE11D48), Color(0xFFF43F5E))
        ColorTheme.OLED -> ThemePalette(Color(0xFF71717A), Color(0xFFA1A1AA), Color.Black, Color(0xFF0F0F0F))
        ColorTheme.WEEKND -> ThemePalette(Color(0xFFE11D48), Color(0xFFFB7185))
        ColorTheme.TAYLOR -> ThemePalette(Color(0xFF38BDF8), Color(0xFF818CF8))
        ColorTheme.BILLIE -> ThemePalette(Color(0xFF22C55E), Color(0xFF4ADE80))
        ColorTheme.DAFTPUNK -> ThemePalette(Color(0xFFEAB308), Color(0xFFFACC15))
        ColorTheme.TRAVIS -> ThemePalette(Color(0xFFB45309), Color(0xFFD97706))
        ColorTheme.LANA -> ThemePalette(Color(0xFFF472B6), Color(0xFFFB7185))
    }
}
