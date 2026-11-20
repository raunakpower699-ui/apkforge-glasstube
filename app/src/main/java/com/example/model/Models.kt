package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("White")
}

@Serializable
enum class DownloadQuality(
    val label: String,
    val badge: String,
    val resolutionLabel: String,
    val estimatedMbPerMin: Double
) {
    ULTRA_4K("4K Ultra HD", "4K", "2160p • 60fps HDR", 48.0),
    QHD_1440P("2K QHD", "2K", "1440p • 60fps", 26.0),
    FHD_1080P("Full HD", "1080p", "1080p • 60fps", 15.0),
    HD_720P("HD Saver", "720p", "720p • 30fps", 8.0),
    AUDIO_320("Music Audio", "AUDIO", "320kbps AAC", 2.4);

    companion object {
        fun fromName(raw: String?): DownloadQuality =
            entries.firstOrNull { it.name == raw } ?: ULTRA_4K
    }
}

enum class DownloadStatus {
    NONE,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

enum class MainTab(val label: String) {
    HOME("Home"),
    SHORTS("Shorts"),
    CREATE("Create"),
    SUBSCRIPTIONS("Subscriptions"),
    YOU("You")
}

data class VideoItem(
    val id: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val channelHandle: String,
    val category: String,
    val description: String,
    val streamUrl: String,
    val localFilePath: String?,
    val thumbnailResKey: String,
    val durationSec: Int,
    val viewCountText: String,
    val publishedAgo: String,
    val likesCount: Long,
    val maxQuality: String,
    val isShort: Boolean,
    val isMusic: Boolean,
    val isLiked: Boolean,
    val isDisliked: Boolean,
    val isSaved: Boolean,
    val isCachedOffline: Boolean,
    val downloadStatus: DownloadStatus,
    val downloadProgress: Int,
    val downloadedQuality: DownloadQuality?,
    val fileSizeBytes: Long,
    val watchCount: Int,
    val lastPositionMs: Long,
    val lastWatchedAt: Long,
    val isCustom: Boolean
)

data class ChannelItem(
    val channelId: String,
    val channelName: String,
    val handle: String,
    val category: String,
    val subscriberCountText: String,
    val isSubscribed: Boolean,
    val hasNewUpload: Boolean,
    val avatarSeedColor: Long
)

data class NetworkStatus(
    val isOnline: Boolean = true,
    val isMetered: Boolean = false,
    val transportLabel: String = "Wi-Fi"
)

data class SyncBannerState(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncedAtMillis: Long? = null,
    val lastError: String? = null
)

data class StorageStats(
    val downloadedVideosBytes: Long = 0L,
    val offlineCacheBytes: Long = 0L,
    val appDatabaseBytes: Long = 0L,
    val availableDeviceBytes: Long = 0L,
    val downloadedFileCount: Int = 0
) {
    val totalAppBytes: Long
        get() = downloadedVideosBytes + offlineCacheBytes + appDatabaseBytes
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// --- Firestore DTOs (No `id` in body, no `is`-prefixed Booleans, nullable Timestamp?) ---

@Keep
@IgnoreExtraProperties
data class UserProfileDto(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredTopics: List<String> = emptyList(),
    val themeMode: String = ThemeMode.SYSTEM.name,
    val backgroundPlay: Boolean = true,
    val backgroundDataPlay: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoStateDto(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val videoId: String = "",
    val title: String = "",
    val category: String = "",
    val liked: Boolean = false,
    val saved: Boolean = false,
    val watchCount: Long = 0L,
    val lastPositionMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChannelSubscriptionDto(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val category: String = "",
    val subscribed: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomVideoDto(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val title: String = "",
    val channelName: String = "",
    val category: String = "",
    val streamUrl: String = "",
    val durationSec: Long = 0L,
    val shortVideo: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
