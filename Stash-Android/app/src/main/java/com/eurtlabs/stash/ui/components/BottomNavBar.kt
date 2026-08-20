package com.eurtlabs.stash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.model.NavigationTab
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BottomNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    activeQueueCount: Int = 0,
    libraryCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragXOffset by remember { mutableFloatStateOf(0f) }

    // Glassmorphic Nav Shell
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.surface.copy(alpha = 0.90f),
                        palette.background.copy(alpha = 0.98f)
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        // Specular top glass rim highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragXOffset = offset.x
                        },
                        onDragEnd = {
                            isDragging = false
                            val totalWidthPx = size.width.toFloat()
                            val tabWidthPx = totalWidthPx / NavigationTab.values().size
                            val selectedIndex = (dragXOffset / tabWidthPx).toInt().coerceIn(0, NavigationTab.values().size - 1)
                            onTabSelected(NavigationTab.values()[selectedIndex])
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragXOffset = (dragXOffset + dragAmount).coerceIn(0f, size.width.toFloat())
                            val totalWidthPx = size.width.toFloat()
                            val tabWidthPx = totalWidthPx / NavigationTab.values().size
                            val targetIndex = (dragXOffset / tabWidthPx).toInt().coerceIn(0, NavigationTab.values().size - 1)
                            if (targetIndex != currentTab.ordinal) {
                                onTabSelected(NavigationTab.values()[targetIndex])
                            }
                        }
                    )
                }
        ) {
            val totalWidth = maxWidth
            val tabCount = NavigationTab.values().size
            val tabWidth = totalWidth / tabCount
            val bubbleWidth = if (isDragging) 70.dp else 64.dp
            val bubbleHeight = 32.dp

            val targetBubbleOffset = tabWidth * currentTab.ordinal + (tabWidth - bubbleWidth) / 2

            // Liquid Sliding Glass Bubble indicator with spring physics
            val bubbleOffset by animateDpAsState(
                targetValue = targetBubbleOffset,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = if (isDragging) 800f else 360f
                ),
                label = "liquidBubbleOffset"
            )

            val liquidScaleX by animateFloatAsState(
                targetValue = if (isDragging) 1.12f else 1.0f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                label = "liquidScaleX"
            )

            // REAL LIQUID GLASS BUBBLE (Multi-layer Refraction & Specular Caustic Lens)
            Box(
                modifier = Modifier
                    .offset(x = bubbleOffset, y = 2.dp)
                    .width(bubbleWidth)
                    .height(bubbleHeight)
                    .scale(scaleX = liquidScaleX, scaleY = 1.0f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Inner Specular Liquid Light Dome
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .fillMaxWidth(0.7f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.50f))
                )
            }

            // Tab Items Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab

                    val (iconSelected, iconUnselected) = when (tab) {
                        NavigationTab.QUEUE -> Icons.Filled.Download to Icons.Outlined.Download
                        NavigationTab.SEARCH -> Icons.Filled.Search to Icons.Outlined.Search
                        NavigationTab.LIBRARY -> Icons.Filled.Folder to Icons.Outlined.Folder
                        NavigationTab.SETTINGS -> Icons.Filled.Tune to Icons.Outlined.Tune
                    }

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) palette.textPrimary else palette.textSecondary.copy(alpha = 0.65f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "iconColor"
                    )

                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) palette.textPrimary else palette.textSecondary.copy(alpha = 0.65f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "labelColor"
                    )

                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 0.94f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                        label = "itemScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(tabWidth)
                            .scale(itemScale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(vertical = 2.dp)
                    ) {
                        // Icon Area (Aligned over liquid bubble)
                        Box(
                            modifier = Modifier
                                .height(bubbleHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (tab == NavigationTab.QUEUE && activeQueueCount > 0) {
                                        Badge(
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary,
                                            modifier = Modifier.offset(x = 6.dp, y = (-2).dp)
                                        ) {
                                            Text(
                                                text = "$activeQueueCount",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    } else if (tab == NavigationTab.LIBRARY && libraryCount > 0) {
                                        Badge(
                                            containerColor = palette.surfaceVariant,
                                            contentColor = palette.textSecondary,
                                            modifier = Modifier.offset(x = 6.dp, y = (-2).dp)
                                        ) {
                                            Text(
                                                text = "$libraryCount",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) iconSelected else iconUnselected,
                                    contentDescription = tab.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Tab Label
                        Text(
                            text = tab.label,
                            color = labelColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
