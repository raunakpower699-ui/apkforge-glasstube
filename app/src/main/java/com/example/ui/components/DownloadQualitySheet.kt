package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DownloadQuality
import com.example.model.VideoItem
import com.example.ui.theme.LocalGlassPalette
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualitySheet(
    video: VideoItem,
    defaultQuality: DownloadQuality,
    onDismiss: () -> Unit,
    onSelectQuality: (DownloadQuality) -> Unit
) {
    val glass = LocalGlassPalette.current
    val durationMinutes = (video.durationSec / 60.0).coerceAtLeast(0.5)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (glass.isDark) {
            androidx.compose.ui.graphics.Color(0xEB0A101E)
        } else {
            androidx.compose.ui.graphics.Color(0xF0F1F7FF)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Download to Offline Vault",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            DownloadQuality.entries.forEach { quality ->
                val estMb = durationMinutes * quality.estimatedMbPerMin
                val sizeText = if (estMb >= 1024) {
                    String.format(Locale.US, "%.2f GB", estMb / 1024.0)
                } else {
                    String.format(Locale.US, "%.0f MB", estMb)
                }
                val recommended = quality == defaultQuality

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            glowHighlighted = recommended
                        )
                        .glowScaleClickable { onSelectQuality(quality) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("dl_option_${quality.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (recommended) glass.tapGlowPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = quality.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = quality.resolutionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.tapGlowPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
