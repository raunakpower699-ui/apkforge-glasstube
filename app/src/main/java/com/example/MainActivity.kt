package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MainTab
import com.example.model.ThemeMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.DownloadQualitySheet
import com.example.ui.components.DynamicIslandPill
import com.example.ui.components.LiquidGlassBottomBar
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.TapGlowOverlayBox
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.VideoPlayerOverlay
import com.example.ui.screens.YouProfileScreen
import com.example.ui.theme.GlassTubeTheme
import com.example.ui.viewmodel.GlassTubeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        Log.d("Auth", "AuthStateListener fired: uid=${auth.currentUser?.uid}")
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = applicationContext as GlassTubeApp
            val settings by app.preferencesRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = com.example.data.local.AppSettings())

            GlassTubeTheme(themeMode = settings.themeMode) {
                TapGlowOverlayBox(modifier = Modifier.fillMaxSize()) {
                    AppRootNavigation(themeMode = settings.themeMode)
                }
            }
        }
    }
}

@Composable
private fun AppRootNavigation(
    themeMode: ThemeMode,
    auth: FirebaseAuth = Firebase.auth
) {
    val authFlowUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var manualAuthUser by remember { mutableStateOf(auth.currentUser) }
    val user = authFlowUser ?: manualAuthUser

    LaunchedEffect(authFlowUser) {
        Log.d("Auth", "AppRootNavigation observed authFlowUser uid=${authFlowUser?.uid}")
        manualAuthUser = authFlowUser
    }

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                val signedIn = auth.currentUser
                Log.i("Auth", "AppRootNavigation onAuthSuccess triggered (uid=${signedIn?.uid})")
                manualAuthUser = signedIn
            }
        )
    } else {
        key(user.uid) {
            val context = LocalContext.current
            val viewModel: GlassTubeViewModel = viewModel(
                key = user.uid,
                factory = GlassTubeViewModel.factory(
                    context = context,
                    userId = user.uid,
                    displayName = user.displayName ?: "GlassTube Viewer",
                    email = user.email ?: ""
                )
            )
            GlassTubeMainHost(
                viewModel = viewModel,
                displayName = user.displayName ?: "GlassTube Viewer",
                email = user.email ?: ""
            )
        }
    }
}

