package com.example

import android.app.Application
import com.example.data.local.GlassTubeDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.network.NetworkObserver
import com.example.data.network.OfflineDownloadManager
import com.example.data.network.StorageInspector
import com.example.data.remote.FirestoreSyncRepository
import com.example.media.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlassTubeApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var database: GlassTubeDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var networkObserver: NetworkObserver
        private set
    lateinit var storageInspector: StorageInspector
        private set
    lateinit var downloadManager: OfflineDownloadManager
        private set
    lateinit var syncRepository: FirestoreSyncRepository
        private set
    lateinit var playbackController: PlaybackController
        private set

    override fun onCreate() {
        super.onCreate()
        database = GlassTubeDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        networkObserver = NetworkObserver(this)
        storageInspector = StorageInspector(this)
        downloadManager = OfflineDownloadManager(
            context = this,
            videoDao = database.videoDao(),
            storageInspector = storageInspector,
            scope = applicationScope
        )
        syncRepository = FirestoreSyncRepository(
            context = this,
            videoDao = database.videoDao(),
            channelDao = database.channelDao(),
            pendingSyncDao = database.pendingSyncDao(),
            preferencesRepository = preferencesRepository
        )
        playbackController = PlaybackController(this, applicationScope)

        applicationScope.launch(Dispatchers.IO) {
            GlassTubeDatabase.seedIfEmpty(this@GlassTubeApp, database)
        }
    }
}
