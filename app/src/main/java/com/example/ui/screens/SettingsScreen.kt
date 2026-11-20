package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppSettings
import com.example.data.network.StorageInspector
import com.example.model.DownloadQuality
import com.example.model.NetworkStatus
import com.example.model.StorageStats
import com.example.model.ThemeMode
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.glowScaleClickable
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GlassEmerald
import com.example.ui.theme.LocalGlassPalette

@Composable
fun SettingsScreen(
    settings: AppSettings,
    storageStats: StorageStats,
    networkStatus: NetworkStatus,
    onBack: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleBackgroundDataPlay: (Boolean) -> Unit,
    onToggleWifiOnlyDownloads: (Boolean) -> Unit,
    onToggleDynamicIsland: (Boolean) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSelectDefaultQuality: (DownloadQuality) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val glass = LocalGlassPalette.current

    LiquidGlassSurface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .liquidGlass(shape = RoundedCornerShape(22.dp), elevated = true)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .liquidGlass(shape = CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings & Storage",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 3-Mode Appearance Switcher (Dark, White, System)
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = 16.dp,
                    elevated = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Appearance Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeModeOption(
                                mode = ThemeMode.DARK,
                                label = "Dark",
                                icon = Icons.Rounded.DarkMode,
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { onSelectTheme(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeOption(
                                mode = ThemeMode.LIGHT,
                                label = "White",
                                icon = Icons.Rounded.LightMode,
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { onSelectTheme(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeOption(
                                mode = ThemeMode.SYSTEM,
                                label = "System",
                                icon = Icons.Rounded.BrightnessAuto,
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. Storage Usage Analyzer
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = glass.tapGlowPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Storage Usage",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = StorageInspector.formatBytes(storageStats.totalAppBytes),
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.tapGlowPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val totalPool = (storageStats.totalAppBytes + storageStats.availableDeviceBytes)
                            .coerceAtLeast(1L)
                        val usedRatio = (storageStats.totalAppBytes.toFloat() / totalPool.toFloat())
                            .coerceIn(0.03f, 1f)
                        LinearProgressIndicator(
                            progress = { usedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )

                        StorageStatRow("4K & Offline Videos (${storageStats.downloadedFileCount})", StorageInspector.formatBytes(storageStats.downloadedVideosBytes))
                        StorageStatRow("Temporary Stream Cache", StorageInspector.formatBytes(storageStats.offlineCacheBytes))
                        StorageStatRow("Offline Database & Sync Queue", StorageInspector.formatBytes(storageStats.appDatabaseBytes))
                        StorageStatRow("Available Device Storage", StorageInspector.formatBytes(storageStats.availableDeviceBytes))

                        OutlinedButton(
                            onClick = onClearCache,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_cache_button")
                        ) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Temporary Cache")
                        }
                    }
                }

                // 3. Background Playback, Background Data Play & Dynamic Island
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Background Play & Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        SettingSwitchRow(
                            title = "Background Playback",
                            subtitle = "Keep music & videos playing when you go to the home screen or lock device",
                            checked = settings.backgroundPlayEnabled,
                            onCheckedChange = onToggleBackgroundPlay,
                            testTag = "switch_background_play"
                        )
                        SettingSwitchRow(
                            title = "Background Data Play",
                            subtitle = "Allow streaming in background over cellular mobile data (${networkStatus.transportLabel})",
                            checked = settings.backgroundDataPlayEnabled,
                            onCheckedChange = onToggleBackgroundDataPlay,
                            testTag = "switch_background_data_play"
                        )
                        SettingSwitchRow(
                            title = "Dynamic Island Media Pill",
                            subtitle = "Display live frosted capsule & system media controls while minimized",
                            checked = settings.dynamicIslandEnabled,
                            onCheckedChange = onToggleDynamicIsland,
                            testTag = "switch_dynamic_island"
                        )
                        SettingSwitchRow(
                            title = "Wi-Fi Only 4K Downloads",
                            subtitle = "Pause large 4K downloads when on metered mobile data",
                            checked = settings.wifiOnlyDownloads,
                            onCheckedChange = onToggleWifiOnlyDownloads,
                            testTag = "switch_wifi_only_dl"
                        )
                        SettingSwitchRow(
                            title = "Auto-Sync When Connected",
                            subtitle = "Sync offline likes, subscriptions & history only when internet returns",
                            checked = settings.autoSyncOnReconnect,
                            onCheckedChange = onToggleAutoSync,
                            testTag = "switch_auto_sync"
                        )
                    }
                }

                // 4. Default 4K Download Quality
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Default Offline Download Quality",
                            style = MaterialTheme.typography.titleMedium
                        )
                        DownloadQuality.entries.forEach { quality ->
                            val selected = settings.defaultDownloadQuality == quality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(
                                        shape = RoundedCornerShape(14.dp),
                                        glowHighlighted = selected
                                    )
                                    .glowScaleClickable { onSelectDefaultQuality(quality) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("quality_default_${quality.name}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(quality.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        quality.resolutionLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (selected) "ACTIVE" else quality.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 5. Ad-Free Shield
                LiquidGlassBox(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    cornerRadius = 20.dp,
                    contentPadding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = GlassEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ad-Free Liquid Engine",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Zero pre-roll, mid-roll, or banner ads enabled permanently.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(16.dp), glowHighlighted = selected)
            .glowScaleClickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("theme_mode_${mode.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
