package com.eurtlabs.stash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .navigationBarsPadding()
    ) {
        // Subtle top border divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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

                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 60.dp else 44.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "pillWidth"
                )

                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) palette.pillActive else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "pillColor"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) palette.pillOnActive else palette.textSecondary,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "iconColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(vertical = 4.dp)
                ) {
                    // Pill Icon Container
                    Box(
                        modifier = Modifier
                            .width(pillWidth)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillColor),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (tab == NavigationTab.QUEUE && activeQueueCount > 0) {
                                    Badge(
                                        containerColor = palette.success,
                                        contentColor = Color.Black
                                    ) {
                                        Text(
                                            text = "$activeQueueCount",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                } else if (tab == NavigationTab.LIBRARY && libraryCount > 0 && !isSelected) {
                                    Badge(
                                        containerColor = palette.surfaceVariant,
                                        contentColor = palette.textSecondary
                                    ) {
                                        Text(
                                            text = "$libraryCount",
                                            fontSize = 9.sp
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tab.label,
                        color = if (isSelected) palette.textPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
