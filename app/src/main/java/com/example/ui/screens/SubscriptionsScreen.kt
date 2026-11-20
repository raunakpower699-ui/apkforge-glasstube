package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChannelItem
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.GlassVideoCard
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass

@Composable
fun SubscriptionsScreen(
    channels: List<ChannelItem>,
    videos: List<VideoItem>,
    onToggleSubscribe: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onDownloadVideo: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannelId by remember { mutableStateOf<String?>(null) }
    val subscribedChannels = remember(channels) { channels.filter { it.isSubscribed } }
    val subscribedIds = remember(subscribedChannels) {
        subscribedChannels.map { it.channelId }.toSet()
    }

    val filteredVideos = remember(videos, subscribedIds, selectedChannelId) {
        videos.filter { !it.isShort }
            .filter { video ->
                if (selectedChannelId != null) video.channelId == selectedChannelId
                else video.channelId in subscribedIds
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .liquidGlass(shape = RoundedCornerShape(22.dp), elevated = true)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val allSelected = selectedChannelId == null
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .glowScaleClickable { selectedChannelId = null }
                        .testTag("sub_filter_all")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .liquidGlass(shape = CircleShape, glowHighlighted = allSelected),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("All", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All (${subscribedChannels.size})", style = MaterialTheme.typography.labelSmall)
                }
            }

            items(channels, key = { it.channelId }) { channel ->
                val isSelected = selectedChannelId == channel.channelId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .glowScaleClickable {
                            selectedChannelId =
                                if (isSelected) null else channel.channelId
                        }
                        .testTag("sub_channel_chip_${channel.channelId}")
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(2.dp)
                        ) {
                            ChannelAvatar(
                                name = channel.channelName,
                                seedColor = Color(channel.avatarSeedColor),
                                sizeDp = 50
                            )
                        }
                        if (channel.hasNewUpload && channel.isSubscribed) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.channelName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (channel.isSubscribed) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 8.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedChannelId != null) {
                val selectedChannel = channels.firstOrNull { it.channelId == selectedChannelId }
                if (selectedChannel != null) {
                    item {
                        ChannelRowCard(
                            channel = selectedChannel,
                            onToggleSubscribe = { onToggleSubscribe(selectedChannel.channelId) }
                        )
                    }
                }
            }

            if (filteredVideos.isEmpty()) {
                item {
                    LiquidGlassBox(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        contentPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Manage Channels",
                                style = MaterialTheme.typography.titleMedium
                            )
                            channels.forEach { ch ->
                                ChannelRowCard(
                                    channel = ch,
                                    onToggleSubscribe = { onToggleSubscribe(ch.channelId) }
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredVideos, key = { it.id }) { video ->
                    GlassVideoCard(
                        video = video,
                        onPlay = { onPlayVideo(video) },
                        onDownloadClick = { onDownloadVideo(video) },
                        onToggleSave = { onToggleSave(video) },
                        onToggleLike = { onToggleLike(video) },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }

                item {
                    LiquidGlassBox(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        contentPadding = 14.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Discover & Manage Channels",
                                style = MaterialTheme.typography.titleMedium
                            )
                            channels.forEach { ch ->
                                ChannelRowCard(
                                    channel = ch,
                                    onToggleSubscribe = { onToggleSubscribe(ch.channelId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRowCard(
    channel: ChannelItem,
    onToggleSubscribe: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelAvatar(
            name = channel.channelName,
            seedColor = Color(channel.avatarSeedColor),
            sizeDp = 42
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.channelName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${channel.handle} • ${channel.subscriberCountText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (channel.isSubscribed) {
            OutlinedButton(
                onClick = onToggleSubscribe,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("sub_btn_${channel.channelId}")
            ) {
                Text("Subscribed")
            }
        } else {
            Button(
                onClick = onToggleSubscribe,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("sub_btn_${channel.channelId}")
            ) {
                Text("Subscribe", color = Color.White)
            }
        }
    }
}
