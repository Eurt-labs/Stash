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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.model.NavigationTab
import com.eurtlabs.stash.ui.theme.LocalStashPalette

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

    var dragXOffset by remember { mutableStateOf<Float?>(null) }

    // Floating Glass Island Nav
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Frosted Glass Island Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.surface.copy(alpha = 0.90f),
                            palette.surfaceVariant.copy(alpha = 0.82f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragXOffset = offset.x
                            },
                            onDragEnd = {
                                val currentX = dragXOffset
                                if (currentX != null) {
                                    val totalWidthPx = size.width.toFloat()
                                    val tabWidthPx = totalWidthPx / NavigationTab.values().size
                                    val selectedIndex = (currentX / tabWidthPx).toInt().coerceIn(0, NavigationTab.values().size - 1)
                                    onTabSelected(NavigationTab.values()[selectedIndex])
                                }
                                dragXOffset = null
                            },
                            onDragCancel = {
                                dragXOffset = null
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val current = dragXOffset ?: change.position.x
                                dragXOffset = (current + dragAmount).coerceIn(0f, size.width.toFloat())
                            }
                        )
                    }
            ) {
                val totalWidth = maxWidth
                val tabCount = NavigationTab.values().size
                val tabWidth = totalWidth / tabCount
                val bubbleWidth = (tabWidth - 6.dp).coerceAtLeast(54.dp)
                val bubbleHeight = 36.dp

                val defaultOffset = tabWidth * currentTab.ordinal + (tabWidth - bubbleWidth) / 2
                val animatedOffset by animateDpAsState(
                    targetValue = defaultOffset,
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = 450f
                    ),
                    label = "bubbleSpring"
                )

                val currentBubbleOffset = if (dragXOffset != null) {
                    val dragDp = with(density) { dragXOffset!!.toDp() }
                    (dragDp - bubbleWidth / 2).coerceIn(0.dp, totalWidth - bubbleWidth)
                } else {
                    animatedOffset
                }

                val isDragging = dragXOffset != null
                val liquidStretchX by animateFloatAsState(
                    targetValue = if (isDragging) 1.14f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    label = "liquidStretch"
                )

                // Refractive Liquid Glass Bubble with Ambient Glow
                Box(
                    modifier = Modifier
                        .offset(x = currentBubbleOffset, y = 2.dp)
                        .width(bubbleWidth)
                        .height(bubbleHeight)
                        .scale(scaleX = liquidStretchX, scaleY = 1.0f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.70f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )

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
                            targetValue = if (isSelected) 1.05f else 0.95f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
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
                            // Icon Area
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

                            Spacer(modifier = Modifier.height(2.dp))

                            // Tab Label
                            Text(
                                text = tab.label,
                                color = labelColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
