package com.example.data.network

import android.content.Context
import android.os.StatFs
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class StorageInspector(private val context: Context) {

    fun downloadsDir(): File =
        File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }

    suspend fun inspectStorage(recordedOfflineBytes: Long, downloadedCount: Int): StorageStats =
        withContext(Dispatchers.IO) {
            val dlBytes = folderSize(downloadsDir()).coerceAtLeast(recordedOfflineBytes)
            val cacheBytes = folderSize(context.cacheDir)
            val dbDir = context.getDatabasePath("glasstube_offline.db").parentFile
            val dbBytes = dbDir?.let { folderSize(it) } ?: 0L

            val stat = StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

            StorageStats(
                downloadedVideosBytes = dlBytes,
                offlineCacheBytes = cacheBytes,
                appDatabaseBytes = dbBytes,
                availableDeviceBytes = availableBytes,
                downloadedFileCount = downloadedCount
            )
        }

    suspend fun clearTempCache(): Long = withContext(Dispatchers.IO) {
        val before = folderSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        before
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { folderSize(it) } ?: 0L
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 MB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
            }
        }
    }
}
