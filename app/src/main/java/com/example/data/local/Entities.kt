package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ChannelItem
import com.example.model.DownloadQuality
import com.example.model.DownloadStatus
import com.example.model.VideoItem

@Entity(tableName = "cached_videos")
data class CachedVideoEntity(
    @PrimaryKey val id: String,
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
    val downloadStatus: String,
    val downloadProgress: Int,
    val downloadedQuality: String?,
    val fileSizeBytes: Long,
    val watchCount: Int,
    val lastPositionMs: Long,
    val lastWatchedAt: Long,
    val isCustom: Boolean
) {
    fun toDomain(): VideoItem = VideoItem(
        id = id,
        title = title,
        channelId = channelId,
        channelName = channelName,
        channelHandle = channelHandle,
        category = category,
        description = description,
        streamUrl = streamUrl,
        localFilePath = localFilePath,
        thumbnailResKey = thumbnailResKey,
        durationSec = durationSec,
        viewCountText = viewCountText,
        publishedAgo = publishedAgo,
        likesCount = likesCount,
        maxQuality = maxQuality,
        isShort = isShort,
        isMusic = isMusic,
        isLiked = isLiked,
        isDisliked = isDisliked,
        isSaved = isSaved,
        isCachedOffline = isCachedOffline,
        downloadStatus = DownloadStatus.entries.firstOrNull { it.name == downloadStatus }
            ?: DownloadStatus.NONE,
        downloadProgress = downloadProgress,
        downloadedQuality = downloadedQuality?.let { DownloadQuality.fromName(it) },
        fileSizeBytes = fileSizeBytes,
        watchCount = watchCount,
        lastPositionMs = lastPositionMs,
        lastWatchedAt = lastWatchedAt,
        isCustom = isCustom
    )
}

@Entity(tableName = "subscribed_channels")
data class SubscribedChannelEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val handle: String,
    val category: String,
    val subscriberCountText: String,
    val isSubscribed: Boolean,
    val hasNewUpload: Boolean,
    val avatarSeedColor: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): ChannelItem = ChannelItem(
        channelId = channelId,
        channelName = channelName,
        handle = handle,
        category = category,
        subscriberCountText = subscriberCountText,
        isSubscribed = isSubscribed,
        hasNewUpload = hasNewUpload,
        avatarSeedColor = avatarSeedColor
    )
}

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey(autoGenerate = true) val seqId: Long = 0,
    val entityType: String, // "VIDEO_STATE", "SUBSCRIPTION", "PROFILE", "CUSTOM_VIDEO"
    val targetId: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