@Composable
private fun GlassTubeMainHost(
    viewModel: GlassTubeViewModel,
    displayName: String,
    email: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val downloadSheetVideo by viewModel.downloadSheetVideo.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val syncState by viewModel.syncBannerState.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val feedVideos by viewModel.personalizedHomeFeed.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val shortsFeed by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val offlineVideos by viewModel.offlineVaultVideos.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val likedVideos by viewModel.likedVideos.collectAsStateWithLifecycle()
    val savedVideos by viewModel.savedVideos.collectAsStateWithLifecycle()
    val topTopics by viewModel.topAffinityTopics.collectAsStateWithLifecycle()

    val player by viewModel.playbackController.player.collectAsStateWithLifecycle()
    val activeVideo by viewModel.playbackController.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackController.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.playbackController.isBuffering.collectAsStateWithLifecycle()
    val hasRenderedFirstFrame by viewModel.playbackController.hasRenderedFirstFrame.collectAsStateWithLifecycle()
    val positionMs by viewModel.playbackController.positionMs.collectAsStateWithLifecycle()
    val bufferedPositionMs by viewModel.playbackController.bufferedPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.playbackController.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackController.playbackSpeed.collectAsStateWithLifecycle()
    val isPlayerExpanded by viewModel.playbackController.isPlayerExpanded.collectAsStateWithLifecycle()
    val isMinimizedToIsland by viewModel.playbackController.isMinimizedToIsland.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.playbackController.selectedQuality.collectAsStateWithLifecycle()
    val playbackNote by viewModel.playbackController.playbackStatusNote.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var notificationPermissionAsked by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // Enforce background playback & background data rules when user switches to home screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.handleAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && !notificationPermissionAsked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionAsked = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    BackHandler(enabled = selectedTab != MainTab.HOME && !isPlayerExpanded && !showSettings) {
        viewModel.selectTab(MainTab.HOME)
    }

    LiquidGlassSurface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = 84.dp)
                    )
                },
                bottomBar = {
                    if (!isPlayerExpanded && !showSettings) {
                        LiquidGlassBottomBar(
                            selectedTab = selectedTab,
                            hasNewSubscriptions = channels.any { it.isSubscribed && it.hasNewUpload },
                            onTabSelected = viewModel::selectTab
                        )
                    }
                }
            ) { _ ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { it / 5 * direction }
                        ) + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(180),
                                targetOffsetX = { -it / 5 * direction }
                            ) + fadeOut(tween(180)))
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        MainTab.HOME -> HomeScreen(
                            feedVideos = feedVideos,
                            shortsVideos = shortsFeed,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            isSearchOpen = isSearchOpen,
                            syncState = syncState,
                            topTopics = topTopics,
                            onSelectCategory = viewModel::selectCategory,
                            onToggleSearch = viewModel::toggleSearch,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSyncClick = viewModel::syncNow,
                            onOpenSettings = viewModel::openSettings,
                            onPlayVideo = { viewModel.playVideo(it, expand = true) },
                            onOpenShort = { shortVideo ->
                                viewModel.playVideo(shortVideo, expand = false)
                                viewModel.selectTab(MainTab.SHORTS)
                            },
                            onDownloadVideo = viewModel::openDownloadSheet,
                            onToggleSave = viewModel::toggleSave,
                            onToggleLike = viewModel::toggleLike
                        )

                        MainTab.SHORTS -> ShortsScreen(
                            shorts = shortsFeed,
                            activeVideoId = activeVideo?.id,
                            isPlaying = isPlaying,
                            player = player,
                            isChannelSubscribed = viewModel::isChannelSubscribed,
                            onPlayShort = { viewModel.playVideo(it, expand = false) },
                            onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                            onToggleLike = viewModel::toggleLike,
                            onToggleDislike = viewModel::toggleDislike,
                            onToggleSave = viewModel::toggleSave,
                            onToggleSubscribe = viewModel::toggleSubscribe,
                            onDownloadClick = viewModel::openDownloadSheet
                        )

                        MainTab.CREATE -> CreateScreen(
                            defaultChannelName = displayName,
                            onPublishVideo = viewModel::addCustomVideo
                        )

                        MainTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            channels = channels,
                            videos = allVideos,
                            onToggleSubscribe = viewModel::toggleSubscribe,
                            onPlayVideo = { viewModel.playVideo(it, expand = true) },
                            onDownloadVideo = viewModel::openDownloadSheet,
                            onToggleSave = viewModel::toggleSave,
                            onToggleLike = viewModel::toggleLike
                        )

                        MainTab.YOU -> YouProfileScreen(
                            displayName = displayName,
                            email = email,
                            syncState = syncState,
                            storageStats = storageStats,
                            offlineVideos = offlineVideos,
                            watchHistory = watchHistory,
                            likedVideos = likedVideos,
                            savedVideos = savedVideos,
                            onSyncNow = viewModel::syncNow,
                            onOpenSettings = viewModel::openSettings,
                            onSignOut = {
                                viewModel.playbackController.closeMiniPlayer()
                                signOutUser(credentialManager, scope)
                            },
                            onPlayVideo = { viewModel.playVideo(it, expand = true) },
                            onRemoveDownload = viewModel::removeOfflineDownload
                        )
                    }
                }
            }

            // Floating Liquid Glass Dynamic Island Pill
            if (settings.dynamicIslandEnabled && !isPlayerExpanded && selectedTab != MainTab.SHORTS) {
                DynamicIslandPill(
                    video = activeVideo,
                    isPlaying = isPlaying,
                    visible = isMinimizedToIsland && activeVideo != null,
                    onTogglePlay = viewModel.playbackController::togglePlayPause,
                    onExpand = viewModel.playbackController::expandFromDynamicIsland,
                    onClose = viewModel.playbackController::closeMiniPlayer,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                )
            }

            // Fullscreen Animated Video Watch Overlay
            AnimatedVisibility(
                visible = isPlayerExpanded && activeVideo != null,
                enter = slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = tween(220),
                    targetOffsetY = { it }
                ) + fadeOut()
            ) {
                val video = activeVideo
                if (video != null) {
                    VideoPlayerOverlay(
                        video = video,
                        relatedVideos = viewModel.relatedVideosFor(video),
                        player = player,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        hasRenderedFirstFrame = hasRenderedFirstFrame,
                        positionMs = positionMs,
                        bufferedPositionMs = bufferedPositionMs,
                        durationMs = durationMs,
                        playbackSpeed = playbackSpeed,
                        selectedQuality = selectedQuality,
                        statusNote = playbackNote,
                        isSubscribed = viewModel.isChannelSubscribed(video.channelId),
                        onMinimizeToIsland = viewModel.playbackController::minimizeToDynamicIsland,
                        onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                        onSeekTo = viewModel.playbackController::seekTo,
                        onSeekBy = viewModel.playbackController::seekBy,
                        onCycleSpeed = viewModel.playbackController::cyclePlaybackSpeed,
                        onSelectQuality = viewModel.playbackController::setQualityLabel,
                        onToggleLike = viewModel::toggleLike,
                        onToggleDislike = viewModel::toggleDislike,
                        onToggleSave = viewModel::toggleSave,
                        onToggleSubscribe = viewModel::toggleSubscribe,
                        onDownloadClick = viewModel::openDownloadSheet,
                        onPlayRelated = { viewModel.playVideo(it, expand = true) }
                    )
                }
            }

            // Animated Settings & Storage Overlay
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetX = { it }
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    animationSpec = tween(200),
                    targetOffsetX = { it }
                ) + fadeOut()
            ) {
                SettingsScreen(
                    settings = settings,
                    storageStats = storageStats,
                    networkStatus = networkStatus,
                    onBack = viewModel::closeSettings,
                    onSelectTheme = viewModel::setThemeMode,
                    onToggleBackgroundPlay = viewModel::setBackgroundPlay,
                    onToggleBackgroundDataPlay = viewModel::setBackgroundDataPlay,
                    onToggleWifiOnlyDownloads = viewModel::setWifiOnlyDownloads,
                    onToggleDynamicIsland = viewModel::setDynamicIslandEnabled,
                    onToggleAutoSync = viewModel::setAutoSyncOnReconnect,
                    onSelectDefaultQuality = viewModel::setDefaultDownloadQuality,
                    onClearCache = viewModel::clearTempCache
                )
            }

            val dlVideo = downloadSheetVideo
            if (dlVideo != null) {
                DownloadQualitySheet(
                    video = dlVideo,
                    defaultQuality = settings.defaultDownloadQuality,
                    onDismiss = viewModel::closeDownloadSheet,
                    onSelectQuality = { q -> viewModel.startDownload(dlVideo, q) }
                )
            }
        }
    }
}
