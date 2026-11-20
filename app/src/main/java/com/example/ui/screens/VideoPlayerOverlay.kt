package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.DownloadStatus
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.GlassBadgePill
import com.example.ui.components.GlassVideoCard
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.formatDuration
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass
import com.example.ui.components.thumbnailResForKey
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette
import kotlinx.coroutines.delay

private val StreamQualities = listOf("4K 2160p", "1440p", "1080p", "720p", "Audio Only")

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    video: VideoItem,
    relatedVideos: List<VideoItem>,
    player: Player?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasRenderedFirstFrame: Boolean,
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    selectedQuality: String,
    statusNote: String?,
    isSubscribed: Boolean,
    onMinimizeToIsland: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onSelectQuality: (String) -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    onToggleDislike: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onPlayRelated: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onMinimizeToIsland() }
    val glass = LocalGlassPalette.current
    val context = LocalContext.current
    var qualityMenuOpen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var zoomToFill by remember { mutableStateOf(false) }

    val effectiveDuration = durationMs.coerceAtLeast(video.durationSec * 1000L).coerceAtLeast(1000L)
    val liveFraction = (positionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isScrubbing) scrubFraction else liveFraction
    val bufferedFraction = (bufferedPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val displayPositionMs = (displayFraction * effectiveDuration).toLong()

    // Auto-hide custom Liquid Glass HUD controls smoothly after 3.5s when playing
    LaunchedEffect(controlsVisible, isPlaying, isScrubbing, qualityMenuOpen, video.id) {
        if (controlsVisible && isPlaying && !isScrubbing && !qualityMenuOpen) {
            delay(3500L)
            controlsVisible = false
        }
    }

    LiquidGlassSurface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Media3 ExoPlayer Viewport + Custom Liquid Glass HUD Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        controlsVisible = !controlsVisible
                    }
            ) {
                if (player != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                this.player = player
                                this.keepScreenOn = isPlaying
                            }
                        },
                        update = { view ->
                            if (view.player !== player) {
                                view.player = player
                            }
                            view.keepScreenOn = isPlaying
                            view.resizeMode = if (zoomToFill) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Poster art shown until ExoPlayer renders its first video frame
                androidx.compose.animation.AnimatedVisibility(
                    visible = player == null || !hasRenderedFirstFrame,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = thumbnailResForKey(video.thumbnailResKey)),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Custom Liquid Glass HUD Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible || !isPlaying || isBuffering,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x88050812),
                                        Color(0x22050812),
                                        Color(0x99050812)
                                    )
                                )
                            )
                    ) {
                        // Top Liquid Glass Bar (Minimize, Status, Speed, Resize, Quality)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onMinimizeToIsland,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .liquidGlass(shape = CircleShape)
                                    .testTag("minimize_to_island_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_minimize_island),
                                    tint = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (statusNote != null) {
                                    GlassBadgePill(
                                        text = statusNote,
                                        backgroundColor = GlassEmerald.copy(alpha = 0.85f),
                                        contentColor = Color.White
                                    )
                                }

                                // Playback Speed Glass Pill
                                Row(
                                    modifier = Modifier
                                        .liquidGlass(
                                            shape = RoundedCornerShape(14.dp),
                                            glowHighlighted = playbackSpeed != 1.0f
                                        )
                                        .glowScaleClickable { onCycleSpeed() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("player_speed_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = "Playback speed",
                                        tint = glass.tapGlowPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Aspect Ratio Fit / Fill Glass Pill
                                Box(
                                    modifier = Modifier
                                        .liquidGlass(
                                            shape = RoundedCornerShape(14.dp),
                                            glowHighlighted = zoomToFill
                                        )
                                        .glowScaleClickable { zoomToFill = !zoomToFill }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("player_resize_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = if (zoomToFill) "Fit video" else "Fill screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Quality Selector Pill
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .liquidGlass(shape = RoundedCornerShape(14.dp), glowHighlighted = true)
                                            .glowScaleClickable { qualityMenuOpen = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("player_quality_button"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.HighQuality,
                                            contentDescription = "Select quality",
                                            tint = glass.tapGlowPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = selectedQuality,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = qualityMenuOpen,
                                        onDismissRequest = { qualityMenuOpen = false }
                                    ) {
                                        StreamQualities.forEach { q ->
                                            DropdownMenuItem(
                                                text = { Text(q) },
                                                onClick = {
                                                    qualityMenuOpen = false
                                                    onSelectQuality(q)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Center Play / Pause / Seek / Buffering Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(26.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSeekBy(-10_000L) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(50.dp)
                                    .liquidGlass(shape = CircleShape, elevated = true)
                                    .testTag("player_rewind_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White
                                )
                            }

                            Box(contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier
                                        .size(66.dp)
                                        .liquidGlass(
                                            shape = CircleShape,
                                            glowHighlighted = true,
                                            elevated = true
                                        )
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.68f),
                                            CircleShape
                                        )
                                        .testTag("player_play_pause_button")
                                ) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.6.dp,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_play_pause),
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onSeekBy(10_000L) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(50.dp)
                                    .liquidGlass(shape = CircleShape, elevated = true)
                                    .testTag("player_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White
                                )
                            }
                        }

                        // Scrub Preview Timestamp Pill
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isScrubbing,
                            enter = fadeIn() + scaleIn(initialScale = 0.9f),
                            exit = fadeOut() + scaleOut(targetScale = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 58.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(shape = RoundedCornerShape(14.dp), glowHighlighted = true)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${formatDuration((displayPositionMs / 1000L).toInt())} / ${formatDuration((effectiveDuration / 1000L).toInt())}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bottom Custom Liquid Glass Seek Bar Capsule
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .liquidGlass(shape = RoundedCornerShape(20.dp), elevated = true)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatDuration((displayPositionMs / 1000L).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )

                            LiquidGlassSeekBar(
                                progress = displayFraction,
                                bufferedProgress = bufferedFraction,
                                isScrubbing = isScrubbing,
                                onScrubStart = { fraction ->
                                    isScrubbing = true
                                    scrubFraction = fraction
                                },
                                onScrubChange = { fraction ->
                                    scrubFraction = fraction
                                },
                                onScrubEnd = { fraction ->
                                    isScrubbing = false
                                    onSeekTo((fraction * effectiveDuration).toLong())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_seek_bar")
                            )

                            Text(
                                text = formatDuration((effectiveDuration / 1000L).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Scrollable Details & Related Feed
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${video.viewCountText} • ${video.publishedAgo} • ${video.category} • Ad-Free",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Pill Rail
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            PlayerActionChip(
                                icon = Icons.Rounded.ThumbUp,
                                label = "${video.likesCount}",
                                highlighted = video.isLiked,
                                onClick = { onToggleLike(video) },
                                testTag = "player_like_button"
                            )
                        }
                        item {
                            PlayerActionChip(
                                icon = Icons.Rounded.ThumbDown,
                                label = "Dislike",
                                highlighted = video.isDisliked,
                                onClick = { onToggleDislike(video) },
                                testTag = "player_dislike_button"
                            )
                        }
                        item {
                            PlayerActionChip(
                                icon = if (video.downloadStatus == DownloadStatus.COMPLETED)
                                    Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                label = if (video.downloadStatus == DownloadStatus.COMPLETED)
                                    "Offline (${video.downloadedQuality?.badge ?: "4K"})"
                                else "Download 4K",
                                highlighted = video.downloadStatus == DownloadStatus.COMPLETED,
                                onClick = { onDownloadClick(video) },
                                testTag = "player_download_button"
                            )
                        }
                        item {
                            PlayerActionChip(
                                icon = Icons.Rounded.Headphones,
                                label = "Background Island",
                                highlighted = false,
                                onClick = onMinimizeToIsland,
                                testTag = "player_background_island_button"
                            )
                        }
                        item {
                            PlayerActionChip(
                                icon = if (video.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                label = if (video.isSaved) "Saved" else "Save",
                                highlighted = video.isSaved,
                                onClick = { onToggleSave(video) },
                                testTag = "player_save_button"
                            )
                        }
                        item {
                            PlayerActionChip(
                                icon = Icons.Rounded.Share,
                                label = "Share",
                                highlighted = false,
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Watch ${video.title} ad-free on GlassTube")
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share Video"))
                                },
                                testTag = "player_share_button"
                            )
                        }
                    }
                }

                // Channel Subscribe Card
                item {
                    LiquidGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = 12.dp,
                        elevated = true
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChannelAvatar(
                                name = video.channelName,
                                seedColor = MaterialTheme.colorScheme.secondary,
                                sizeDp = 42
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.channelName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = video.channelHandle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSubscribed) {
                                OutlinedButton(
                                    onClick = { onToggleSubscribe(video.channelId) },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("player_subscribe_button")
                                ) {
                                    Text("Subscribed")
                                }
                            } else {
                                Button(
                                    onClick = { onToggleSubscribe(video.channelId) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("player_subscribe_button")
                                ) {
                                    Text("Subscribe", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Up Next • Related to ${video.category}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(relatedVideos, key = { "rel_${it.id}" }) { rel ->
                    GlassVideoCard(
                        video = rel,
                        onPlay = { onPlayRelated(rel) },
                        onDownloadClick = { onDownloadClick(rel) },
                        onToggleSave = { onToggleSave(rel) },
                        onToggleLike = { onToggleLike(rel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassSeekBar(
    progress: Float,
    bufferedProgress: Float,
    isScrubbing: Boolean,
    onScrubStart: (Float) -> Unit,
    onScrubChange: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var currentDragFraction by remember { mutableFloatStateOf(progress) }

    Box(
        modifier = modifier
            .height(30.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                    onScrubEnd(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        currentDragFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        onScrubStart(currentDragFraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentDragFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        onScrubChange(currentDragFraction)
                    },
                    onDragEnd = {
                        onScrubEnd(currentDragFraction)
                    },
                    onDragCancel = {
                        onScrubEnd(currentDragFraction)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = if (isScrubbing) 7.dp.toPx() else 5.dp.toPx()
            val topY = (size.height - trackHeight) / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // Frosted glass track background
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(0f, topY),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // Buffered progress indicator
            val bufferedWidth = (size.width * bufferedProgress.coerceIn(0f, 1f))
            if (bufferedWidth > 0f) {
                drawRoundRect(
                    color = glass.tapGlowPrimary.copy(alpha = 0.34f),
                    topLeft = Offset(0f, topY),
                    size = Size(bufferedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Active luminous playback track
            val activeWidth = (size.width * progress.coerceIn(0f, 1f))
            if (activeWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(glass.tapGlowSecondary, glass.tapGlowPrimary)
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Glowing crystal thumb
            val thumbCenter = Offset(activeWidth.coerceIn(0f, size.width), size.height / 2f)
            val haloRadius = if (isScrubbing) 14.dp.toPx() else 10.dp.toPx()
            val thumbRadius = if (isScrubbing) 8.5.dp.toPx() else 6.5.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.tapGlowPrimary.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = thumbCenter,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = glass.tapGlowPrimary,
                radius = thumbRadius * 0.45f,
                center = thumbCenter
            )
        }
    }
}

@Composable
private fun PlayerActionChip(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .liquidGlass(shape = RoundedCornerShape(20.dp), glowHighlighted = highlighted)
            .glowScaleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
