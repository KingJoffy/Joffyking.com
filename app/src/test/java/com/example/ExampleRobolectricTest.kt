package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NexusCast", appName)
  }

  @Test
  fun `leaderboard entry computes totalMatches and winRatePercent`() {
    val entry = com.example.data.model.LeaderboardEntry(
      userId = "u1",
      username = "Striker",
      wins = 7L,
      losses = 3L,
      rating = 1130L
    )
    assertEquals(10L, entry.totalMatches)
    assertEquals(70, entry.winRatePercent)

    val empty = com.example.data.model.LeaderboardEntry(userId = "u2", username = "Rookie")
    assertEquals(0L, empty.totalMatches)
    assertEquals(0, empty.winRatePercent)
  }

  @Test
  fun `sports catalog includes NBA 2K27 Madden 27 and sports data`() {
    val sports = com.example.data.model.GameCatalog.sportsCategories
    org.junit.Assert.assertTrue(sports.any { it.id == "nba_2k27" && it.title == "NBA 2K27" })
    org.junit.Assert.assertTrue(sports.any { it.id == "madden_27" && it.title == "Madden NFL 27" })
    org.junit.Assert.assertTrue(com.example.data.model.SportsDataCatalog.scoreboards.isNotEmpty())
    org.junit.Assert.assertTrue(com.example.data.model.SportsDataCatalog.standings.isNotEmpty())
    org.junit.Assert.assertTrue(com.example.data.model.SportsDataCatalog.statLeaders.isNotEmpty())
    org.junit.Assert.assertTrue(com.example.data.model.SportsDataCatalog.officialSpotlights.isNotEmpty())
  }

  @Test
  fun `match record and performance summary calculate scores net points ROI and curve`() {
    val t1 = com.google.firebase.Timestamp(1_700_000_000L, 0)
    val t2 = com.google.firebase.Timestamp(1_700_000_100L, 0)
    val t3 = com.google.firebase.Timestamp(1_700_000_200L, 0)
    val t4 = com.google.firebase.Timestamp(1_700_000_300L, 0)

    val m1 = com.example.data.model.MatchRecord(
      id = "m1",
      userId = "u1",
      gameId = "nba_2k27",
      gameTitle = "NBA 2K27",
      outcome = "win",
      opponentName = "ApexPhantom",
      myScore = 88L,
      opponentScore = 80L,
      wagerPoints = 500L,
      payoutPoints = 1000L,
      createdAt = t1
    )
    val m2 = com.example.data.model.MatchRecord(
      id = "m2",
      userId = "u1",
      gameId = "madden_27",
      gameTitle = "Madden NFL 27",
      outcome = "loss",
      opponentName = "GridironKing",
      myScore = 21L,
      opponentScore = 24L,
      wagerPoints = 200L,
      payoutPoints = 0L,
      createdAt = t2
    )
    val m3 = com.example.data.model.MatchRecord(
      id = "m3",
      userId = "u1",
      gameId = "ea_fc_27",
      gameTitle = "EA Sports FC 27",
      outcome = "draw",
      opponentName = "VoidKeeper",
      myScore = 2L,
      opponentScore = 2L,
      wagerPoints = 100L,
      payoutPoints = 100L,
      createdAt = t3
    )
    val m4 = com.example.data.model.MatchRecord(
      id = "m4",
      userId = "u1",
      gameId = "nba_2k27",
      gameTitle = "NBA 2K27",
      outcome = "win",
      opponentName = "NeonSlayer",
      myScore = 95L,
      opponentScore = 85L,
      wagerPoints = 200L,
      payoutPoints = 400L,
      createdAt = t4
    )

    assertEquals("88 - 80", m1.resolvedScoreDisplay)
    assertEquals(8L, m1.scoreMargin)
    assertEquals(500L, m1.netVirtualPoints)
    assertEquals(100, m1.matchRoiPercent)

    val summary = com.example.data.model.computeMatchPerformanceSummary(listOf(m4, m2, m1, m3))
    assertEquals(4, summary.totalMatches)
    assertEquals(2, summary.wins)
    assertEquals(1, summary.losses)
    assertEquals(1, summary.draws)
    assertEquals(66, summary.winRatePercent)
    assertEquals(1000L, summary.totalStakedPoints)
    assertEquals(1500L, summary.totalPayoutPoints)
    assertEquals(500L, summary.netPoints)
    assertEquals(50, summary.roiPercent)
    assertEquals(1, summary.currentStreak)
    assertEquals(1, summary.bestWinStreak)
    assertEquals(500L, summary.biggestWinNetPoints)
    assertEquals(listOf(500L, 300L, 300L, 500L), summary.cumulativeNetCurve)
  }

  @Test
  fun `voice commentary deterministic signal ID and models work correctly`() {
    val pair1 = com.example.data.model.VoiceCommentaryCatalog.deterministicPairSignalId("uid_alpha", "uid_zeta")
    val pair2 = com.example.data.model.VoiceCommentaryCatalog.deterministicPairSignalId("uid_zeta", "uid_alpha")
    assertEquals("uid_alpha_uid_zeta", pair1)
    assertEquals(pair1, pair2)

    val stream = com.example.data.model.LiveStream(
      id = "s1",
      hostId = "uid_alpha",
      title = "Madden 27 Playbook",
      voiceChannelEnabled = true,
      voiceTopic = "Tactics & Feedback"
    )
    assertEquals(true, stream.voiceChannelEnabled)
    assertEquals("Tactics & Feedback", stream.voiceTopic)

    val participant = com.example.data.model.VoiceParticipant(
      streamId = "s1",
      hostId = "uid_alpha",
      userId = "uid_zeta",
      username = "Tactician",
      role = "commentator",
      focusTag = "Tactics"
    )
    assertEquals("commentator", participant.role)
    assertEquals("Tactics", participant.focusTag)
    assertEquals(false, participant.hostMuted)
  }
}
