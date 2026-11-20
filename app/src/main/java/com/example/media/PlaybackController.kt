package com.example.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.NetworkStatus
import com.example.model.VideoItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private val PlaybackSpeedSteps = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)

class PlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _player = MutableStateFlow<MediaController?>(null)
    val player: StateFlow<MediaController?> = _player.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _hasRenderedFirstFrame = MutableStateFlow(false)
    val hasRenderedFirstFrame: StateFlow<Boolean> = _hasRenderedFirstFrame.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isMinimizedToIsland = MutableStateFlow(false)
    val isMinimizedToIsland: StateFlow<Boolean> = _isMinimizedToIsland.asStateFlow()

    private val _selectedQuality = MutableStateFlow("4K 2160p")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _playbackStatusNote = MutableStateFlow<String?>(null)
    val playbackStatusNote: StateFlow<String?> = _playbackStatusNote.asStateFlow()

    private var tickerJob: Job? = null
    private var pendingPlayRequest: Triple<VideoItem, Boolean, Boolean>? = null

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, GlassPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    _player.value = controller
                    attachListener(controller)
                    startProgressTicker()
                    pendingPlayRequest?.let { (video, expand, isOnline) ->
                        pendingPlayRequest = null
                        playVideo(video, expand = expand, isOnline = isOnline)
                    }
                } catch (_: Exception) {
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun attachListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                _durationMs.value = controller.duration.coerceAtLeast(0L)
                _bufferedPositionMs.value = controller.bufferedPosition.coerceAtLeast(0L)
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }

            override fun onRenderedFirstFrame() {
                _hasRenderedFirstFrame.value = true
            }

            override fun onPlayerError(error: PlaybackException) {
                _isBuffering.value = false
                val active = _currentVideo.value
                val fallbackPath = active?.localFilePath
                if (active != null && !fallbackPath.isNullOrBlank()) {
                    // Automatically switch to local offline audio/video fallback
                    val fallbackItem = buildMediaItem(active, fallbackPath)
                    controller.setMediaItem(fallbackItem)
                    controller.prepare()
                    controller.play()
                    _playbackStatusNote.value = "Playing from Offline Vault cache"
                } else {
                    _playbackStatusNote.value = "Stream unavailable offline. Download in 4K when online."
                }
            }
        })
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val ctrl = _player.value
                if (ctrl != null) {
                    _positionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
                    _bufferedPositionMs.value = ctrl.bufferedPosition.coerceAtLeast(0L)
                    _durationMs.value = ctrl.duration.coerceAtLeast(0L)
                }
                delay(250L)
            }
        }
    }

    fun playVideo(
        video: VideoItem,
        expand: Boolean = true,
        isOnline: Boolean = true
    ) {
        connect()
        _currentVideo.value = video
        _hasRenderedFirstFrame.value = false
        _isBuffering.value = true
        _positionMs.value = video.lastPositionMs.coerceAtLeast(0L)
        _bufferedPositionMs.value = 0L
        _selectedQuality.value = video.downloadedQuality?.resolutionLabel ?: video.maxQuality
        if (expand) {
            _isPlayerExpanded.value = true
            _isMinimizedToIsland.value = false
        } else {
            _isMinimizedToIsland.value = true
        }

        val controller = _player.value
        if (controller == null) {
            pendingPlayRequest = Triple(video, expand, isOnline)
            return
        }

        val resolvedUri = resolvePlayableUri(video, isOnline)
        _playbackStatusNote.value = if (!isOnline && video.localFilePath != null) {
            "Offline Vault • Zero Data Used"
        } else if (isLocalFileReady(video.localFilePath)) {
            "Playing Local 4K/Offline File"
        } else {
            null
        }

        val item = buildMediaItem(video, resolvedUri)
        controller.setMediaItem(item)
        if (video.lastPositionMs > 2000L && video.lastPositionMs < video.durationSec * 1000L) {
            controller.seekTo(video.lastPositionMs)
        }
        controller.setPlaybackSpeed(_playbackSpeed.value)
        controller.prepare()
        controller.play()
    }

    private fun resolvePlayableUri(video: VideoItem, isOnline: Boolean): String {
        val local = video.localFilePath
        if (isLocalFileReady(local)) {
            return local!!
        }
        if (!isOnline && !local.isNullOrBlank()) {
            return local
        }
        return if (isOnline) {
            video.streamUrl
        } else {
            local ?: "android.resource://${context.packageName}/raw/offline_ambient_lofi"
        }
    }

    private fun isLocalFileReady(localPath: String?): Boolean {
        if (localPath.isNullOrBlank()) return false
        if (localPath.startsWith("android.resource://") || localPath.startsWith("content://")) {
            return true
        }
        return try {
            val file = File(Uri.parse(localPath).path ?: localPath)
            file.exists() && file.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    private fun buildMediaItem(video: VideoItem, uriString: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.channelName)
            .setAlbumTitle("GlassTube • ${video.category}")
            .build()

        return MediaItem.Builder()
            .setMediaId(video.id)
            .setUri(uriString)
            .setMediaMetadata(metadata)
            .build()
    }

    fun togglePlayPause() {
        val ctrl = _player.value ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            if (ctrl.playbackState == Player.STATE_ENDED) {
                ctrl.seekTo(0L)
            }
            ctrl.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        _positionMs.value = clamped
        _player.value?.seekTo(clamped)
    }

    fun seekBy(deltaMs: Long) {
        val ctrl = _player.value ?: return
        val maxDur = ctrl.duration.coerceAtLeast(_currentVideo.value?.durationSec?.times(1000L) ?: 0L)
        val target = (ctrl.currentPosition + deltaMs).coerceIn(0L, maxDur.coerceAtLeast(0L))
        _positionMs.value = target
        ctrl.seekTo(target)
    }

    fun cyclePlaybackSpeed() {
        val currentIdx = PlaybackSpeedSteps.indexOfFirst { it == _playbackSpeed.value }
        val nextSpeed = PlaybackSpeedSteps[(currentIdx + 1) % PlaybackSpeedSteps.size]
        _playbackSpeed.value = nextSpeed
        _player.value?.setPlaybackSpeed(nextSpeed)
    }

    fun setQualityLabel(label: String) {
        _selectedQuality.value = label
    }

    fun minimizeToDynamicIsland() {
        _isPlayerExpanded.value = false
        if (_currentVideo.value != null) {
            _isMinimizedToIsland.value = true
        }
    }

    fun expandFromDynamicIsland() {
        if (_currentVideo.value != null) {
            _isMinimizedToIsland.value = false
            _isPlayerExpanded.value = true
        }
    }

    fun closeMiniPlayer() {
        _player.value?.pause()
        _isPlayerExpanded.value = false
        _isMinimizedToIsland.value = false
        _currentVideo.value = null
    }

    fun onAppBackgrounded(
        backgroundPlayEnabled: Boolean,
        backgroundDataPlayEnabled: Boolean,
        networkStatus: NetworkStatus
    ) {
        val ctrl = _player.value ?: return
        if (!ctrl.isPlaying) return

        val current = _currentVideo.value
        val playingFromLocal = isLocalFileReady(current?.localFilePath)

        val allowBackground = when {
            !backgroundPlayEnabled -> false
            playingFromLocal -> true
            networkStatus.isMetered && !backgroundDataPlayEnabled -> false
            else -> true
        }

        if (!allowBackground) {
            ctrl.pause()
            _playbackStatusNote.value = if (!backgroundPlayEnabled) {
                "Paused (Background Play disabled in Settings)"
            } else {
                "Paused (Background Mobile Data disabled in Settings)"
            }
        }
    }

    fun release() {
        tickerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _player.value = null
    }
}
