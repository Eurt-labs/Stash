package com.eurtlabs.stash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    // Floating Glass Island Nav
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Liquid Glass Island — WebGL shader refraction aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.surfaceVariant,
                            palette.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalWidth = maxWidth
                val tabs = NavigationTab.values()
                val tabCount = tabs.size
                val tabWidth = totalWidth / tabCount
                val bubbleWidth = (tabWidth - 4.dp).coerceAtLeast(48.dp)
                val bubbleHeight = 52.dp

                val targetOffset = tabWidth * currentTab.ordinal + (tabWidth - bubbleWidth) / 2

                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 600f),
                    label = "bubbleSpring"
                )

                // Liquid Glass Selection Bubble
                LiquidGlassPill(
                    isSelected = true,
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .offset(x = animatedOffset, y = 0.dp)
                        .width(bubbleWidth)
                        .height(bubbleHeight)
                ) { }

                // Tab Items Row
                Row(
                    modifier = Modifier.fillMaxWidth().height(bubbleHeight),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentTab == tab

                        val (iconSelected, iconUnselected) = when (tab) {
                            NavigationTab.DISCOVER -> Icons.Filled.Explore to Icons.Outlined.Explore
                            NavigationTab.SEARCH -> Icons.Filled.Search to Icons.Outlined.Search
                            NavigationTab.QUEUE -> Icons.Filled.Download to Icons.Outlined.Download
                            NavigationTab.LIBRARY -> Icons.Filled.Folder to Icons.Outlined.Folder
                            NavigationTab.SETTINGS -> Icons.Filled.Tune to Icons.Outlined.Tune
                        }

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) palette.primary else palette.textSecondary.copy(alpha = 0.70f),
                            animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "iconColor"
                        )

                        val labelColor by animateColorAsState(
                            targetValue = if (isSelected) palette.textPrimary else palette.textSecondary.copy(alpha = 0.70f),
                            animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "labelColor"
                        )

                        val itemScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 0.98f,
                            animationSpec = tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "itemScale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(tabWidth)
                                .height(bubbleHeight)
                                .scale(itemScale)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember(tab) { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(tab) }
                        ) {
                            // Icon Area
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
                                                fontSize = 9.5.sp
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
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) iconSelected else iconUnselected,
                                    contentDescription = tab.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Tab Label
                            Text(
                                text = tab.label,
                                color = labelColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
