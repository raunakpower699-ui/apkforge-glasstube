package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.DownloadQuality
import com.example.model.DownloadStatus

@Database(
    entities = [
        CachedVideoEntity::class,
        SubscribedChannelEntity::class,
        PendingSyncActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GlassTubeDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun channelDao(): ChannelDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: GlassTubeDatabase? = null

        fun getInstance(context: Context): GlassTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlassTubeDatabase::class.java,
                    "glasstube_offline.db"
                ).build().also { INSTANCE = it }
            }
        }

        suspend fun seedIfEmpty(context: Context, db: GlassTubeDatabase) {
            val channelDao = db.channelDao()
            val videoDao = db.videoDao()

            if (channelDao.countChannels() == 0) {
                channelDao.upsertAll(starterChannels())
            }
            if (videoDao.countVideos() == 0) {
                val offlineUri = "android.resource://${context.packageName}/raw/offline_ambient_lofi"
                videoDao.upsertAll(starterVideos(offlineUri))
            }
        }

        private fun starterChannels(): List<SubscribedChannelEntity> = listOf(
            SubscribedChannelEntity(
                channelId = "ch_cosmos_4k",
                channelName = "Aurora 4K Cinema",
                handle = "@aurora4k",
                category = "4K Cinema",
                subscriberCountText = "4.2M subscribers",
                isSubscribed = true,
                hasNewUpload = true,
                avatarSeedColor = 0xFF22D3EE,
                updatedAtMillis = System.currentTimeMillis()
            ),
            SubscribedChannelEntity(
                channelId = "ch_glass_synth",
                channelName = "Glass Synth Lab",
                handle = "@glasssynth",
                category = "Music",
                subscriberCountText = "1.8M subscribers",
                isSubscribed = true,
                hasNewUpload = true,
                avatarSeedColor = 0xFFFF2D55,
                updatedAtMillis = System.currentTimeMillis()
            ),
            SubscribedChannelEntity(
                channelId = "ch_prism_tech",
                channelName = "Prism Tech Studio",
                handle = "@prismtech",
                category = "Tech",
                subscriberCountText = "2.9M subscribers",
                isSubscribed = true,
                hasNewUpload = false,
                avatarSeedColor = 0xFF8B5CF6,
                updatedAtMillis = System.currentTimeMillis()
            ),
            SubscribedChannelEntity(
                channelId = "ch_alpine_drone",
                channelName = "Alpine Aerials",
                handle = "@alpineaerials",
                category = "Nature",
                subscriberCountText = "960K subscribers",
                isSubscribed = false,
                hasNewUpload = true,
                avatarSeedColor = 0xFF10B981,
                updatedAtMillis = System.currentTimeMillis()
            ),
            SubscribedChannelEntity(
                channelId = "ch_cyber_beats",
                channelName = "Midnight Lo-Fi",
                handle = "@midnightlofi",
                category = "Music",
                subscriberCountText = "3.4M subscribers",
                isSubscribed = true,
                hasNewUpload = false,
                avatarSeedColor = 0xFFF59E0B,
                updatedAtMillis = System.currentTimeMillis()
            )
        )

        private fun starterVideos(offlineResourceUri: String): List<CachedVideoEntity> = listOf(
            CachedVideoEntity(
                id = "vid_aurora_4k",
                title = "Bioluminescent Aurora & Deep Ocean Reflections in 4K HDR",
                channelId = "ch_cosmos_4k",
                channelName = "Aurora 4K Cinema",
                channelHandle = "@aurora4k",
                category = "4K Cinema",
                description = "Shot in ultra-sharp 4K 60fps HDR along the Arctic coast. Stream ad-free or save directly to your Offline Vault for flights and commutes.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                localFilePath = offlineResourceUri,
                thumbnailResKey = "cosmos",
                durationSec = 246,
                viewCountText = "2.4M views",
                publishedAgo = "3 hours ago",
                likesCount = 148200,
                maxQuality = "4K HDR",
                isShort = false,
                isMusic = false,
                isLiked = false,
                isDisliked = false,
                isSaved = true,
                isCachedOffline = true,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 100,
                downloadedQuality = DownloadQuality.ULTRA_4K.name,
                fileSizeBytes = 142_600_000L,
                watchCount = 1,
                lastPositionMs = 0L,
                lastWatchedAt = System.currentTimeMillis() - 3_600_000L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "vid_lofi_rain",
                title = "Midnight Rain Synthwave • Background Focus Mix (Lossless)",
                channelId = "ch_glass_synth",
                channelName = "Glass Synth Lab",
                channelHandle = "@glasssynth",
                category = "Music",
                description = "Built for background playback & Dynamic Island live controls. Keep listening with your screen locked or while browsing other apps.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                localFilePath = offlineResourceUri,
                thumbnailResKey = "lofi_studio",
                durationSec = 1840,
                viewCountText = "890K views",
                publishedAgo = "1 day ago",
                likesCount = 67400,
                maxQuality = "4K 60fps",
                isShort = false,
                isMusic = true,
                isLiked = true,
                isDisliked = false,
                isSaved = true,
                isCachedOffline = true,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 100,
                downloadedQuality = DownloadQuality.AUDIO_320.name,
                fileSizeBytes = 44_200_000L,
                watchCount = 3,
                lastPositionMs = 0L,
                lastWatchedAt = System.currentTimeMillis() - 7_200_000L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "vid_liquid_glass_ui",
                title = "Inside Liquid Glass OS: Optical Refraction & Zero-Lag Touch",
                channelId = "ch_prism_tech",
                channelName = "Prism Tech Studio",
                channelHandle = "@prismtech",
                category = "Tech",
                description = "Deep dive into frosted glass shaders, spring physics, and next-gen mobile hardware displays in crisp 4K.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                localFilePath = null,
                thumbnailResKey = "tech_glass",
                durationSec = 742,
                viewCountText = "1.1M views",
                publishedAgo = "2 days ago",
                likesCount = 91500,
                maxQuality = "4K 60fps",
                isShort = false,
                isMusic = false,
                isLiked = false,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = false,
                downloadStatus = DownloadStatus.NONE.name,
                downloadProgress = 0,
                downloadedQuality = null,
                fileSizeBytes = 0L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "vid_alpine_drone_4k",
                title = "Dolomites Emerald Lakes at Sunrise • Pure 4K Drone Film",
                channelId = "ch_alpine_drone",
                channelName = "Alpine Aerials",
                channelHandle = "@alpineaerials",
                category = "Nature",
                description = "Uncompressed aerial vistas captured above misty pine valleys. Download in 4K 2160p for offline theater playback.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                localFilePath = offlineResourceUri,
                thumbnailResKey = "alpine_4k",
                durationSec = 515,
                viewCountText = "640K views",
                publishedAgo = "5 days ago",
                likesCount = 41200,
                maxQuality = "4K HDR",
                isShort = false,
                isMusic = false,
                isLiked = false,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = true,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 100,
                downloadedQuality = DownloadQuality.FHD_1080P.name,
                fileSizeBytes = 78_900_000L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "vid_cyber_beats_session",
                title = "Tokyo Neon Skyline • Chill Analog Beats for Late Night Coding",
                channelId = "ch_cyber_beats",
                channelName = "Midnight Lo-Fi",
                channelHandle = "@midnightlofi",
                category = "Music",
                description = "Warm analog chords and crystal rain textures. Background playback ready with Dynamic Island live waveform.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                localFilePath = null,
                thumbnailResKey = "lofi_studio",
                durationSec = 2140,
                viewCountText = "3.1M views",
                publishedAgo = "1 week ago",
                likesCount = 210400,
                maxQuality = "1440p",
                isShort = false,
                isMusic = true,
                isLiked = false,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = false,
                downloadStatus = DownloadStatus.NONE.name,
                downloadProgress = 0,
                downloadedQuality = null,
                fileSizeBytes = 0L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            ),
            // --- SHORTS ---
            CachedVideoEntity(
                id = "short_aurora_pulse",
                title = "Northern Lights pulsing over black sand beach #4K #Shorts",
                channelId = "ch_cosmos_4k",
                channelName = "Aurora 4K Cinema",
                channelHandle = "@aurora4k",
                category = "4K Cinema",
                description = "Captured at ISO 3200 in 4K vertical HDR.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                localFilePath = offlineResourceUri,
                thumbnailResKey = "cosmos",
                durationSec = 38,
                viewCountText = "4.8M views",
                publishedAgo = "6 hours ago",
                likesCount = 329000,
                maxQuality = "4K",
                isShort = true,
                isMusic = false,
                isLiked = true,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = true,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 100,
                downloadedQuality = DownloadQuality.ULTRA_4K.name,
                fileSizeBytes = 28_400_000L,
                watchCount = 2,
                lastPositionMs = 0L,
                lastWatchedAt = System.currentTimeMillis() - 1_800_000L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "short_glass_phone",
                title = "3 Android Glass Tricks You Didn't Know #Tech #Shorts",
                channelId = "ch_prism_tech",
                channelName = "Prism Tech Studio",
                channelHandle = "@prismtech",
                category = "Tech",
                description = "Enable frosted translucent panels and Dynamic Island media pills.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                localFilePath = offlineResourceUri,
                thumbnailResKey = "tech_glass",
                durationSec = 45,
                viewCountText = "1.9M views",
                publishedAgo = "12 hours ago",
                likesCount = 184000,
                maxQuality = "4K",
                isShort = true,
                isMusic = false,
                isLiked = false,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = true,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 100,
                downloadedQuality = DownloadQuality.FHD_1080P.name,
                fileSizeBytes = 19_100_000L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "short_synth_drop",
                title = "Rainy Window Analog Synth Patch in 30 Seconds #Music #Shorts",
                channelId = "ch_glass_synth",
                channelName = "Glass Synth Lab",
                channelHandle = "@glasssynth",
                category = "Music",
                description = "Play this in background mode with screen off!",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                localFilePath = null,
                thumbnailResKey = "lofi_studio",
                durationSec = 31,
                viewCountText = "920K views",
                publishedAgo = "1 day ago",
                likesCount = 94200,
                maxQuality = "1080p",
                isShort = true,
                isMusic = true,
                isLiked = false,
                isDisliked = false,
                isSaved = true,
                isCachedOffline = false,
                downloadStatus = DownloadStatus.NONE.name,
                downloadProgress = 0,
                downloadedQuality = null,
                fileSizeBytes = 0L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            ),
            CachedVideoEntity(
                id = "short_alpine_mirror",
                title = "Mirror Lake Drone Dive at Golden Hour #Nature #4K",
                channelId = "ch_alpine_drone",
                channelName = "Alpine Aerials",
                channelHandle = "@alpineaerials",
                category = "Nature",
                description = "Pure crystal water reflection at 2400m altitude.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                localFilePath = null,
                thumbnailResKey = "alpine_4k",
                durationSec = 27,
                viewCountText = "2.2M views",
                publishedAgo = "2 days ago",
                likesCount = 247000,
                maxQuality = "4K",
                isShort = true,
                isMusic = false,
                isLiked = false,
                isDisliked = false,
                isSaved = false,
                isCachedOffline = false,
                downloadStatus = DownloadStatus.NONE.name,
                downloadProgress = 0,
                downloadedQuality = null,
                fileSizeBytes = 0L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = 0L,
                isCustom = false
            )
        )
    }
}
