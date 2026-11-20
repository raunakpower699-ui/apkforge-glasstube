package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.GlassTubeApp
import com.example.data.local.AppSettings
import com.example.data.local.CachedVideoEntity
import com.example.data.local.ChannelDao
import com.example.data.local.PendingSyncDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VideoDao
import com.example.data.network.NetworkObserver
import com.example.data.network.OfflineDownloadManager
import com.example.data.network.StorageInspector
import com.example.data.remote.FirestoreSyncRepository
import com.example.media.PlaybackController
import com.example.model.ChannelItem
import com.example.model.DownloadQuality
import com.example.model.DownloadStatus
import com.example.model.MainTab
import com.example.model.NetworkStatus
import com.example.model.StorageStats
import com.example.model.SyncBannerState
import com.example.model.ThemeMode
import com.example.model.VideoItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class GlassTubeViewModel(
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val videoDao: VideoDao,
    private val channelDao: ChannelDao,
    private val pendingSyncDao: PendingSyncDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val networkObserver: NetworkObserver,
    private val storageInspector: StorageInspector,
    private val downloadManager: OfflineDownloadManager,
    private val syncRepository: FirestoreSyncRepository,
    val playbackController: PlaybackController
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _downloadSheetVideo = MutableStateFlow<VideoItem?>(null)
    val downloadSheetVideo: StateFlow<VideoItem?> = _downloadSheetVideo.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    private val _syncError = MutableStateFlow<String?>(null)

    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val networkStatus: StateFlow<NetworkStatus> = networkObserver.statusFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            networkObserver.currentStatus()
        )

    val allVideos: StateFlow<List<VideoItem>> = videoDao.observeAllVideos()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val channels: StateFlow<List<ChannelItem>> = channelDao.observeChannels()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingSyncCount: StateFlow<Int> = pendingSyncDao.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val syncBannerState: StateFlow<SyncBannerState> = combine(
        networkStatus,
        _isSyncing,
        pendingSyncCount,
        _lastSyncedAt,
        _syncError
    ) { net, syncing, pending, lastSynced, err ->
        SyncBannerState(
            isOnline = net.isOnline,
            isSyncing = syncing,
            pendingCount = pending,
            lastSyncedAtMillis = lastSynced,
            lastError = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SyncBannerState())

    /**
     * Personalized Home feed: ranks videos based on user's watch history,
     * liked categories, and subscribed channels. When offline, highlights
     * offline-ready videos smoothly without hiding catalog metadata.
     */
    val personalizedHomeFeed: StateFlow<List<VideoItem>> = combine(
        allVideos,
        channels,
        _selectedCategory,
        _searchQuery,
        networkStatus
    ) { videos, channelList, category, query, net ->
        val subscribedIds = channelList.filter { it.isSubscribed }.map { it.channelId }.toSet()
        val affinityWeights = mutableMapOf<String, Int>()
        videos.forEach { v ->
            val score = v.watchCount * 3 + (if (v.isLiked) 4 else 0) + (if (v.isSaved) 2 else 0)
            if (score > 0) {
                affinityWeights[v.category] = (affinityWeights[v.category] ?: 0) + score
            }
        }

        videos
            .filter { !it.isShort }
            .filter { video ->
                when (category) {
                    "All" -> true
                    "Offline Ready" -> video.isCachedOffline || video.downloadStatus == DownloadStatus.COMPLETED
                    "Liked" -> video.isLiked
                    else -> video.category.equals(category, ignoreCase = true)
                }
            }
            .filter { video ->
                if (query.isBlank()) true
                else video.title.contains(query, ignoreCase = true) ||
                    video.channelName.contains(query, ignoreCase = true) ||
                    video.category.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<VideoItem> { video ->
                    var score = affinityWeights[video.category] ?: 0
                    if (video.channelId in subscribedIds) score += 5
                    if (!net.isOnline && video.isCachedOffline) score += 20
                    if (video.isCustom) score += 10
                    score
                }.thenByDescending { it.likesCount }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shortsFeed: StateFlow<List<VideoItem>> = allVideos
        .map { list -> list.filter { it.isShort } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val offlineVaultVideos: StateFlow<List<VideoItem>> = allVideos
        .map { list ->
            list.filter {
                it.isCachedOffline ||
                    it.downloadStatus == DownloadStatus.COMPLETED ||
                    it.downloadStatus == DownloadStatus.DOWNLOADING
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val likedVideos: StateFlow<List<VideoItem>> = allVideos
        .map { list -> list.filter { it.isLiked } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedVideos: StateFlow<List<VideoItem>> = allVideos
        .map { list -> list.filter { it.isSaved } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val watchHistory: StateFlow<List<VideoItem>> = allVideos
        .map { list -> list.filter { it.lastWatchedAt > 0L }.sortedByDescending { it.lastWatchedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val topAffinityTopics: StateFlow<List<String>> = allVideos
        .map { videos ->
            videos.groupBy { it.category }
                .mapValues { (_, items) ->
                    items.sumOf { it.watchCount * 3 + (if (it.isLiked) 4 else 0) }
                }
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        playbackController.connect()
        observeConnectionRestoredSync()
        observeCloudListeners()
        observeStorageChanges()
    }

    private fun observeConnectionRestoredSync() {
        viewModelScope.launch {
            networkStatus
                .map { it.isOnline }
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline && settings.value.autoSyncOnReconnect && currentUserId.isNotBlank()) {
                        syncNow()
                    }
                }
        }
    }

    private fun observeCloudListeners() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            syncRepository.observeRemoteVideoStates(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w("GlassTubeVM", "Remote videoStates stream error: ${error.message}")
                }
                .collect { remoteStates ->
                    for (state in remoteStates) {
                        val local = videoDao.getVideoById(state.videoId) ?: continue
                        if (local.isLiked != state.liked || local.isSaved != state.saved) {
                            videoDao.upsert(
                                local.copy(
                                    isLiked = state.liked,
                                    isSaved = state.saved,
                                    watchCount = maxOf(local.watchCount, state.watchCount.toInt()),
                                    lastPositionMs = maxOf(local.lastPositionMs, state.lastPositionMs)
                                )
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            syncRepository.observeRemoteSubscriptions(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w("GlassTubeVM", "Remote subscriptions stream error: ${error.message}")
                }
                .collect { remoteSubs ->
                    for (sub in remoteSubs) {
                        val local = channelDao.getChannelById(sub.channelId) ?: continue
                        if (local.isSubscribed != sub.subscribed) {
                            channelDao.updateSubscription(
                                sub.channelId,
                                sub.subscribed,
                                System.currentTimeMillis()
                            )
                        }
                    }
                }
        }
    }

    private fun observeStorageChanges() {
        viewModelScope.launch {
            allVideos.collect { videos ->
                val offlineVideos = videos.filter {
                    it.isCachedOffline || it.downloadStatus == DownloadStatus.COMPLETED
                }
                val recordedBytes = offlineVideos.sumOf { it.fileSizeBytes }
                _storageStats.value = storageInspector.inspectStorage(
                    recordedOfflineBytes = recordedBytes,
                    downloadedCount = offlineVideos.size
                )
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        _showSettings.value = false
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleSearch() {
        _isSearchOpen.value = !_isSearchOpen.value
        if (!_isSearchOpen.value) _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSettings() {
        _showSettings.value = true
        refreshStorageStats()
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val offlineVideos = allVideos.value.filter {
                it.isCachedOffline || it.downloadStatus == DownloadStatus.COMPLETED
            }
            _storageStats.value = storageInspector.inspectStorage(
                recordedOfflineBytes = offlineVideos.sumOf { it.fileSizeBytes },
                downloadedCount = offlineVideos.size
            )
        }
    }

    fun clearTempCache() {
        viewModelScope.launch {
            val freed = storageInspector.clearTempCache()
            refreshStorageStats()
            showMessage("Cleared ${StorageInspector.formatBytes(freed)} temporary cache")
        }
    }

    fun playVideo(video: VideoItem, expand: Boolean = true) {
        val online = networkStatus.value.isOnline
        if (!online && !video.isCachedOffline && video.localFilePath.isNullOrBlank()) {
            showMessage("Offline mode: showing cached fallback audio. Download 4K when online!")
        }
        playbackController.playVideo(video, expand = expand, isOnline = online)
        viewModelScope.launch {
            videoDao.recordWatch(video.id, System.currentTimeMillis(), video.lastPositionMs)
            queueAndMaybeSync("VIDEO_STATE", video.id)
        }
    }

    fun toggleLike(video: VideoItem) {
        viewModelScope.launch {
            val newLiked = !video.isLiked
            val delta = if (newLiked) 1L else -1L
            val updatedCount = (video.likesCount + delta).coerceAtLeast(0L)
            videoDao.updateLikeState(
                videoId = video.id,
                liked = newLiked,
                disliked = false,
                likesCount = updatedCount
            )
            queueAndMaybeSync("VIDEO_STATE", video.id)
        }
    }

    fun toggleDislike(video: VideoItem) {
        viewModelScope.launch {
            val newDisliked = !video.isDisliked
            val updatedCount = if (video.isLiked) (video.likesCount - 1L).coerceAtLeast(0L) else video.likesCount
            videoDao.updateLikeState(
                videoId = video.id,
                liked = false,
                disliked = newDisliked,
                likesCount = updatedCount
            )
            queueAndMaybeSync("VIDEO_STATE", video.id)
        }
    }

    fun toggleSave(video: VideoItem) {
        viewModelScope.launch {
            val newSaved = !video.isSaved
            videoDao.updateSavedState(video.id, newSaved)
            queueAndMaybeSync("VIDEO_STATE", video.id)
            showMessage(if (newSaved) "Saved to Library" else "Removed from Saved")
        }
    }

    fun toggleSubscribe(channelId: String) {
        viewModelScope.launch {
            val existing = channelDao.getChannelById(channelId) ?: return@launch
            val newSub = !existing.isSubscribed
            channelDao.updateSubscription(channelId, newSub, System.currentTimeMillis())
            queueAndMaybeSync("SUBSCRIPTION", channelId)
            showMessage(
                if (newSub) "Subscribed to ${existing.channelName}"
                else "Unsubscribed from ${existing.channelName}"
            )
        }
    }

    fun openDownloadSheet(video: VideoItem) {
        _downloadSheetVideo.value = video
    }

    fun closeDownloadSheet() {
        _downloadSheetVideo.value = null
    }

    fun startDownload(video: VideoItem, quality: DownloadQuality) {
        val net = networkStatus.value
        if (settings.value.wifiOnlyDownloads && net.isMetered && net.isOnline) {
            showMessage("Wi-Fi Only Downloads enabled in Settings")
            return
        }
        _downloadSheetVideo.value = null
        downloadManager.startDownload(video, quality)
        showMessage("Downloading ${quality.label} for offline viewing")
    }

    fun removeOfflineDownload(video: VideoItem) {
        viewModelScope.launch {
            downloadManager.removeDownload(video)
            refreshStorageStats()
            showMessage("Removed offline copy & freed storage")
        }
    }

    fun addCustomVideo(
        title: String,
        channelName: String,
        category: String,
        streamOrLocalUri: String,
        isShort: Boolean,
        isLocalPicked: Boolean
    ) {
        val cleanTitle = title.trim().ifBlank { "My Liquid Glass Clip" }.take(160)
        val cleanChannel = channelName.trim().ifBlank { userDisplayName.ifBlank { "Creator" } }.take(80)
        val cleanUrl = streamOrLocalUri.trim().ifBlank {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        }.take(500)
        val newId = "custom_${UUID.randomUUID().toString().take(8)}"

        viewModelScope.launch {
            val entity = CachedVideoEntity(
                id = newId,
                title = cleanTitle,
                channelId = "ch_user_${currentUserId.take(6)}",
                channelName = cleanChannel,
                channelHandle = "@${cleanChannel.lowercase().replace(" ", "")}",
                category = category,
                description = "Added via GlassTube Creator Studio • Ad-Free & Offline Ready",
                streamUrl = cleanUrl,
                localFilePath = if (isLocalPicked) cleanUrl else null,
                thumbnailResKey = if (category == "Music") "lofi_studio" else "tech_glass",
                durationSec = if (isShort) 42 else 185,
                viewCountText = "Just now",
                publishedAgo = "Just now",
                likesCount = 1L,
                maxQuality = "4K HDR",
                isShort = isShort,
                isMusic = category.equals("Music", ignoreCase = true),
                isLiked = false,
                isDisliked = false,
                isSaved = true,
                isCachedOffline = isLocalPicked,
                downloadStatus = if (isLocalPicked) DownloadStatus.COMPLETED.name else DownloadStatus.NONE.name,
                downloadProgress = if (isLocalPicked) 100 else 0,
                downloadedQuality = if (isLocalPicked) DownloadQuality.ULTRA_4K.name else null,
                fileSizeBytes = if (isLocalPicked) 32_000_000L else 0L,
                watchCount = 0,
                lastPositionMs = 0L,
                lastWatchedAt = System.currentTimeMillis(),
                isCustom = true
            )
            videoDao.upsert(entity)
            queueAndMaybeSync("CUSTOM_VIDEO", newId)
            showMessage(if (isShort) "Published to Shorts!" else "Added to GlassTube Feed!")
            _selectedTab.value = if (isShort) MainTab.SHORTS else MainTab.HOME
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            queueAndMaybeSync("PROFILE", currentUserId)
        }
    }

    fun setBackgroundPlay(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundPlay(enabled)
            queueAndMaybeSync("PROFILE", currentUserId)
        }
    }

    fun setBackgroundDataPlay(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundDataPlay(enabled)
            queueAndMaybeSync("PROFILE", currentUserId)
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiOnlyDownloads(enabled) }
    }

    fun setDynamicIslandEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicIslandEnabled(enabled) }
    }

    fun setAutoSyncOnReconnect(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSyncOnReconnect(enabled) }
    }

    fun setDefaultDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch { preferencesRepository.setDefaultDownloadQuality(quality) }
    }

    fun syncNow() {
        if (currentUserId.isBlank()) return
        if (!networkStatus.value.isOnline) {
            showMessage("Offline — pending changes will sync when connection is restored")
            return
        }
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val result = syncRepository.flushPendingQueue(currentUserId, topAffinityTopics.value)
            if (result.isSuccess) {
                syncRepository.pullCloudStateToLocal(currentUserId)
                _lastSyncedAt.value = System.currentTimeMillis()
            } else {
                _syncError.value = result.exceptionOrNull()?.localizedMessage ?: "Sync deferred"
            }
            _isSyncing.value = false
        }
    }

    private suspend fun queueAndMaybeSync(entityType: String, targetId: String) {
        syncRepository.enqueueAction(entityType, targetId)
        if (networkStatus.value.isOnline && settings.value.autoSyncOnReconnect) {
            syncNow()
        }
    }

    fun handleAppBackgrounded() {
        val currentSettings = settings.value
        playbackController.onAppBackgrounded(
            backgroundPlayEnabled = currentSettings.backgroundPlayEnabled,
            backgroundDataPlayEnabled = currentSettings.backgroundDataPlayEnabled,
            networkStatus = networkStatus.value
        )
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun relatedVideosFor(current: VideoItem): List<VideoItem> =
        allVideos.value
            .filter { it.id != current.id && !it.isShort }
            .sortedWith(
                compareByDescending<VideoItem> { it.category == current.category }
                    .thenByDescending { it.channelId == current.channelId }
                    .thenByDescending { it.likesCount }
            )

    fun isChannelSubscribed(channelId: String): Boolean =
        channels.value.firstOrNull { it.channelId == channelId }?.isSubscribed ?: false

    companion object {
        fun factory(
            context: Context,
            userId: String,
            displayName: String,
            email: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as GlassTubeApp
                return GlassTubeViewModel(
                    currentUserId = userId,
                    userDisplayName = displayName,
                    userEmail = email,
                    videoDao = app.database.videoDao(),
                    channelDao = app.database.channelDao(),
                    pendingSyncDao = app.database.pendingSyncDao(),
                    preferencesRepository = app.preferencesRepository,
                    networkObserver = app.networkObserver,
                    storageInspector = app.storageInspector,
                    downloadManager = app.downloadManager,
                    syncRepository = app.syncRepository,
                    playbackController = app.playbackController
                ) as T
            }
        }
    }
}
