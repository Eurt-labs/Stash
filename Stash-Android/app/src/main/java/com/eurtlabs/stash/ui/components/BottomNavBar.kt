package com.eurtlabs.stash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

    // Glassmorphic Nav Shell
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.surface.copy(alpha = 0.92f),
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
                            Color.White.copy(alpha = 0.03f),
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = NavigationTab.values().size
            val tabWidth = totalWidth / tabCount
            val bubbleWidth = 64.dp
            val bubbleHeight = 32.dp

            val targetBubbleOffset = tabWidth * currentTab.ordinal + (tabWidth - bubbleWidth) / 2

            // Liquid Sliding Glass Bubble with smooth spring physics (no lag)
            val bubbleOffset by animateDpAsState(
                targetValue = targetBubbleOffset,
                animationSpec = spring(
                    dampingRatio = 0.76f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "liquidBubbleOffset"
            )

            // Pure Liquid Glass Bubble (Flawless glassmorphic refraction)
            Box(
                modifier = Modifier
                    .offset(x = bubbleOffset, y = 2.dp)
                    .width(bubbleWidth)
                    .height(bubbleHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
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
                        targetValue = if (isSelected) palette.textPrimary else palette.textSecondary.copy(alpha = 0.60f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "iconColor"
                    )

                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) palette.textPrimary else palette.textSecondary.copy(alpha = 0.60f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "labelColor"
                    )

                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 0.95f,
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
