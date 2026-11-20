package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.model.DownloadStatus
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.GlassBadgePill
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass
import com.example.ui.components.thumbnailResForKey
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette

@Composable
fun ShortsScreen(
    shorts: List<VideoItem>,
    activeVideoId: String?,
    isPlaying: Boolean,
    player: Player?,
    isChannelSubscribed: (String) -> Boolean,
    onPlayShort: (VideoItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onToggleDislike: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Shorts cached yet", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val initialPage = remember(activeVideoId, shorts) {
        shorts.indexOfFirst { it.id == activeVideoId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { shorts.size })
    var showCommentsFor by remember { mutableStateOf<VideoItem?>(null) }
    val commentsMap = remember { mutableStateListOf("Pure liquid glass vibes!", "Works smoothly even offline in 4K!") }
    val context = LocalContext.current

    var lastTriggeredShortId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage, shorts) {
        val current = shorts.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (current.id != lastTriggeredShortId) {
            lastTriggeredShortId = current.id
            onPlayShort(current)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val item = shorts[page]
        val isCurrentPage = page == pagerState.currentPage
        val glass = LocalGlassPalette.current
        val subscribed = isChannelSubscribed(item.channelId)

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = thumbnailResForKey(item.thumbnailResKey)),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrentPage && player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.player = player
                        }
                    },
                    update = { view ->
                        if (view.player !== player) {
                            view.player = player
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .glowScaleClickable { onTogglePlayPause() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66000000),
                                Color.Transparent,
                                Color(0xCC05070E)
                            )
                        )
                    )
            )

            // Top Glass Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .liquidGlass(shape = RoundedCornerShape(20.dp), elevated = true)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Shorts • 4K Glass",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassBadgePill(
                        text = item.maxQuality,
                        backgroundColor = Color(0x99000000),
                        contentColor = glass.tapGlowPrimary
                    )
                    if (item.isCachedOffline || item.downloadStatus == DownloadStatus.COMPLETED) {
                        GlassBadgePill(
                            text = "OFFLINE",
                            backgroundColor = GlassEmerald.copy(alpha = 0.85f),
                            contentColor = Color.White
                        )
                    }
                }
            }

            // Center Play/Pause Tap Badge
            if (isCurrentPage && !isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .liquidGlass(shape = CircleShape, glowHighlighted = true)
                        .glowScaleClickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play Short",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Right Action Rail
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShortRailButton(
                    icon = Icons.Rounded.ThumbUp,
                    label = formatCount(item.likesCount),
                    active = item.isLiked,
                    onClick = { onToggleLike(item) },
                    testTag = "short_like_${item.id}"
                )
                ShortRailButton(
                    icon = Icons.Rounded.ThumbDown,
                    label = "Dislike",
                    active = item.isDisliked,
                    onClick = { onToggleDislike(item) },
                    testTag = "short_dislike_${item.id}"
                )
                ShortRailButton(
                    icon = Icons.Rounded.Comment,
                    label = "${commentsMap.size}",
                    active = false,
                    onClick = { showCommentsFor = item },
                    testTag = "short_comment_${item.id}"
                )
                ShortRailButton(
                    icon = if (item.downloadStatus == DownloadStatus.COMPLETED)
                        Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                    label = if (item.downloadStatus == DownloadStatus.COMPLETED) "4K Saved" else "4K DL",
                    active = item.downloadStatus == DownloadStatus.COMPLETED,
                    onClick = { onDownloadClick(item) },
                    testTag = "short_download_${item.id}"
                )
                ShortRailButton(
                    icon = if (item.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    label = if (item.isSaved) "Saved" else "Save",
                    active = item.isSaved,
                    onClick = { onToggleSave(item) },
                    testTag = "short_save_${item.id}"
                )
                ShortRailButton(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    active = false,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Watch ${item.title} on GlassTube (Ad-Free 4K)")
                        }
                        context.startActivity(Intent.createChooser(send, "Share Short"))
                    },
                    testTag = "short_share_${item.id}"
                )
                ShortRailButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    label = if (isPlaying) "Pause" else "Play",
                    active = isPlaying,
                    onClick = onTogglePlayPause,
                    testTag = "short_play_pause"
                )
            }

            // Bottom Creator Info + Subscribe Pill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.76f)
                    .padding(start = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelAvatar(
                        name = item.channelName,
                        seedColor = glass.tapGlowPrimary,
                        sizeDp = 36
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.channelHandle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (subscribed) Color(0x66FFFFFF)
                                else MaterialTheme.colorScheme.primary
                            )
                            .glowScaleClickable { onToggleSubscribe(item.channelId) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("short_subscribe_${item.channelId}")
                    ) {
                        Text(
                            text = if (subscribed) "Subscribed" else "Subscribe",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${item.category} • ${item.viewCountText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }

    if (showCommentsFor != null) {
        var newComment by remember { mutableStateOf("") }
        LiquidGlassDialog(onDismissRequest = { showCommentsFor = null }) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                commentsMap.forEach { c ->
                    Text(
                        text = "• $c",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    placeholder = { Text("Add a glowing comment...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showCommentsFor = null }) {
                    Text("Close")
                }
                TextButton(
                    onClick = {
                        if (newComment.isNotBlank()) {
                            commentsMap.add(newComment.trim())
                        }
                        showCommentsFor = null
                    }
                ) {
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShortRailButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .glowScaleClickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .liquidGlass(shape = CircleShape, glowHighlighted = active),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}
