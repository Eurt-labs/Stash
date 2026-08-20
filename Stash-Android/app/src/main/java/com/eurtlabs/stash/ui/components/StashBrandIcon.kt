package com.eurtlabs.stash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eurtlabs.stash.ui.theme.LocalStashPalette

@Composable
fun StashBrandIcon(
    size: Dp = 38.dp,
    iconTint: Color = Color.White
) {
    val palette = LocalStashPalette.current

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(palette.surfaceVariant)
            .border(1.dp, palette.border, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeWidth = w * 0.11f

            // 1. Note Loop (Left Bottom Circle)
            val circleCenter = Offset(w * 0.36f, h * 0.68f)
            val radius = w * 0.22f
            drawCircle(
                color = iconTint,
                radius = radius,
                center = circleCenter,
                style = Stroke(width = strokeWidth)
            )

            // 2. Note Stem & Arch extending into download shaft
            val stemPath = Path().apply {
                moveTo(w * 0.58f, h * 0.68f)
                lineTo(w * 0.58f, h * 0.32f)
                cubicTo(
                    w * 0.58f, h * 0.14f,
                    w * 0.86f, h * 0.14f,
                    w * 0.86f, h * 0.32f
                )
                lineTo(w * 0.86f, h * 0.62f)
            }
            drawPath(
                path = stemPath,
                color = iconTint,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 3. Download Arrowhead
            val arrowPath = Path().apply {
                moveTo(w * 0.72f, h * 0.50f)
                lineTo(w * 0.86f, h * 0.64f)
                lineTo(w * 1.00f, h * 0.50f)
            }
            drawPath(
                path = arrowPath,
                color = iconTint,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
