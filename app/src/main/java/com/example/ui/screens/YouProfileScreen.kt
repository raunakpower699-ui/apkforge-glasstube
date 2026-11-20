package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.network.StorageInspector
import com.example.model.DownloadStatus
import com.example.model.StorageStats
import com.example.model.SyncBannerState
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.GlassBadgePill
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass
import com.example.ui.components.thumbnailResForKey
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette

@Composable
fun YouProfileScreen(
    displayName: String,
    email: String,
    syncState: SyncBannerState,
    storageStats: StorageStats,
    offlineVideos: List<VideoItem>,
    watchHistory: List<VideoItem>,
    likedVideos: List<VideoItem>,
    savedVideos: List<VideoItem>,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onRemoveDownload: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(22.dp), elevated = true)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .liquidGlass(shape = CircleShape)
                        .testTag("you_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Open Settings"
                    )
                }
            }
        }

        // Google/YouTube Account & Sync Card
        item {
            LiquidGlassBox(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = 16.dp,
                glowHighlighted = true,
                elevated = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(
                            name = displayName.ifBlank { "G" },
                            seedColor = MaterialTheme.colorScheme.primary,
                            sizeDp = 56
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName.ifBlank { "GlassTube Viewer" },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = email.ifBlank { "Google Account Connected" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        SyncStatusPill(syncState = syncState, onSyncClick = onSyncNow)
                    }

                    Text(
                        text = if (syncState.isOnline) {
                            "Connected • Subscriptions, likes & watch tastes sync automatically."
                        } else {
                            "Offline • ${syncState.pendingCount} action(s) saved locally and will sync once internet returns."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onSyncNow,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("you_sync_now_button")
                        ) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("you_sign_out_button")
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // Storage Summary Pill Card
        item {
            LiquidGlassBox(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = 14.dp,
                onClick = onOpenSettings
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Offline Vault Storage",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${StorageInspector.formatBytes(storageStats.totalAppBytes)} used",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.tapGlowPrimary
                        )
                    }
                    val totalPool = (storageStats.totalAppBytes + storageStats.availableDeviceBytes)
                        .coerceAtLeast(1L)
                    val fraction = (storageStats.totalAppBytes.toFloat() / totalPool.toFloat())
                        .coerceIn(0.03f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "${storageStats.downloadedFileCount} offline item(s) • ${StorageInspector.formatBytes(storageStats.availableDeviceBytes)} free on device • Tap for Storage Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Saved & Liked Summary Row
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Rounded.ThumbUp,
                    title = "Liked Videos",
                    count = "${likedVideos.size} clips",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { likedVideos.firstOrNull()?.let(onPlayVideo) },
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    icon = Icons.Rounded.Bookmark,
                    title = "Saved Library",
                    count = "${savedVideos.size} saved",
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = { savedVideos.firstOrNull()?.let(onPlayVideo) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Watch History Horizontal Strip
        if (watchHistory.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch History",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(watchHistory, key = { "hist_${it.id}" }) { video ->
                            CompactVideoTile(
                                video = video,
                                onClick = { onPlayVideo(video) }
                            )
                        }
                    }
                }
            }
        }

        // Offline Vault (4K Downloads)
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    tint = GlassEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Offline Vault (4K & Audio Downloads)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (offlineVideos.isEmpty()) {
            item {
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                    contentPadding = 18.dp
                ) {
                    Text(
                        text = "No downloads yet. Tap Download 4K on any video to save it for offline viewing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(offlineVideos, key = { "dl_${it.id}" }) { video ->
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(18.dp))
                        .glowScaleClickable { onPlayVideo(video) }
                        .padding(10.dp)
                        .testTag("offline_vault_item_${video.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(116.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassBadgePill(
                                text = video.downloadedQuality?.badge ?: "OFFLINE",
                                backgroundColor = GlassEmerald.copy(alpha = 0.85f),
                                contentColor = Color.White
                            )
                            Text(
                                text = if (video.downloadStatus == DownloadStatus.DOWNLOADING)
                                    "${video.downloadProgress}%"
                                else StorageInspector.formatBytes(video.fileSizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { onRemoveDownload(video) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_download_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove offline download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassBox(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = 14.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = count,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactVideoTile(
    video: VideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .liquidGlass(shape = RoundedCornerShape(16.dp))
            .glowScaleClickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = video.channelName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
