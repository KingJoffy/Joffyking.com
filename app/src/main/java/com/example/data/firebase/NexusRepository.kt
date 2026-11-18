package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Friendship
import com.example.data.model.GameCategory
import com.example.data.model.GameCatalog
import com.example.data.model.GamerProfile
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LiveStream
import com.example.data.model.MatchRecord
import com.example.data.model.SportHighlight
import com.example.data.model.StreamMessage
import com.example.data.model.VideoCall
import com.example.data.model.ViewerSignal
import com.example.data.model.VoiceCommentaryCatalog
import com.example.data.model.VoiceParticipant
import com.example.data.model.VoiceSignal
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NexusRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    suspend fun ensureDefaultProfile(user: FirebaseUser): Result<Unit> {
        val path = "gamer_profiles/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Authenticated user mismatch." }
            val docRef = db.collection("gamer_profiles").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val rawName = user.displayName
                    ?.replace(Regex("[^a-zA-Z0-9_ ]"), "")
                    ?.trim()
                    ?.take(24)
                    ?: ""
                val username = if (rawName.length >= 3) rawName else "Gamer_${uid.take(6)}"
                val payload = mapOf(
                    "userId" to uid,
                    "username" to username,
                    "usernameLower" to username.lowercase(),
                    "bio" to "Live on NexusCast. Always down for ranked or co-op!",
                    "avatarData" to GameCatalog.avatarPresets.first().key,
                    "favoriteGame" to GameCatalog.categories.first().title,
                    "onlineStatus" to "online",
                    "discoverable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                ensureLeaderboardEntry(uid, username, GameCatalog.avatarPresets.first().key, GameCatalog.categories.first().title)
            } else {
                docRef.update(
                    mapOf(
                        "onlineStatus" to "online",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val existingName = snapshot.getString("username") ?: "Gamer_${uid.take(6)}"
                val existingAvatar = snapshot.getString("avatarData") ?: ""
                val existingGame = snapshot.getString("favoriteGame") ?: GameCatalog.categories.first().title
                ensureLeaderboardEntry(uid, existingName, existingAvatar, existingGame)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMyProfile(userId: String): Flow<GamerProfile?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        emitAll(
            db.collection("gamer_profiles").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        GamerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    fun observeDiscoverableProfiles(): Flow<List<GamerProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("gamer_profiles")
                .whereEqualTo("discoverable", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GamerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.userId.isNotBlank() && it.userId != uid }
                        .sortedBy { it.usernameLower }
                }
        )
    }

    suspend fun updateProfile(
        username: String,
        bio: String,
        avatarData: String,
        favoriteGame: String,
        discoverable: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "gamer_profiles/$uid"
        return try {
            val verifiedUid = requireUserId()
            val cleanName = username.trim()
            val cleanBio = bio.trim()
            val cleanGame = favoriteGame.trim()
            require(cleanName.length in 3..24) { "Username must be between 3 and 24 characters." }
            require(cleanBio.length <= 240) { "Biography must be 240 characters or fewer." }
            require(cleanGame.length <= 60) { "Favorite game title is too long." }
            require(avatarData.length <= 120_000) { "Avatar payload exceeds size limit." }

            val updates = mutableMapOf<String, Any>(
                "username" to cleanName,
                "usernameLower" to cleanName.lowercase(),
                "bio" to cleanBio,
                "favoriteGame" to cleanGame,
                "discoverable" to discoverable,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarData.isNotBlank()) {
                updates["avatarData"] = avatarData
            }
            db.collection("gamer_profiles").document(verifiedUid).update(updates).await()
            val lbRef = db.collection("leaderboard").document(verifiedUid)
            val lbSnap = lbRef.get().await()
            if (lbSnap.exists()) {
                val lbUpdates = mutableMapOf<String, Any>(
                    "username" to cleanName,
                    "favoriteGame" to cleanGame,
                    "listed" to discoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (avatarData.isNotBlank()) {
                    lbUpdates["avatarData"] = avatarData
                }
                lbRef.update(lbUpdates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(status: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "gamer_profiles/$uid"
        return try {
            val verifiedUid = requireUserId()
            db.collection("gamer_profiles").document(verifiedUid).update(
                mapOf(
                    "onlineStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeLiveStreams(): Flow<List<LiveStream>> = flow {
        requireUserId()
        emitAll(
            db.collection("streams")
                .whereEqualTo("status", "live")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LiveStream::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.viewerCount }
                }
        )
    }

    fun observeStream(streamId: String): Flow<LiveStream?> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        LiveStream::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun startLiveStream(
        hostProfile: GamerProfile,
        title: String,
        category: GameCategory,
        region: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
        voiceChannelEnabled: Boolean = true,
        voiceTopic: String = "Tactics & Feedback"
    ): Result<String> {
        val docRef = db.collection("streams").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length in 3..100) { "Stream title must be 3 to 100 characters." }

            val payload = mutableMapOf<String, Any>(
                "hostId" to uid,
                "hostUsername" to hostProfile.username.ifBlank { "Streamer" }.take(24),
                "title" to cleanTitle,
                "titleLower" to cleanTitle.lowercase(),
                "gameId" to category.id,
                "gameTitle" to category.title,
                "gameTitleLower" to category.title.lowercase(),
                "categoryGenre" to category.genre,
                "region" to region.take(30),
                "status" to "live",
                "viewerCount" to 1L,
                "micEnabled" to micEnabled,
                "cameraEnabled" to cameraEnabled,
                "voiceChannelEnabled" to voiceChannelEnabled,
                "voiceTopic" to voiceTopic.trim().ifBlank { "Tactics & Feedback" }.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (hostProfile.avatarData.isNotBlank()) {
                payload["hostAvatarData"] = hostProfile.avatarData
            }
            docRef.set(payload).await()
            db.collection("gamer_profiles").document(uid).update(
                mapOf(
                    "onlineStatus" to "streaming",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStreamControls(
        streamId: String,
        micEnabled: Boolean,
        cameraEnabled: Boolean
    ): Result<Unit> {
        val path = "streams/$streamId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId).update(
                mapOf(
                    "micEnabled" to micEnabled,
                    "cameraEnabled" to cameraEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustStreamViewerCount(streamId: String, newCount: Long): Result<Unit> {
        val path = "streams/$streamId"
        return try {
            requireUserId()
            val clamped = newCount.coerceIn(0L, 1_000_000L)
            db.collection("streams").document(streamId).update(
                mapOf(
                    "viewerCount" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        val path = "streams/$streamId"
        return try {
            val uid = requireUserId()
            db.collection("streams").document(streamId).update(
                mapOf(
                    "status" to "ended",
                    "viewerCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("gamer_profiles").document(uid).update(
                mapOf(
                    "onlineStatus" to "online",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeStreamMessages(streamId: String): Flow<List<StreamMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId).collection("messages")
                .whereEqualTo("streamId", streamId)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StreamMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun sendStreamMessage(
        streamId: String,
        senderProfile: GamerProfile,
        text: String,
        messageType: String = "chat"
    ): Result<Unit> {
        val docRef = db.collection("streams").document(streamId).collection("messages").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(cleanText.isNotEmpty() && cleanText.length <= 300) {
                "Message must be 1 to 300 characters."
            }
            val payload = mutableMapOf<String, Any>(
                "streamId" to streamId,
                "senderId" to uid,
                "senderUsername" to senderProfile.username.ifBlank { "Gamer" }.take(24),
                "text" to cleanText,
                "messageType" to messageType,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (senderProfile.avatarData.isNotBlank()) {
                payload["senderAvatarData"] = senderProfile.avatarData
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // Stream WebRTC Viewer Signaling
    fun observeHostViewerSignals(streamId: String, hostId: String): Flow<List<ViewerSignal>> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId).collection("viewer_signals")
                .whereEqualTo("hostId", hostId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ViewerSignal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    fun observeViewerSignal(streamId: String, viewerId: String): Flow<ViewerSignal?> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId)
                .collection("viewer_signals").document(viewerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        ViewerSignal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun createOrUpdateViewerOffer(
        streamId: String,
        hostId: String,
        offerSdp: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "streams/$streamId/viewer_signals/$uid"
        return try {
            val verifiedUid = requireUserId()
            val payload = mapOf(
                "streamId" to streamId,
                "hostId" to hostId,
                "viewerId" to verifiedUid,
                "status" to "requesting",
                "offerSdp" to offerSdp.take(20_000),
                "viewerCandidates" to emptyList<String>(),
                "hostCandidates" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("streams").document(streamId)
                .collection("viewer_signals").document(verifiedUid)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun answerViewerOffer(
        streamId: String,
        viewerId: String,
        answerSdp: String
    ): Result<Unit> {
        val path = "streams/$streamId/viewer_signals/$viewerId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId)
                .collection("viewer_signals").document(viewerId)
                .update(
                    mapOf(
                        "answerSdp" to answerSdp.take(20_000),
                        "status" to "connected",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun appendViewerSignalCandidate(
        streamId: String,
        viewerId: String,
        isHost: Boolean,
        candidate: String
    ): Result<Unit> {
        val path = "streams/$streamId/viewer_signals/$viewerId"
        return try {
            requireUserId()
            val field = if (isHost) "hostCandidates" else "viewerCandidates"
            db.collection("streams").document(streamId)
                .collection("viewer_signals").document(viewerId)
                .update(
                    mapOf(
                        field to FieldValue.arrayUnion(candidate.take(1000)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Friendships
    fun observeFriendships(userId: String): Flow<List<Friendship>> = flow {
        requireUserId()
        emitAll(
            db.collection("friendships")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Friendship::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        )
    }

    fun deterministicFriendshipId(uidA: String, uidB: String): String {
        return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
    }

    suspend fun sendFriendRequest(
        myProfile: GamerProfile,
        targetProfile: GamerProfile
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val friendshipId = deterministicFriendshipId(uid, targetProfile.userId)
        val path = "friendships/$friendshipId"
        return try {
            val verifiedUid = requireUserId()
            require(verifiedUid != targetProfile.userId) { "Cannot send a friend request to yourself." }
            val docRef = db.collection("friendships").document(friendshipId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingStatus = existingSnap.getString("status")
                if (existingStatus == "declined" && existingSnap.getString("requesterId") == verifiedUid) {
                    val updates = mutableMapOf<String, Any>(
                        "status" to "pending",
                        "requesterName" to myProfile.username.ifBlank { "Gamer" }.take(24),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (myProfile.avatarData.isNotBlank()) {
                        updates["requesterAvatarData"] = myProfile.avatarData
                    }
                    docRef.update(updates).await()
                }
            } else {
                val payload = mutableMapOf<String, Any>(
                    "requesterId" to verifiedUid,
                    "recipientId" to targetProfile.userId,
                    "requesterName" to myProfile.username.ifBlank { "Gamer" }.take(24),
                    "recipientName" to targetProfile.username.ifBlank { "Gamer" }.take(24),
                    "memberUids" to listOf(verifiedUid, targetProfile.userId),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (myProfile.avatarData.isNotBlank()) {
                    payload["requesterAvatarData"] = myProfile.avatarData
                }
                if (targetProfile.avatarData.isNotBlank()) {
                    payload["recipientAvatarData"] = targetProfile.avatarData
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(
        friendshipId: String,
        accept: Boolean,
        myProfile: GamerProfile
    ): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to if (accept) "accepted" else "declined",
                "recipientName" to myProfile.username.ifBlank { "Gamer" }.take(24),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (myProfile.avatarData.isNotBlank()) {
                updates["recipientAvatarData"] = myProfile.avatarData
            }
            db.collection("friendships").document(friendshipId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            db.collection("friendships").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // 1-on-1 Gamer Video Calls
    fun observeMyCalls(userId: String): Flow<List<VideoCall>> = flow {
        requireUserId()
        emitAll(
            db.collection("video_calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VideoCall::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        )
    }

    fun observeCall(callId: String): Flow<VideoCall?> = flow {
        requireUserId()
        emitAll(
            db.collection("video_calls").document(callId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        VideoCall::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun startVideoCall(
        myProfile: GamerProfile,
        targetUserId: String,
        targetUsername: String,
        targetAvatarData: String,
        gameContext: String,
        offerSdp: String
    ): Result<String> {
        val docRef = db.collection("video_calls").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            require(uid != targetUserId) { "Cannot video call yourself." }
            val payload = mutableMapOf<String, Any>(
                "callerId" to uid,
                "calleeId" to targetUserId,
                "callerName" to myProfile.username.ifBlank { "Gamer" }.take(24),
                "calleeName" to targetUsername.ifBlank { "Gamer" }.take(24),
                "participantUids" to listOf(uid, targetUserId),
                "gameContext" to gameContext.take(60),
                "status" to "ringing",
                "offerSdp" to offerSdp.take(20_000),
                "callerCandidates" to emptyList<String>(),
                "calleeCandidates" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (myProfile.avatarData.isNotBlank()) {
                payload["callerAvatarData"] = myProfile.avatarData
            }
            if (targetAvatarData.isNotBlank()) {
                payload["calleeAvatarData"] = targetAvatarData
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun answerVideoCall(callId: String, answerSdp: String): Result<Unit> {
        val path = "video_calls/$callId"
        return try {
            requireUserId()
            db.collection("video_calls").document(callId).update(
                mapOf(
                    "status" to "active",
                    "answerSdp" to answerSdp.take(20_000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVideoCallOffer(callId: String, offerSdp: String): Result<Unit> {
        val path = "video_calls/$callId"
        return try {
            requireUserId()
            db.collection("video_calls").document(callId).update(
                mapOf(
                    "offerSdp" to offerSdp.take(20_000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun appendCallCandidate(
        callId: String,
        isCaller: Boolean,
        candidate: String
    ): Result<Unit> {
        val path = "video_calls/$callId"
        return try {
            requireUserId()
            val field = if (isCaller) "callerCandidates" else "calleeCandidates"
            db.collection("video_calls").document(callId).update(
                mapOf(
                    field to FieldValue.arrayUnion(candidate.take(1000)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun endOrDeclineVideoCall(callId: String, status: String): Result<Unit> {
        val path = "video_calls/$callId"
        return try {
            requireUserId()
            db.collection("video_calls").document(callId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Global Competitive Leaderboard & Match Records ---

    private suspend fun ensureLeaderboardEntry(
        uid: String,
        username: String,
        avatarData: String,
        favoriteGame: String
    ) {
        val lbRef = db.collection("leaderboard").document(uid)
        val snap = lbRef.get().await()
        if (!snap.exists()) {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "username" to username.take(24),
                "favoriteGame" to favoriteGame.take(60),
                "wins" to 0L,
                "losses" to 0L,
                "rating" to 1000L,
                "currentStreak" to 0L,
                "bestStreak" to 0L,
                "listed" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarData.isNotBlank()) {
                payload["avatarData"] = avatarData
            }
            lbRef.set(payload).await()
        }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        requireUserId()
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("listed", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LeaderboardEntry> { it.rating }
                            .thenByDescending { it.wins }
                            .thenBy { it.losses }
                    )
                }
        )
    }

    fun observeMyMatchHistory(userId: String): Flow<List<MatchRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated user mismatch." }
        emitAll(
            db.collection("match_records")
                .whereEqualTo("userId", uid)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun recordMatchResult(
        profile: GamerProfile,
        category: GameCategory,
        outcome: String,
        opponentName: String,
        scoreSummary: String,
        myScore: Long? = null,
        opponentScore: Long? = null,
        matchMode: String = "",
        wagerPoints: Long = 0L,
        payoutPoints: Long = 0L,
        notes: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val matchRef = db.collection("match_records").document()
        val lbRef = db.collection("leaderboard").document(uid)
        return try {
            val verifiedUid = requireUserId()
            val normalizedOutcome = when (outcome.lowercase()) {
                "win", "loss", "draw" -> outcome.lowercase()
                else -> "win"
            }
            val cleanName = profile.username.ifBlank { "Gamer_${verifiedUid.take(5)}" }.take(24)
            val cleanOpponent = opponentName.trim().take(40)
            val cleanMyScore = myScore?.coerceIn(0L, 9999L)
            val cleanOppScore = opponentScore?.coerceIn(0L, 9999L)
            val resolvedScoreSummary = scoreSummary.trim().ifBlank {
                if (cleanMyScore != null && cleanOppScore != null) {
                    "$cleanMyScore - $cleanOppScore"
                } else {
                    ""
                }
            }.take(40)
            val cleanMode = matchMode.trim().take(40)
            val cleanWager = wagerPoints.coerceIn(0L, 1_000_000L)
            val cleanPayout = payoutPoints.coerceIn(0L, 5_000_000L)
            val cleanNotes = notes.trim().take(160)

            val lbSnap = lbRef.get().await()
            val batch = db.batch()

            val matchPayload = mutableMapOf<String, Any>(
                "userId" to verifiedUid,
                "username" to cleanName,
                "gameId" to category.id.take(40),
                "gameTitle" to category.title.take(60),
                "outcome" to normalizedOutcome,
                "wagerPoints" to cleanWager,
                "payoutPoints" to cleanPayout,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanOpponent.isNotEmpty()) matchPayload["opponentName"] = cleanOpponent
            if (resolvedScoreSummary.isNotEmpty()) matchPayload["scoreSummary"] = resolvedScoreSummary
            if (cleanMyScore != null) matchPayload["myScore"] = cleanMyScore
            if (cleanOppScore != null) matchPayload["opponentScore"] = cleanOppScore
            if (cleanMode.isNotEmpty()) matchPayload["matchMode"] = cleanMode
            if (cleanNotes.isNotEmpty()) matchPayload["notes"] = cleanNotes
            batch.set(matchRef, matchPayload)

            if (!lbSnap.exists()) {
                val initWins = if (normalizedOutcome == "win") 1L else 0L
                val initLosses = if (normalizedOutcome == "loss") 1L else 0L
                val initRating = when (normalizedOutcome) {
                    "win" -> 1025L
                    "loss" -> 985L
                    else -> 1000L
                }
                val initStreak = if (normalizedOutcome == "win") 1L else 0L
                val lbPayload = mutableMapOf<String, Any>(
                    "userId" to verifiedUid,
                    "username" to cleanName,
                    "favoriteGame" to category.title,
                    "wins" to initWins,
                    "losses" to initLosses,
                    "rating" to initRating,
                    "currentStreak" to initStreak,
                    "bestStreak" to initStreak,
                    "listed" to profile.discoverable,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (profile.avatarData.isNotBlank()) {
                    lbPayload["avatarData"] = profile.avatarData
                }
                batch.set(lbRef, lbPayload)
            } else {
                val curWins = lbSnap.getLong("wins") ?: 0L
                val curLosses = lbSnap.getLong("losses") ?: 0L
                val curRating = lbSnap.getLong("rating") ?: 1000L
                val curStreak = lbSnap.getLong("currentStreak") ?: 0L
                val curBest = lbSnap.getLong("bestStreak") ?: 0L

                val nextWins = if (normalizedOutcome == "win") curWins + 1L else curWins
                val nextLosses = if (normalizedOutcome == "loss") curLosses + 1L else curLosses
                val nextRating = when (normalizedOutcome) {
                    "win" -> (curRating + 25L).coerceAtMost(100_000L)
                    "loss" -> (curRating - 15L).coerceAtLeast(100L)
                    else -> curRating
                }
                val nextStreak = when (normalizedOutcome) {
                    "win" -> curStreak + 1L
                    "loss" -> 0L
                    else -> curStreak
                }
                val nextBest = maxOf(curBest, nextStreak)

                val lbUpdates = mutableMapOf<String, Any>(
                    "username" to cleanName,
                    "favoriteGame" to category.title,
                    "wins" to nextWins,
                    "losses" to nextLosses,
                    "rating" to nextRating,
                    "currentStreak" to nextStreak,
                    "bestStreak" to nextBest,
                    "listed" to profile.discoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (profile.avatarData.isNotBlank()) {
                    lbUpdates["avatarData"] = profile.avatarData
                }
                batch.update(lbRef, lbUpdates)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "leaderboard/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteMatchRecord(matchId: String): Result<Unit> {
        val path = "match_records/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank()) { "Match ID cannot be blank." }
            db.collection("match_records").document(matchId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Sports Highlights & Community Reels ---

    fun observeSportHighlights(): Flow<List<SportHighlight>> = flow {
        requireUserId()
        emitAll(
            db.collection("sport_highlights")
                .whereEqualTo("featured", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SportHighlight::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<SportHighlight> { it.hypeCount }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
        )
    }

    suspend fun publishSportHighlight(
        profile: GamerProfile,
        category: GameCategory,
        sportLeague: String,
        title: String,
        playType: String,
        statLine: String,
        quarterOrTime: String,
        description: String
    ): Result<String> {
        val docRef = db.collection("sport_highlights").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.length >= 3) { "Highlight title must be at least 3 characters." }
            val cleanAuthor = profile.username.ifBlank { "Gamer_${uid.take(5)}" }.take(24)

            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorUsername" to cleanAuthor,
                "gameId" to category.id.take(40),
                "gameTitle" to category.title.take(60),
                "sportLeague" to sportLeague.trim().ifBlank { category.genre }.take(30),
                "title" to cleanTitle,
                "playType" to playType.trim().ifBlank { "Highlight" }.take(40),
                "hypeCount" to 0L,
                "hypedByUids" to emptyList<String>(),
                "featured" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (profile.avatarData.isNotBlank()) payload["authorAvatarData"] = profile.avatarData
            if (statLine.trim().isNotEmpty()) payload["statLine"] = statLine.trim().take(80)
            if (quarterOrTime.trim().isNotEmpty()) payload["quarterOrTime"] = quarterOrTime.trim().take(30)
            if (description.trim().isNotEmpty()) payload["description"] = description.trim().take(300)

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleHighlightHype(highlight: SportHighlight): Result<Unit> {
        val path = "sport_highlights/${highlight.id}"
        return try {
            val uid = requireUserId()
            val alreadyHyped = highlight.hypedByUids.contains(uid)
            val nextList = if (alreadyHyped) {
                highlight.hypedByUids.filterNot { it == uid }
            } else {
                (highlight.hypedByUids + uid).distinct().take(500)
            }
            val nextCount = if (alreadyHyped) {
                (highlight.hypeCount - 1L).coerceAtLeast(0L)
            } else {
                highlight.hypeCount + 1L
            }
            db.collection("sport_highlights").document(highlight.id).update(
                mapOf(
                    "hypeCount" to nextCount,
                    "hypedByUids" to nextList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSportHighlight(highlightId: String): Result<Unit> {
        val path = "sport_highlights/$highlightId"
        return try {
            requireUserId()
            db.collection("sport_highlights").document(highlightId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Real-Time Audio-Only Voice Commentary Channel ---

    suspend fun updateStreamVoiceSettings(
        streamId: String,
        voiceChannelEnabled: Boolean,
        voiceTopic: String
    ): Result<Unit> {
        val path = "streams/$streamId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId).update(
                mapOf(
                    "voiceChannelEnabled" to voiceChannelEnabled,
                    "voiceTopic" to voiceTopic.trim().ifBlank { "Tactics & Feedback" }.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeVoiceParticipants(streamId: String): Flow<List<VoiceParticipant>> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId).collection("voice_participants")
                .whereEqualTo("streamId", streamId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VoiceParticipant::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<VoiceParticipant> { if (it.role == "host") 0 else 1 }
                            .thenBy { if (it.muted) 1 else 0 }
                            .thenBy { it.joinedAt ?: Timestamp(0, 0) }
                    )
                }
        )
    }

    suspend fun joinVoiceChannel(
        streamId: String,
        hostId: String,
        profile: GamerProfile,
        focusTag: String,
        muted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "streams/$streamId/voice_participants/$uid"
        return try {
            val verifiedUid = requireUserId()
            val role = if (verifiedUid == hostId) "host" else "commentator"
            val normalizedFocus = if (focusTag in VoiceCommentaryCatalog.focusTags) focusTag else "Tactics"
            val payload = mutableMapOf<String, Any>(
                "streamId" to streamId,
                "hostId" to hostId,
                "userId" to verifiedUid,
                "username" to profile.username.ifBlank { "Gamer_${verifiedUid.take(5)}" }.take(24),
                "role" to role,
                "focusTag" to normalizedFocus,
                "muted" to muted,
                "hostMuted" to false,
                "speakingStatus" to if (muted) "muted" else "active",
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (profile.avatarData.isNotBlank()) {
                payload["avatarData"] = profile.avatarData
            }
            db.collection("streams").document(streamId)
                .collection("voice_participants").document(verifiedUid)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMyVoiceParticipant(
        streamId: String,
        focusTag: String,
        muted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "streams/$streamId/voice_participants/$uid"
        return try {
            val verifiedUid = requireUserId()
            val normalizedFocus = if (focusTag in VoiceCommentaryCatalog.focusTags) focusTag else "Tactics"
            db.collection("streams").document(streamId)
                .collection("voice_participants").document(verifiedUid)
                .update(
                    mapOf(
                        "focusTag" to normalizedFocus,
                        "muted" to muted,
                        "speakingStatus" to if (muted) "muted" else "active",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun hostModerateVoiceParticipant(
        streamId: String,
        targetUserId: String,
        hostMuted: Boolean
    ): Result<Unit> {
        val path = "streams/$streamId/voice_participants/$targetUserId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId)
                .collection("voice_participants").document(targetUserId)
                .update(
                    mapOf(
                        "muted" to hostMuted,
                        "hostMuted" to hostMuted,
                        "speakingStatus" to if (hostMuted) "muted" else "active",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveVoiceChannel(
        streamId: String,
        targetUserId: String
    ): Result<Unit> {
        val path = "streams/$streamId/voice_participants/$targetUserId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId)
                .collection("voice_participants").document(targetUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeMyVoiceSignals(streamId: String, myUid: String): Flow<List<VoiceSignal>> = flow {
        requireUserId()
        emitAll(
            db.collection("streams").document(streamId).collection("voice_signals")
                .whereEqualTo("streamId", streamId)
                .whereArrayContains("participantUids", myUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VoiceSignal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun upsertVoiceOffer(
        streamId: String,
        responderId: String,
        offerSdp: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val signalId = VoiceCommentaryCatalog.deterministicPairSignalId(uid, responderId)
        val path = "streams/$streamId/voice_signals/$signalId"
        return try {
            val verifiedUid = requireUserId()
            val deterministicId = VoiceCommentaryCatalog.deterministicPairSignalId(verifiedUid, responderId)
            val payload = mapOf(
                "streamId" to streamId,
                "initiatorId" to verifiedUid,
                "responderId" to responderId,
                "participantUids" to listOf(verifiedUid, responderId),
                "status" to "offering",
                "offerSdp" to offerSdp.take(20_000),
                "initiatorCandidates" to emptyList<String>(),
                "responderCandidates" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("streams").document(streamId)
                .collection("voice_signals").document(deterministicId)
                .set(payload)
                .await()
            Result.success(deterministicId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun answerVoiceOffer(
        streamId: String,
        signalId: String,
        answerSdp: String
    ): Result<Unit> {
        val path = "streams/$streamId/voice_signals/$signalId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId)
                .collection("voice_signals").document(signalId)
                .update(
                    mapOf(
                        "answerSdp" to answerSdp.take(20_000),
                        "status" to "connected",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun appendVoiceIceCandidate(
        streamId: String,
        signalId: String,
        isInitiator: Boolean,
        candidate: String
    ): Result<Unit> {
        val path = "streams/$streamId/voice_signals/$signalId"
        return try {
            requireUserId()
            val field = if (isInitiator) "initiatorCandidates" else "responderCandidates"
            db.collection("streams").document(streamId)
                .collection("voice_signals").document(signalId)
                .update(
                    mapOf(
                        field to FieldValue.arrayUnion(candidate.take(1000)),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVoiceSignal(streamId: String, signalId: String): Result<Unit> {
        val path = "streams/$streamId/voice_signals/$signalId"
        return try {
            requireUserId()
            db.collection("streams").document(streamId)
                .collection("voice_signals").document(signalId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
