package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM cached_videos ORDER BY isCustom DESC, lastWatchedAt DESC, id ASC")
    fun observeAllVideos(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE isShort = 1 ORDER BY isCustom DESC, id ASC")
    fun observeShorts(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE isCachedOffline = 1 OR downloadStatus = 'COMPLETED' ORDER BY lastWatchedAt DESC")
    fun observeOfflineVideos(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE isLiked = 1 ORDER BY lastWatchedAt DESC")
    fun observeLikedVideos(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE isSaved = 1 ORDER BY lastWatchedAt DESC")
    fun observeSavedVideos(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE lastWatchedAt > 0 ORDER BY lastWatchedAt DESC LIMIT 20")
    fun observeWatchHistory(): Flow<List<CachedVideoEntity>>

    @Query("SELECT * FROM cached_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): CachedVideoEntity?

    @Query("SELECT COUNT(*) FROM cached_videos")
    suspend fun countVideos(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<CachedVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(video: CachedVideoEntity)

    @Query("UPDATE cached_videos SET isLiked = :liked, isDisliked = :disliked, likesCount = :likesCount WHERE id = :videoId")
    suspend fun updateLikeState(videoId: String, liked: Boolean, disliked: Boolean, likesCount: Long)

    @Query("UPDATE cached_videos SET isSaved = :saved WHERE id = :videoId")
    suspend fun updateSavedState(videoId: String, saved: Boolean)

    @Query("UPDATE cached_videos SET watchCount = watchCount + 1, lastWatchedAt = :watchedAt, lastPositionMs = :positionMs WHERE id = :videoId")
    suspend fun recordWatch(videoId: String, watchedAt: Long, positionMs: Long)

    @Query("UPDATE cached_videos SET lastPositionMs = :positionMs WHERE id = :videoId")
    suspend fun updatePosition(videoId: String, positionMs: Long)

    @Query(
        "UPDATE cached_videos SET downloadStatus = :status, downloadProgress = :progress, " +
            "downloadedQuality = :quality, localFilePath = :localPath, fileSizeBytes = :sizeBytes, " +
            "isCachedOffline = :cachedOffline WHERE id = :videoId"
    )
    suspend fun updateDownloadState(
        videoId: String,
        status: String,
        progress: Int,
        quality: String?,
        localPath: String?,
        sizeBytes: Long,
        cachedOffline: Boolean
    )
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM subscribed_channels ORDER BY isSubscribed DESC, channelName ASC")
    fun observeChannels(): Flow<List<SubscribedChannelEntity>>

    @Query("SELECT * FROM subscribed_channels WHERE channelId = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): SubscribedChannelEntity?

    @Query("SELECT COUNT(*) FROM subscribed_channels")
    suspend fun countChannels(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<SubscribedChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(channel: SubscribedChannelEntity)

    @Query("UPDATE subscribed_channels SET isSubscribed = :subscribed, updatedAtMillis = :updatedAt WHERE channelId = :channelId")
    suspend fun updateSubscription(channelId: String, subscribed: Boolean, updatedAt: Long)
}

@Dao
interface PendingSyncDao {
    @Query("SELECT COUNT(*) FROM pending_sync_actions")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_sync_actions ORDER BY seqId ASC")
    suspend fun getAllPending(): List<PendingSyncActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(action: PendingSyncActionEntity)

    @Query("DELETE FROM pending_sync_actions WHERE seqId = :seqId")
    suspend fun deleteBySeqId(seqId: Long)

    @Query("DELETE FROM pending_sync_actions")
    suspend fun clearAll()
}
