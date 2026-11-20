package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DownloadStatus
import com.example.model.MainTab
import com.example.model.SyncBannerState
import com.example.model.VideoItem
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette
import java.util.Locale

@Composable
fun GlassVideoCard(
    video: VideoItem,
    onPlay: () -> Unit,
    onDownloadClick: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    LiquidGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}"),
        cornerRadius = 24.dp,
        contentPadding = 10.dp,
        elevated = true,
        onClick = onPlay
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = glass.glassBorderHighlight.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color(0x5506080F)
                                )
                            )
                        )
                )

                // Top-left quality & offline status pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    GlassBadgePill(
                        text = video.maxQuality,
                        backgroundColor = Color(0x9906080F),
                        contentColor = glass.tapGlowPrimary
                    )
                    if (video.isCachedOffline || video.downloadStatus == DownloadStatus.COMPLETED) {
                        GlassBadgePill(
                            text = video.downloadedQuality?.badge?.let { "OFFLINE $it" } ?: "OFFLINE",
                            backgroundColor = GlassEmerald.copy(alpha = 0.85f),
                            contentColor = Color.White
                        )
                    } else if (video.downloadStatus == DownloadStatus.DOWNLOADING) {
                        GlassBadgePill(
                            text = "DL ${video.downloadProgress}%",
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                            contentColor = Color.White
                        )
                    }
                }

                // Bottom-right duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xAA06080F))
                        .border(0.9.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChannelAvatar(
                    name = video.channelName,
                    seedColor = Color(0xFF22D3EE),
                    sizeDp = 40
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${video.channelName} • ${video.viewCountText} • ${video.publishedAgo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_menu_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Video options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (video.downloadStatus == DownloadStatus.COMPLETED)
                                        "Downloaded (${video.downloadedQuality?.badge ?: "4K"})"
                                    else "Download 4K / Offline"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (video.downloadStatus == DownloadStatus.COMPLETED)
                                        Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDownloadClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (video.isSaved) "Remove from Saved" else "Save to Library") },
                            onClick = {
                                menuExpanded = false
                                onToggleSave()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (video.isLiked) "Unlike Video" else "Like Video") },
                            onClick = {
                                menuExpanded = false
                                onToggleLike()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassShortPreviewCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    Box(
        modifier = modifier
            .aspectRatio(9f / 15f)
            .liquidGlass(shape = RoundedCornerShape(18.dp))
            .glowScaleClickable(onClick = onClick)
            .testTag("short_card_${video.id}")
    ) {
        Image(
            painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x22000000), Color(0xCC06080F))
                    )
                )
        )
        if (video.isCachedOffline) {
            Icon(
                imageVector = Icons.Rounded.OfflinePin,
                contentDescription = "Offline ready",
                tint = glass.tapGlowPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.viewCountText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
fun GlassBadgePill(
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        backgroundColor.copy(alpha = (backgroundColor.alpha + 0.1f).coerceAtMost(1f)),
                        backgroundColor.copy(alpha = (backgroundColor.alpha - 0.12f).coerceAtLeast(0.4f))
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChannelAvatar(
    name: String,
    seedColor: Color,
    sizeDp: Int = 40
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(seedColor, seedColor.copy(alpha = 0.55f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SyncStatusPill(
    syncState: SyncBannerState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when {
        syncState.isSyncing -> Icons.Rounded.CloudSync
        !syncState.isOnline -> Icons.Rounded.CloudOff
        else -> Icons.Rounded.CloudDone
    }
    val tint = when {
        syncState.isSyncing -> MaterialTheme.colorScheme.secondary
        !syncState.isOnline -> Color(0xFFF59E0B)
        else -> GlassEmerald
    }
    val label = when {
        syncState.isSyncing -> "Syncing..."
        !syncState.isOnline && syncState.pendingCount > 0 ->
            "Offline • ${syncState.pendingCount} queued"
        !syncState.isOnline -> "Offline Mode"
        syncState.pendingCount > 0 -> "Sync (${syncState.pendingCount})"
        else -> "Synced"
    }

    Row(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(20.dp))
            .glowScaleClickable(onClick = onSyncClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("sync_status_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (syncState.isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = tint,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LiquidGlassBottomBar(
    selectedTab: MainTab,
    hasNewSubscriptions: Boolean,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(30.dp), elevated = true)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                if (tab == MainTab.CREATE) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        glass.tapGlowPrimary.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .glowScaleClickable { onTabSelected(tab) }
                            .testTag("nav_tab_create"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create or import video",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tabTint"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .then(
                                if (selected) Modifier.liquidGlass(
                                    shape = RoundedCornerShape(22.dp),
                                    glowHighlighted = true
                                )
                                else Modifier.clip(RoundedCornerShape(22.dp))
                            )
                            .glowScaleClickable { onTabSelected(tab) }
                            .padding(vertical = 6.dp)
                            .scale(scale)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Box {
                            val icon = when (tab) {
                                MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                MainTab.SHORTS -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
                                MainTab.SUBSCRIPTIONS ->
                                    if (selected) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions
                                MainTab.YOU -> Icons.Rounded.Person
                                MainTab.CREATE -> Icons.Rounded.Add
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            if (tab == MainTab.SUBSCRIPTIONS && hasNewSubscriptions) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}
