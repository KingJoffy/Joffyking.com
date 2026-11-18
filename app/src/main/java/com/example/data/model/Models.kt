package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class GamerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val bio: String = "",
    val avatarData: String = "",
    val favoriteGame: String = "",
    val onlineStatus: String = "online",
    val discoverable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostUsername: String = "",
    val hostAvatarData: String = "",
    val title: String = "",
    val titleLower: String = "",
    val gameId: String = "",
    val gameTitle: String = "",
    val gameTitleLower: String = "",
    val categoryGenre: String = "",
    val region: String = "Global",
    val status: String = "live",
    val viewerCount: Long = 1L,
    val micEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    val voiceChannelEnabled: Boolean = true,
    val voiceTopic: String = "Tactics & Feedback",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StreamMessage(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarData: String = "",
    val text: String = "",
    val messageType: String = "chat",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ViewerSignal(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val hostId: String = "",
    val viewerId: String = "",
    val status: String = "requesting",
    val offerSdp: String = "",
    val answerSdp: String = "",
    val viewerCandidates: List<String> = emptyList(),
    val hostCandidates: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VoiceParticipant(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val hostId: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarData: String = "",
    val role: String = "commentator",
    val focusTag: String = "Tactics",
    val muted: Boolean = false,
    val hostMuted: Boolean = false,
    val speakingStatus: String = "active",
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VoiceSignal(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val initiatorId: String = "",
    val responderId: String = "",
    val participantUids: List<String> = emptyList(),
    val status: String = "offering",
    val offerSdp: String = "",
    val answerSdp: String = "",
    val initiatorCandidates: List<String> = emptyList(),
    val responderCandidates: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object VoiceCommentaryCatalog {
    val focusTags: List<String> = listOf(
        "Tactics",
        "Feedback",
        "Play-by-Play",
        "Co-Cast"
    )

    val topics: List<String> = listOf(
        "Tactics & Feedback",
        "Playbook Breakdown",
        "Squad Comms & Calls",
        "Draft & Loadout Critique",
        "Open Mic Q&A"
    )

    fun deterministicPairSignalId(uidA: String, uidB: String): String {
        return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
    }
}

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val recipientId: String = "",
    val requesterName: String = "",
    val requesterAvatarData: String = "",
    val recipientName: String = "",
    val recipientAvatarData: String = "",
    val memberUids: List<String> = emptyList(),
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoCall(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val callerAvatarData: String = "",
    val calleeName: String = "",
    val calleeAvatarData: String = "",
    val participantUids: List<String> = emptyList(),
    val gameContext: String = "",
    val status: String = "ringing",
    val offerSdp: String = "",
    val answerSdp: String = "",
    val callerCandidates: List<String> = emptyList(),
    val calleeCandidates: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GameCategory(
    val id: String,
    val title: String,
    val genre: String,
    val coverRes: Int,
    val accentHex: Long,
    val tags: List<String>,
    val isSports: Boolean = false
)

data class FoodPerkOffer(
    val id: String,
    val title: String,
    val partnerLabel: String,
    val discountBadge: String,
    val promoCode: String,
    val description: String,
    val websiteUrl: String
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarData: String = "",
    val favoriteGame: String = "",
    val wins: Long = 0L,
    val losses: Long = 0L,
    val rating: Long = 1000L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val listed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalMatches: Long get() = wins + losses
    val winRatePercent: Int
        get() = if (totalMatches == 0L) 0 else ((wins * 100L) / totalMatches).toInt()
}

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val gameId: String = "",
    val gameTitle: String = "",
    val outcome: String = "win",
    val opponentName: String = "",
    val scoreSummary: String = "",
    val myScore: Long? = null,
    val opponentScore: Long? = null,
    val matchMode: String = "",
    val wagerPoints: Long = 0L,
    val payoutPoints: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val netVirtualPoints: Long
        get() = payoutPoints - wagerPoints

    val resolvedScoreDisplay: String
        get() = when {
            myScore != null && opponentScore != null -> "$myScore - $opponentScore"
            scoreSummary.isNotBlank() -> scoreSummary
            else -> "—"
        }

    val scoreMargin: Long?
        get() = if (myScore != null && opponentScore != null) myScore - opponentScore else null

    val matchRoiPercent: Int
        get() = if (wagerPoints <= 0L) 0 else ((netVirtualPoints * 100L) / wagerPoints).toInt()
}

data class MatchPerformanceSummary(
    val totalMatches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winRatePercent: Int,
    val totalStakedPoints: Long,
    val totalPayoutPoints: Long,
    val netPoints: Long,
    val roiPercent: Int,
    val currentStreak: Int,
    val bestWinStreak: Int,
    val biggestWinNetPoints: Long,
    val averageMargin: Double?,
    val cumulativeNetCurve: List<Long>
)

fun computeMatchPerformanceSummary(records: List<MatchRecord>): MatchPerformanceSummary {
    if (records.isEmpty()) {
        return MatchPerformanceSummary(
            totalMatches = 0,
            wins = 0,
            losses = 0,
            draws = 0,
            winRatePercent = 0,
            totalStakedPoints = 0L,
            totalPayoutPoints = 0L,
            netPoints = 0L,
            roiPercent = 0,
            currentStreak = 0,
            bestWinStreak = 0,
            biggestWinNetPoints = 0L,
            averageMargin = null,
            cumulativeNetCurve = emptyList()
        )
    }

    val chronological = records.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
    var wins = 0
    var losses = 0
    var draws = 0
    var totalStaked = 0L
    var totalPayout = 0L
    var curStreak = 0
    var bestStreak = 0
    var biggestNet = 0L
    var marginSum = 0L
    var marginCount = 0
    var runningNet = 0L
    val curve = mutableListOf<Long>()

    chronological.forEach { rec ->
        when (rec.outcome) {
            "win" -> {
                wins++
                curStreak++
                if (curStreak > bestStreak) bestStreak = curStreak
            }
            "loss" -> {
                losses++
                curStreak = 0
            }
            else -> {
                draws++
            }
        }
        totalStaked += rec.wagerPoints.coerceAtLeast(0L)
        totalPayout += rec.payoutPoints.coerceAtLeast(0L)
        val net = rec.netVirtualPoints
        if (net > biggestNet) biggestNet = net
        runningNet += net
        curve.add(runningNet)

        rec.scoreMargin?.let { margin ->
            marginSum += margin
            marginCount++
        }
    }

    val decided = wins + losses
    val winRate = if (decided == 0) 0 else ((wins * 100.0) / decided).toInt()
    val netPoints = totalPayout - totalStaked
    val roi = if (totalStaked <= 0L) 0 else ((netPoints * 100L) / totalStaked).toInt()
    val avgMargin = if (marginCount == 0) null else marginSum.toDouble() / marginCount.toDouble()

    return MatchPerformanceSummary(
        totalMatches = records.size,
        wins = wins,
        losses = losses,
        draws = draws,
        winRatePercent = winRate,
        totalStakedPoints = totalStaked,
        totalPayoutPoints = totalPayout,
        netPoints = netPoints,
        roiPercent = roi,
        currentStreak = curStreak,
        bestWinStreak = bestStreak,
        biggestWinNetPoints = biggestNet,
        averageMargin = avgMargin,
        cumulativeNetCurve = curve
    )
}

@Keep
@IgnoreExtraProperties
data class SportHighlight(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarData: String = "",
    val gameId: String = "",
    val gameTitle: String = "",
    val sportLeague: String = "",
    val title: String = "",
    val playType: String = "",
    val statLine: String = "",
    val quarterOrTime: String = "",
    val description: String = "",
    val hypeCount: Long = 0L,
    val hypedByUids: List<String> = emptyList(),
    val featured: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SportMatchupScore(
    val id: String,
    val league: String,
    val gameId: String,
    val awayTeam: String,
    val awayShort: String,
    val awayScore: Int,
    val homeTeam: String,
    val homeShort: String,
    val homeScore: Int,
    val clockStatus: String,
    val liveNow: Boolean,
    val keyPerformers: String,
    val homeWinProbability: Int
)

data class SportStandingRow(
    val rank: Int,
    val team: String,
    val conferenceOrDivision: String,
    val wins: Int,
    val losses: Int,
    val extraStat: String,
    val diffOrRating: String,
    val streak: String,
    val league: String
)

data class SportStatLeader(
    val playerName: String,
    val team: String,
    val position: String,
    val league: String,
    val gameId: String,
    val categoryLabel: String,
    val primaryValue: String,
    val breakdown: String,
    val ratingBadge: String
)

data class ProHighlightSpotlight(
    val id: String,
    val gameId: String,
    val gameTitle: String,
    val league: String,
    val title: String,
    val playType: String,
    val playerAndTeam: String,
    val statLine: String,
    val quarterClock: String,
    val coverRes: Int,
    val playByPlaySteps: List<String>,
    val peakTelemetry: String
)
