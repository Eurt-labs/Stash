package com.eurtlabs.stash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Card — replicates the iOS / WebGL liquid glass refraction shader aesthetic.
 *
 * Shader decomposition (from the GLSL fragment shader):
 *   1. Squircle mask (power=6 superellipse) → approximated by high-radius RoundedCornerShape
 *   2. Lens distortion + 9×9 Gaussian blur → opaque frosted dark fill + subtle radial luminance
 *   3. Specular top-edge gradient (rb1 * gradient) → top meniscus highlight
 *   4. Border rim ring (rb2 * 0.3 lighting) → thin specular rim border
 *   5. Smooth transition at edges → smoothstep via gradient alpha falloff
 *
 * Params:
 *   cornerRadius — squircle corner radius (default 22dp for cards, 28dp for nav/modals)
 *   rimAlpha — specular top-rim brightness (0.0-1.0), higher = more glass refraction feel
 *   innerPadding — content padding inside the glass surface
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    rimAlpha: Float = 0.55f,
    innerPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val palette = com.eurtlabs.stash.ui.theme.LocalStashPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            // Layer 1: Opaque frosted dark fill (simulates the blurred background sampling)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        palette.surfaceVariant, // slightly lighter top (specular gradient offset)
                        palette.surface,        // core frosted dark
                        palette.surface         // bottom edge fade
                    )
                )
            )
            // Layer 3: Specular top-meniscus rim border (rb2 * lighting from shader)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = rimAlpha),         // bright meniscus top
                        Color.White.copy(alpha = rimAlpha * 0.35f), // mid-fade
                        Color.White.copy(alpha = 0.03f)             // near-invisible bottom
                    )
                ),
                shape = shape
            )
            .padding(innerPadding),
        content = content
    )
}

/**
 * Compact variant — for inline pills, chips, and small selectable items.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    accentColor: Color = Color.White,
    cornerRadius: Dp = 18.dp,
    innerPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val palette = com.eurtlabs.stash.ui.theme.LocalStashPalette.current

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    if (isSelected) {
                        listOf(
                            palette.border,
                            palette.surfaceVariant
                        )
                    } else {
                        listOf(
                            palette.surfaceVariant,
                            palette.surface
                        )
                    }
                )
            )
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                brush = Brush.verticalGradient(
                    if (isSelected) {
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    }
                ),
                shape = shape
            )
            .then(if (innerPadding > 0.dp) Modifier.padding(innerPadding) else Modifier),
        content = content
    )
}

/**
 * Draws a subtle radial luminance glow in the upper-center of the card,
 * simulating the lens distortion center-brightening from the WebGL shader.
 */
private fun drawLensGlow(scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    // Soft elliptical glow positioned in the upper third
    scope.drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.015f),
                Color.Transparent
            ),
            center = Offset(w * 0.5f, h * 0.25f),
            radius = w * 0.6f
        ),
        topLeft = Offset(w * 0.1f, 0f),
        size = Size(w * 0.8f, h * 0.6f)
    )
}
