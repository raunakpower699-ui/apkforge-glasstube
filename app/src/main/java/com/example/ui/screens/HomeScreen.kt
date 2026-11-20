package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SyncBannerState
import com.example.model.VideoItem
import com.example.ui.components.GlassShortPreviewCard
import com.example.ui.components.GlassVideoCard
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette

private val HomeCategories = listOf(
    "All",
    "Offline Ready",
    "4K Cinema",
    "Music",
    "Tech",
    "Nature",
    "Liked"
)

@Composable
fun HomeScreen(
    feedVideos: List<VideoItem>,
    shortsVideos: List<VideoItem>,
    selectedCategory: String,
    searchQuery: String,
    isSearchOpen: Boolean,
    syncState: SyncBannerState,
    topTopics: List<String>,
    onSelectCategory: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSyncClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onOpenShort: (VideoItem) -> Unit,
    onDownloadVideo: (VideoItem) -> Unit,
    onToggleSave: (VideoItem) -> Unit,
    onToggleLike: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    var showNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top YouTube-style Floating Liquid Glass Header Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .liquidGlass(shape = RoundedCornerShape(24.dp), elevated = true)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_glasstube_icon_src),
                    contentDescription = "GlassTube logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GlassTube",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = GlassEmerald,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = stringResource(R.string.ad_free_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassEmerald
                    )
                }
            }

            SyncStatusPill(syncState = syncState, onSyncClick = onSyncClick)

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { showNotifications = true },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .liquidGlass(shape = CircleShape)
                    .testTag("notifications_button")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = stringResource(R.string.cd_notifications),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "9+",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .liquidGlass(shape = CircleShape, glowHighlighted = isSearchOpen)
                    .testTag("search_button")
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .liquidGlass(shape = CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search 4K videos, music, offline clips...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = glass.tapGlowPrimary,
                    unfocusedBorderColor = glass.glassBorderHighlight,
                    focusedContainerColor = glass.glassCardFill,
                    unfocusedContainerColor = glass.glassCardFill
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("home_search_input")
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HomeCategories) { category ->
                val selected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            glowHighlighted = selected
                        )
                        .glowScaleClickable { onSelectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("chip_${category.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 6.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!syncState.isOnline) {
                item {
                    LiquidGlassBox(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = 12.dp,
                        glowHighlighted = true
                    ) {
                        Text(
                            text = "Offline Mode Active • Watch any Offline Vault video without internet. ${syncState.pendingCount} action(s) queued to sync when reconnected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (topTopics.isNotEmpty() && selectedCategory == "All" && searchQuery.isBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Personalized for your taste: ${topTopics.take(2).joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.tapGlowPrimary
                        )
                    }
                }
            }

            // Shorts 2-column grid shelf matching the uploaded YouTube screenshot!
            if (shortsVideos.isNotEmpty() && selectedCategory == "All" && searchQuery.isBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shorts",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        val gridShorts = shortsVideos.take(4)
                        val rows = gridShorts.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { pair ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    pair.forEach { shortItem ->
                                        GlassShortPreviewCard(
                                            video = shortItem,
                                            onClick = { onOpenShort(shortItem) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (feedVideos.isEmpty()) {
                item {
                    LiquidGlassBox(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentPadding = 24.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No matching videos in this filter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try switching back to All or import a clip via Create (+).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(feedVideos, key = { it.id }) { video ->
                    GlassVideoCard(
                        video = video,
                        onPlay = { onPlayVideo(video) },
                        onDownloadClick = { onDownloadVideo(video) },
                        onToggleSave = { onToggleSave(video) },
                        onToggleLike = { onToggleLike(video) },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            }
        }
    }

    if (showNotifications) {
        LiquidGlassDialog(onDismissRequest = { showNotifications = false }) {
            Text(
                text = "Activity & Offline Sync",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• Ad-Free Shield is blocking 100% of video & banner ads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Aurora 4K Cinema uploaded a new 4K HDR film ready for offline download.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Connection Sync: ${if (syncState.isOnline) "Connected & synced" else "Offline — ${syncState.pendingCount} actions queued"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
