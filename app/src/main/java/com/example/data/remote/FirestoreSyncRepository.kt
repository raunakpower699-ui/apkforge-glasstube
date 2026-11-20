package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.ChannelDao
import com.example.data.local.PendingSyncActionEntity
import com.example.data.local.PendingSyncDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.VideoDao
import com.example.model.ChannelSubscriptionDto
import com.example.model.CustomVideoDto
import com.example.model.UserProfileDto
import com.example.model.VideoStateDto
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val videoDao: VideoDao,
    private val channelDao: ChannelDao,
    private val pendingSyncDao: PendingSyncDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(
        context: Context,
        videoDao: VideoDao,
        channelDao: ChannelDao,
        pendingSyncDao: PendingSyncDao,
        preferencesRepository: UserPreferencesRepository
    ) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        videoDao = videoDao,
        channelDao = channelDao,
        pendingSyncDao = pendingSyncDao,
        preferencesRepository = preferencesRepository
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before syncing.")

    fun observeUserProfile(userId: String): Flow<UserProfileDto?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfileDto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRemoteVideoStates(userId: String): Flow<List<VideoStateDto>> = flow {
        val path = "users/$userId/videoStates"
        emitAll(
            db.collection("users").document(userId).collection("videoStates")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VideoStateDto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRemoteSubscriptions(userId: String): Flow<List<ChannelSubscriptionDto>> = flow {
        val path = "users/$userId/subscriptions"
        emitAll(
            db.collection("users").document(userId).collection("subscriptions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChannelSubscriptionDto::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun enqueueAction(entityType: String, targetId: String) {
        pendingSyncDao.enqueue(
            PendingSyncActionEntity(
                entityType = entityType,
                targetId = targetId
            )
        )
    }

    suspend fun syncProfileNow(userId: String, preferredTopics: List<String>): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val user = auth.currentUser
            val settings = preferencesRepository.settingsFlow.first()
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()

            val displayName = (user?.displayName?.takeIf { it.isNotBlank() } ?: "GlassTube Viewer").take(60)
            val email = user?.email?.take(120)
            val topics = preferredTopics.distinct().take(15)

            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "preferredTopics" to topics,
                    "themeMode" to settings.themeMode.name,
                    "backgroundPlay" to settings.backgroundPlayEnabled,
                    "backgroundDataPlay" to settings.backgroundDataPlayEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) payload["email"] = email
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "preferredTopics" to topics,
                    "themeMode" to settings.themeMode.name,
                    "backgroundPlay" to settings.backgroundPlayEnabled,
                    "backgroundDataPlay" to settings.backgroundDataPlayEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) updates["email"] = email
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun flushPendingQueue(userId: String, topTopics: List<String>): Result<Int> {
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val actions = pendingSyncDao.getAllPending()
            var syncedCount = 0

            val uniquePairs = actions.map { it.entityType to it.targetId }.distinct()
            for ((entityType, targetId) in uniquePairs) {
                when (entityType) {
                    "VIDEO_STATE" -> syncVideoState(uid, targetId).getOrThrow()
                    "SUBSCRIPTION" -> syncSubscription(uid, targetId).getOrThrow()
                    "CUSTOM_VIDEO" -> syncCustomVideo(uid, targetId).getOrThrow()
                    "PROFILE" -> syncProfileNow(uid, topTopics).getOrThrow()
                }
                syncedCount++
            }

            // Ensure profile exists on first connect
            syncProfileNow(uid, topTopics)

            for (action in actions) {
                pendingSyncDao.deleteBySeqId(action.seqId)
            }
            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/syncQueue")
            Result.failure(e)
        }
    }

    private suspend fun syncVideoState(uid: String, videoId: String): Result<Unit> {
        val docPath = "users/$uid/videoStates/$videoId"
        return try {
            val video = videoDao.getVideoById(videoId) ?: return Result.success(Unit)
            val safeId = sanitizeFirestoreId(video.id)
            val docRef = db.collection("users").document(uid)
                .collection("videoStates").document(safeId)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "videoId" to safeId,
                    "title" to video.title.take(160).ifBlank { "Untitled" },
                    "category" to video.category.take(40).ifBlank { "General" },
                    "liked" to video.isLiked,
                    "saved" to video.isSaved,
                    "watchCount" to video.watchCount.coerceIn(0, 1_000_000).toLong(),
                    "lastPositionMs" to video.lastPositionMs.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to video.title.take(160).ifBlank { "Untitled" },
                        "category" to video.category.take(40).ifBlank { "General" },
                        "liked" to video.isLiked,
                        "saved" to video.isSaved,
                        "watchCount" to video.watchCount.coerceIn(0, 1_000_000).toLong(),
                        "lastPositionMs" to video.lastPositionMs.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun syncSubscription(uid: String, channelId: String): Result<Unit> {
        val docPath = "users/$uid/subscriptions/$channelId"
        return try {
            val channel = channelDao.getChannelById(channelId) ?: return Result.success(Unit)
            val safeId = sanitizeFirestoreId(channel.channelId)
            val docRef = db.collection("users").document(uid)
                .collection("subscriptions").document(safeId)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "channelId" to safeId,
                    "channelName" to channel.channelName.take(80).ifBlank { "Channel" },
                    "category" to channel.category.take(40).ifBlank { "General" },
                    "subscribed" to channel.isSubscribed,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "channelName" to channel.channelName.take(80).ifBlank { "Channel" },
                        "category" to channel.category.take(40).ifBlank { "General" },
                        "subscribed" to channel.isSubscribed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun syncCustomVideo(uid: String, videoId: String): Result<Unit> {
        val docPath = "users/$uid/customVideos/$videoId"
        return try {
            val video = videoDao.getVideoById(videoId) ?: return Result.success(Unit)
            val safeId = sanitizeFirestoreId(video.id)
            val docRef = db.collection("users").document(uid)
                .collection("customVideos").document(safeId)
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to video.title.take(160).ifBlank { "Clip" },
                    "channelName" to video.channelName.take(80).ifBlank { "Creator" },
                    "category" to video.category.take(40).ifBlank { "General" },
                    "streamUrl" to video.streamUrl.take(500).ifBlank { "local://clip" },
                    "durationSec" to video.durationSec.coerceIn(0, 86400).toLong(),
                    "shortVideo" to video.isShort,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to video.title.take(160).ifBlank { "Clip" },
                        "channelName" to video.channelName.take(80).ifBlank { "Creator" },
                        "category" to video.category.take(40).ifBlank { "General" },
                        "streamUrl" to video.streamUrl.take(500).ifBlank { "local://clip" },
                        "durationSec" to video.durationSec.coerceIn(0, 86400).toLong(),
                        "shortVideo" to video.isShort,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pullCloudStateToLocal(userId: String) {
        try {
            val statesSnap = db.collection("users").document(userId)
                .collection("videoStates").get().await()
            val states = statesSnap.toObjects(
                VideoStateDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            for (state in states) {
                val local = videoDao.getVideoById(state.videoId) ?: continue
                videoDao.upsert(
                    local.copy(
                        isLiked = state.liked,
                        isSaved = state.saved,
                        watchCount = maxOf(local.watchCount, state.watchCount.toInt()),
                        lastPositionMs = maxOf(local.lastPositionMs, state.lastPositionMs)
                    )
                )
            }

            val subsSnap = db.collection("users").document(userId)
                .collection("subscriptions").get().await()
            val subs = subsSnap.toObjects(
                ChannelSubscriptionDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            for (sub in subs) {
                val localChannel = channelDao.getChannelById(sub.channelId)
                if (localChannel != null) {
                    channelDao.updateSubscription(sub.channelId, sub.subscribed, System.currentTimeMillis())
                }
            }

            val customSnap = db.collection("users").document(userId)
                .collection("customVideos").get().await()
            val customs = customSnap.toObjects(
                CustomVideoDto::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            for (custom in customs) {
                val id = custom.docId.ifBlank { continue }
                if (videoDao.getVideoById(id) == null) {
                    videoDao.upsert(
                        com.example.data.local.CachedVideoEntity(
                            id = id,
                            title = custom.title,
                            channelId = "ch_user_${userId.take(6)}",
                            channelName = custom.channelName,
                            channelHandle = "@you",
                            category = custom.category,
                            description = "Synced from your GlassTube cloud library.",
                            streamUrl = custom.streamUrl,
                            localFilePath = null,
                            thumbnailResKey = "tech_glass",
                            durationSec = custom.durationSec.toInt(),
                            viewCountText = "Synced",
                            publishedAgo = "Cloud",
                            likesCount = 1L,
                            maxQuality = "4K",
                            isShort = custom.shortVideo,
                            isMusic = custom.category.equals("Music", ignoreCase = true),
                            isLiked = false,
                            isDisliked = false,
                            isSaved = true,
                            isCachedOffline = false,
                            downloadStatus = com.example.model.DownloadStatus.NONE.name,
                            downloadProgress = 0,
                            downloadedQuality = null,
                            fileSizeBytes = 0L,
                            watchCount = 0,
                            lastPositionMs = 0L,
                            lastWatchedAt = 0L,
                            isCustom = true
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$userId")
        }
    }

    private fun sanitizeFirestoreId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "item" }
}
