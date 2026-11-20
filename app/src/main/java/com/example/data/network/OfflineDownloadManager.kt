package com.example.data.network

import android.content.Context
import android.util.Log
import com.example.data.local.VideoDao
import com.example.model.DownloadQuality
import com.example.model.DownloadStatus
import com.example.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OfflineDownloadManager(
    private val context: Context,
    private val videoDao: VideoDao,
    private val storageInspector: StorageInspector,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(video: VideoItem, quality: DownloadQuality) {
        if (activeJobs[video.id]?.isActive == true) return

        val job = scope.launch(Dispatchers.IO) {
            try {
                videoDao.updateDownloadState(
                    videoId = video.id,
                    status = DownloadStatus.DOWNLOADING.name,
                    progress = 2,
                    quality = quality.name,
                    localPath = video.localFilePath,
                    sizeBytes = video.fileSizeBytes,
                    cachedOffline = video.isCachedOffline
                )

                val targetFile = File(
                    storageInspector.downloadsDir(),
                    "${video.id}_${quality.name.lowercase()}.mp4"
                )

                if (video.streamUrl.startsWith("http", ignoreCase = true)) {
                    val request = Request.Builder().url(video.streamUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IllegalStateException("HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IllegalStateException("Empty response body")
                        val totalBytes = body.contentLength().coerceAtLeast(1L)
                        var downloaded = 0L
                        var lastReported = 2

                        body.byteStream().use { input ->
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(5, 99)
                                    if (pct - lastReported >= 5) {
                                        lastReported = pct
                                        videoDao.updateDownloadState(
                                            videoId = video.id,
                                            status = DownloadStatus.DOWNLOADING.name,
                                            progress = pct,
                                            quality = quality.name,
                                            localPath = null,
                                            sizeBytes = downloaded,
                                            cachedOffline = false
                                        )
                                    }
                                }
                            }
                        }

                        val finalSize = targetFile.length().coerceAtLeast(downloaded)
                        videoDao.updateDownloadState(
                            videoId = video.id,
                            status = DownloadStatus.COMPLETED.name,
                            progress = 100,
                            quality = quality.name,
                            localPath = targetFile.toURI().toString(),
                            sizeBytes = finalSize,
                            cachedOffline = true
                        )
                    }
                } else {
                    // Local / content URI already on device
                    val fallbackUri = video.localFilePath ?: video.streamUrl
                    val estimatedBytes = ((video.durationSec / 60.0).coerceAtLeast(0.5) *
                        quality.estimatedMbPerMin * 1024 * 1024).toLong()
                    videoDao.updateDownloadState(
                        videoId = video.id,
                        status = DownloadStatus.COMPLETED.name,
                        progress = 100,
                        quality = quality.name,
                        localPath = fallbackUri,
                        sizeBytes = estimatedBytes,
                        cachedOffline = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("OfflineDownload", "Download failed for ${video.id}: ${e.message}", e)
                videoDao.updateDownloadState(
                    videoId = video.id,
                    status = DownloadStatus.FAILED.name,
                    progress = 0,
                    quality = quality.name,
                    localPath = video.localFilePath,
                    sizeBytes = video.fileSizeBytes,
                    cachedOffline = video.isCachedOffline
                )
            } finally {
                activeJobs.remove(video.id)
            }
        }
        activeJobs[video.id] = job
    }

    suspend fun removeDownload(video: VideoItem) {
        activeJobs[video.id]?.cancel()
        storageInspector.downloadsDir().listFiles()?.forEach { file ->
            if (file.name.startsWith("${video.id}_")) {
                file.delete()
            }
        }
        videoDao.updateDownloadState(
            videoId = video.id,
            status = DownloadStatus.NONE.name,
            progress = 0,
            quality = null,
            localPath = null,
            sizeBytes = 0L,
            cachedOffline = false
        )
    }
}
