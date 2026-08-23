package com.eurtlabs.stash.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurtlabs.stash.data.downloader.LogManager
import com.eurtlabs.stash.data.downloader.YoutubeDLManager
import com.eurtlabs.stash.data.model.ColorTheme
import com.eurtlabs.stash.data.model.DownloadFormat
import com.eurtlabs.stash.data.model.DownloadQuality
import com.eurtlabs.stash.data.model.MediaType
import com.eurtlabs.stash.data.model.StashSettings
import com.eurtlabs.stash.ui.components.LiquidGlassCard
import com.eurtlabs.stash.ui.components.LiquidGlassPill
import com.eurtlabs.stash.ui.theme.LocalStashPalette
import com.eurtlabs.stash.ui.theme.getThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlin.math.roundToInt

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.eurtlabs.stash.data.downloader.CookieManager

@Composable
fun <T> AnimatedSelectorTab(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    var itemLayouts by remember { mutableStateOf(mapOf<T, Pair<Float, Float>>()) }
    var dragXOffset by remember { mutableStateOf<Float?>(null) }
    val scrollState = rememberScrollState()
    var viewportWidth by remember { mutableStateOf(0f) }
    
    val selectedLayout = itemLayouts[selectedItem] ?: Pair(0f, 0f)
    val targetOffset = selectedLayout.first
    val targetWidth = selectedLayout.second
    
    val dragTargetOffset = dragXOffset?.coerceIn(0f, itemLayouts.values.maxOfOrNull { it.first } ?: 0f) ?: targetOffset
    
    // Find closest item for width interpolation during drag
    val closestItemEntry = if (dragXOffset != null) {
        val dragCenter = (dragXOffset ?: 0f) + targetWidth / 2
        itemLayouts.minByOrNull { Math.abs(it.value.first + it.value.second / 2 - dragCenter) }
    } else null
    val dragTargetWidth = closestItemEntry?.value?.second ?: targetWidth
    
    val animatedOffset by animateFloatAsState(
        targetValue = dragTargetOffset,
        animationSpec = if (dragXOffset != null) {
            androidx.compose.animation.core.spring(stiffness = 1000f, dampingRatio = 1f)
        } else {
            tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        },
        label = "offset"
    )
    val animatedWidth by animateFloatAsState(
        targetValue = dragTargetWidth,
        animationSpec = if (dragXOffset != null) {
            androidx.compose.animation.core.spring(stiffness = 1000f, dampingRatio = 1f)
        } else {
            tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        },
        label = "width"
    )

    // Calculate squish effect
    val density = androidx.compose.ui.platform.LocalDensity.current
    val distanceToTarget = Math.abs(animatedOffset - dragTargetOffset)
    val isDragging = dragXOffset != null
    val isTraveling = distanceToTarget > 2f && !isDragging
    
    // Continuous Auto-scroll Effect during Drag
    androidx.compose.runtime.LaunchedEffect(isDragging, viewportWidth) {
        if (isDragging && viewportWidth > 0f) {
            while (true) {
                val currentDragX = dragXOffset ?: break
                
                val scrollPos = scrollState.value.toFloat()
                val visibleLeft = scrollPos
                val visibleRight = scrollPos + viewportWidth
                val bubbleLeft = currentDragX
                val bubbleRight = bubbleLeft + targetWidth
                val edgeThreshold = 80f
                
                var scrollDelta = 0f
                if (bubbleRight > visibleRight - edgeThreshold) {
                    val depth = (bubbleRight - (visibleRight - edgeThreshold)) / edgeThreshold
                    scrollDelta = 12f * depth.coerceIn(0.1f, 1f)
                } else if (bubbleLeft < visibleLeft + edgeThreshold) {
                    val depth = ((visibleLeft + edgeThreshold) - bubbleLeft) / edgeThreshold
                    scrollDelta = -12f * depth.coerceIn(0.1f, 1f)
                }
                
                if (scrollDelta != 0f) {
                    scrollState.dispatchRawDelta(scrollDelta)
                    dragXOffset = (currentDragX + scrollDelta).coerceIn(0f, itemLayouts.values.maxOfOrNull { it.first } ?: 0f)
                }
                
                kotlinx.coroutines.delay(16)
            }
        }
    }
    
    val targetScaleX = if (isDragging) 1.05f else if (isTraveling) 1.15f else 1.0f
    val targetScaleY = if (isDragging) 0.95f else if (isTraveling) 0.85f else 1.0f
    
    val liquidStretchX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "stretchX"
    )
    val liquidShrinkY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "shrinkY"
    )

    val palette = com.eurtlabs.stash.ui.theme.LocalStashPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { 
                val w = it.size.width.toFloat()
                if (viewportWidth != w) viewportWidth = w
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.surfaceVariant,
                        palette.surface
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))),
                RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                if (dragTargetWidth > 0f) {
                    LiquidGlassPill(
                        isSelected = true,
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(animatedOffset.roundToInt(), 0) }
                            .width(with(density) { animatedWidth.toDp() })
                            .height(38.dp)
                            .scale(scaleX = liquidStretchX, scaleY = liquidShrinkY)
                    ) { }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = selectedItem == item
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 0.98f,
                            animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords -> 
                                    val current = itemLayouts[item]
                                    val nextX = coords.positionInParent().x
                                    val nextWidth = coords.size.width.toFloat()
                                    if (current?.first != nextX || current?.second != nextWidth) {
                                        itemLayouts = itemLayouts + (item to Pair(nextX, nextWidth))
                                    }
                                }
                                .height(38.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { dragXOffset = animatedOffset },
                                                onDragEnd = {
                                                    if (dragXOffset != null) {
                                                        val dragCenter = (dragXOffset ?: 0f) + targetWidth / 2
                                                        val closest = itemLayouts.minByOrNull { Math.abs(it.value.first + it.value.second / 2 - dragCenter) }?.key
                                                        if (closest != null) onItemSelected(closest)
                                                    }
                                                    dragXOffset = null
                                                },
                                                onDragCancel = { dragXOffset = null },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val current = dragXOffset ?: animatedOffset
                                                    dragXOffset = (current + dragAmount).coerceIn(0f, itemLayouts.values.maxOfOrNull { it.first } ?: 0f)
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                                .clickable { onItemSelected(item) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            itemContent(item, isSelected)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: StashSettings,
    onSelectMediaType: (MediaType) -> Unit,
    onSelectTheme: (ColorTheme) -> Unit,
    onSelectFormat: (DownloadFormat) -> Unit,
    onSelectQuality: (DownloadQuality) -> Unit,
    onChangeStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalStashPalette.current
    val context = LocalContext.current

    val currentMediaType = settings.mediaType
    val currentFormat = settings.format
    val currentQuality = settings.quality

    val isLosslessAudio = currentMediaType == MediaType.AUDIO && (currentFormat == DownloadFormat.FLAC || currentFormat == DownloadFormat.WAV)

    val availableFormats = if (currentMediaType == MediaType.AUDIO) {
        DownloadFormat.values().filter { it.isAudioOnly }
    } else {
        DownloadFormat.values().filter { !it.isAudioOnly }
    }

    val availableQualities = if (currentMediaType == MediaType.AUDIO) {
        DownloadQuality.values().filter { it.isAudioOnly }
    } else {
        DownloadQuality.values().filter { !it.isAudioOnly }
    }

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var appUpdateStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingAppUpdate by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf("Calculating...") }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sizeBytes = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                val sizeMb = sizeBytes / (1024f * 1024f)
                cacheSize = if (sizeMb >= 1f) String.format("%.1f MB", sizeMb) else String.format("%d KB", sizeBytes / 1024)
            } catch (e: Exception) {
                cacheSize = "Unknown"
            }
        }
    }

    var showLoginDialog by remember { mutableStateOf(false) }
    var cookieStatus by remember { mutableStateOf("Not synced") }

    LaunchedEffect(Unit) {
        val file = CookieManager.getCookiesFile(context)
        if (file.exists() && file.length() > 0) {
            cookieStatus = "Synced via WebView"
        }
    }

    if (showLoginDialog) {
        Dialog(
            onDismissRequest = { 
                showLoginDialog = false 
                CookieManager.extractCookiesToDisk(context)
                val file = CookieManager.getCookiesFile(context)
                cookieStatus = if (file.exists() && file.length() > 0) "Synced via WebView" else "Not synced"
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = palette.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("YouTube Login", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Done",
                            color = palette.primary,
                            modifier = Modifier.clickable {
                                showLoginDialog = false
                                CookieManager.extractCookiesToDisk(context)
                                val file = CookieManager.getCookiesFile(context)
                                cookieStatus = if (file.exists() && file.length() > 0) "Synced via WebView" else "Not synced"
                            }
                        )
                    }
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Automatically extract cookies silently on every page load
                                        CookieManager.extractCookiesToDisk(context)
                                    }
                                }
                                loadUrl("https://m.youtube.com")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp, top = 10.dp, start = 18.dp, end = 18.dp)
    ) {
        // Section: Media Mode Toggle with Compact Animated Floating Cloud Capsule
        item {
            Text(
                text = "DOWNLOAD MODE",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Compact Animated Cloud Liquid Glass Mode Track
            var modeDragXOffset by remember { mutableStateOf<Float?>(null) }
            
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.surfaceVariant,
                                palette.surface
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp)
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val halfWidth = maxWidth / 2
                val defaultTargetOffset = if (currentMediaType == MediaType.AUDIO) 0.dp else halfWidth
                
                val targetOffset = if (modeDragXOffset != null) {
                    val dragDp = with(density) { (modeDragXOffset ?: 0f).toDp() }
                    (dragDp - halfWidth / 2).coerceIn(0.dp, halfWidth)
                } else {
                    defaultTargetOffset
                }
                
                val bubbleOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = if (modeDragXOffset != null) {
                        androidx.compose.animation.core.spring(stiffness = 1000f, dampingRatio = 1f)
                    } else {
                        tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    },
                    label = "modeBubble"
                )

                // Calculate squish effect
                val distanceToTarget = with(density) { Math.abs(bubbleOffset.toPx() - targetOffset.toPx()) }
                val isDragging = modeDragXOffset != null
                val isTraveling = distanceToTarget > 2f && !isDragging
                
                val targetScaleX = if (isDragging) 1.05f else if (isTraveling) 1.15f else 1.0f
                val targetScaleY = if (isDragging) 0.95f else if (isTraveling) 0.85f else 1.0f
                
                val liquidStretchX by animateFloatAsState(
                    targetValue = targetScaleX,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "stretchX"
                )
                val liquidShrinkY by animateFloatAsState(
                    targetValue = targetScaleY,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "shrinkY"
                )

                // Floating Cloud Capsule (LiquidGlassPill for selection thumb)
                LiquidGlassPill(
                    isSelected = true,
                    cornerRadius = 20.dp,
                    modifier = Modifier
                        .offset(x = bubbleOffset)
                        .width(halfWidth)
                        .height(38.dp)
                        .scale(scaleX = liquidStretchX, scaleY = liquidShrinkY)
                ) {
                    // Empty body, just the glass pill
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MediaType.values().forEach { mode ->
                        val isSelected = currentMediaType == mode
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 0.98f,
                            animationSpec = tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { modeDragXOffset = with(density) { bubbleOffset.toPx() } },
                                                onDragEnd = {
                                                    if (modeDragXOffset != null) {
                                                        val totalWidth = size.width.toFloat()
                                                        val isRightSide = (modeDragXOffset ?: 0f) > totalWidth / 2
                                                        onSelectMediaType(if (isRightSide) MediaType.VIDEO else MediaType.AUDIO)
                                                    }
                                                    modeDragXOffset = null
                                                },
                                                onDragCancel = { modeDragXOffset = null },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val current = modeDragXOffset ?: with(density) { bubbleOffset.toPx() }
                                                    modeDragXOffset = (current + dragAmount).coerceIn(0f, size.width.toFloat())
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                                .clickable { onSelectMediaType(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) palette.primary else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Horizontal Format Selection (Sleek Compact Cloud Pills)
        item {
            SettingsSectionHeader(
                icon = if (currentMediaType == MediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Codec & Format" else "Video Container Format",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Select container for music downloads" else "Select container for video downloads"
            )
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedSelectorTab(
                items = availableFormats,
                selectedItem = currentFormat,
                onItemSelected = { onSelectFormat(it) }
            ) { format, isSelected ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = format.ext.uppercase(),
                        color = if (isSelected) palette.primary else palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (format == DownloadFormat.FLAC || format == DownloadFormat.WAV) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(palette.primary.copy(alpha = 0.20f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LOSSLESS",
                                color = palette.primary,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Quality / Bitrate (Sleek Compact Cloud Pills)
        item {
            SettingsSectionHeader(
                icon = Icons.Filled.Tune,
                title = if (currentMediaType == MediaType.AUDIO) "Audio Bitrate & Compression" else "Video Resolution Quality",
                subtitle = if (currentMediaType == MediaType.AUDIO) "Target bitrate for downloaded music" else "Target resolution for downloaded video stream"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLosslessAudio) {
                // FLAC / WAV: Compact Lossless Studio Audio Banner
                LiquidGlassCard(
                    cornerRadius = 18.dp,
                    innerPadding = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Lossless Studio Audio Active (${currentFormat.ext.uppercase()})",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "Bit-perfect uncompressed master stream. Lossy bitrate compression disabled.",
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                // Horizontal Compact Cloud Lossy Bitrates or Video Resolutions
                AnimatedSelectorTab(
                    items = availableQualities,
                    selectedItem = currentQuality,
                    onItemSelected = { onSelectQuality(it) }
                ) { quality, isSelected ->
                    val shortLabel = quality.label.substringBefore(" (")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = shortLabel,
                            color = if (isSelected) palette.primary else palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Color Theme (Compact Cloud Pill Badges)
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "App Color Theme",
                subtitle = "Select clean monochromatic or tinted palette"
            )
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedSelectorTab(
                items = ColorTheme.values().toList(),
                selectedItem = settings.theme,
                onItemSelected = { onSelectTheme(it) }
            ) { theme, isSelected ->
                val themePal = getThemePalette(theme)
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(themePal.primary)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(
                        text = theme.displayName,
                        color = themePal.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Storage Location (Compact Cloud Capsule)
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Folder,
                title = "Storage & Downloads Folder",
                subtitle = "Where completed audio and video files are saved"
            )
            Spacer(modifier = Modifier.height(8.dp))

            LiquidGlassCard(
                cornerRadius = 20.dp,
                innerPadding = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeStorage() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Location",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = settings.outputDir.ifEmpty { "Music / Stash" },
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceVariant)
                            .border(0.8.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Change",
                            color = palette.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        // Section: YouTube Account & Cookies
        item {
            SettingsSectionHeader(
                icon = Icons.Filled.VideoLibrary,
                title = "YouTube Account & Cookies",
                subtitle = "Sync valid browser session to completely bypass CAPTCHAs & Age Restrictions"
            )
            Spacer(modifier = Modifier.height(8.dp))

            LiquidGlassCard(
                cornerRadius = 20.dp,
                innerPadding = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surfaceVariant)
                            .clickable { showLoginDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Login / Sync Browser Cookies",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "Status: $cookieStatus",
                                color = if (cookieStatus.contains("Synced")) palette.primary else palette.textSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "Open WebView 🌐",
                            color = palette.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }

                    if (cookieStatus.contains("Synced")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.surfaceVariant)
                                .clickable {
                                    CookieManager.clearCookies(context)
                                    cookieStatus = "Not synced"
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Clear Saved Cookies",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Diagnostics, Engine & Logs
        item {
            SettingsSectionHeader(
                icon = Icons.Filled.Tune,
                title = "Engine & Diagnostics",
                subtitle = "Core yt-dlp & FFmpeg runtime status and logs"
            )
            Spacer(modifier = Modifier.height(8.dp))

            LiquidGlassCard(
                cornerRadius = 20.dp,
                innerPadding = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // Export Logs Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            LogManager.exportLogs(context)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Diagnostic Logs (.txt)",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Generate and attach full debug log file via Share Sheet",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = "Export 📤",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Update Engine Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            if (!isUpdating) {
                                isUpdating = true
                                updateStatus = "Updating engine..."
                                scope.launch(Dispatchers.IO) {
                                    val result = YoutubeDLManager.updateEngine(context)
                                    withContext(Dispatchers.Main) {
                                        updateStatus = result
                                        isUpdating = false
                                    }
                                }
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Core Engine (yt-dlp)",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = updateStatus ?: "Fetch latest binary updates from official yt-dlp GitHub release channel",
                            color = if (updateStatus != null) palette.primary else palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = if (isUpdating) "..." else "Update ↻",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Check App Update Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            if (!isCheckingAppUpdate) {
                                isCheckingAppUpdate = true
                                appUpdateStatus = "Checking GitHub..."
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val url = java.net.URL("https://api.github.com/repos/Eurt-labs/Stash/releases/latest")
                                        val connection = url.openConnection() as java.net.HttpURLConnection
                                        connection.requestMethod = "GET"
                                        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                                        
                                        if (connection.responseCode == 200) {
                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                                            val tagMatch = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(response)
                                            val urlMatch = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(response)
                                            
                                            val latestVersion = tagMatch?.groupValues?.get(1)
                                            val releaseUrl = urlMatch?.groupValues?.get(1)
                                            
                                            withContext(Dispatchers.Main) {
                                                if (latestVersion != null && releaseUrl != null) {
                                                    val currentVersion = try {
                                                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "v2.0.0"
                                                    } catch (e: Exception) { "v2.0.0" }
                                                    
                                                    val normLatest = latestVersion.removePrefix("v")
                                                    val normCurrent = currentVersion.removePrefix("v")
                                                    
                                                    if (normLatest != normCurrent) {
                                                        appUpdateStatus = "Update found: $latestVersion!"
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(releaseUrl))
                                                        context.startActivity(intent)
                                                    } else {
                                                        appUpdateStatus = "You are on the latest version ($currentVersion)"
                                                    }
                                                } else {
                                                    appUpdateStatus = "Could not parse release info"
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                appUpdateStatus = "No updates found (HTTP ${connection.responseCode})"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            appUpdateStatus = "Network error: ${e.message}"
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            isCheckingAppUpdate = false
                                        }
                                    }
                                }
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Check for App Updates",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = appUpdateStatus ?: "Check GitHub for new Stash App releases",
                            color = if (appUpdateStatus != null) palette.primary else palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = if (isCheckingAppUpdate) "..." else "Check",
                        color = palette.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Clear Cache Storage Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceVariant)
                        .clickable {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    context.cacheDir.mkdirs() // Recreate the directory just in case
                                    
                                    // Recalculate cache size
                                    val sizeBytes = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                                    val sizeMb = sizeBytes / (1024f * 1024f)
                                    val newSize = if (sizeMb >= 1f) String.format("%.1f MB", sizeMb) else String.format("%d KB", sizeBytes / 1024)
                                    
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        cacheSize = newSize
                                    }
                                } catch (e: Exception) {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        cacheSize = "Error"
                                    }
                                }
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Cache Storage",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Frees up space by deleting temporary thumbnails, logs, and interrupted download chunks ($cacheSize)",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    Text(
                        text = "Clear 🗑",
                        color = palette.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

            // Footer Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Developed by @DhruvSaraswat",
                    color = palette.textSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalStashPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(15.dp)
        )
        Column {
            Text(
                text = title,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}
