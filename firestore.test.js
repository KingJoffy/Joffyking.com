const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_gamer_01";
const BOB_UID = "bob_gamer_02";
const CHARLIE_UID = "charlie_gamer_03";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("gamer_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("streams").where("status", "==", "live").get());
  await assertFails(unauthDb.collection("friendships").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("video_calls").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("gamer_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "RogueValkyrie",
      usernameLower: "roguevalkyrie",
      bio: "Testing",
      onlineStatus: "online",
      discoverable: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("GamerProfile: owner create/update, discoverable search, cross-user write denial, ghost field denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("gamer_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "NovaStriker",
      usernameLower: "novastriker",
      bio: "Radiant Duelist & late-night co-op host.",
      avatarData: "preset:cyber_wolf",
      favoriteGame: "Valorant",
      onlineStatus: "online",
      discoverable: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Discoverable query succeeds for Bob
  await assertSucceeds(
    bobDb.collection("gamer_profiles").where("discoverable", "==", true).get()
  );

  // Unfiltered list query fails
  await assertFails(bobDb.collection("gamer_profiles").get());

  // Bob cannot update Alice's profile
  await assertFails(
    bobDb.collection("gamer_profiles").doc(ALICE_UID).update({
      bio: "Hacked bio",
      updatedAt: pastDate(1000),
    })
  );

  // Alice can partially update bio and avatarData
  await assertSucceeds(
    aliceDb.collection("gamer_profiles").doc(ALICE_UID).update({
      bio: "Updated gamer bio!",
      avatarData: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field & immutable createdAt mutation & future timestamp
  await assertFails(
    aliceDb.collection("gamer_profiles").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("gamer_profiles").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("gamer_profiles").doc(ALICE_UID).update({
      bio: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Streams, Chat Messages, and Viewer Signals: lifecycle, category query, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_01").set({
      hostId: ALICE_UID,
      hostUsername: "NovaStriker",
      hostAvatarData: "preset:cyber_wolf",
      title: "Road to Immortal Ranked",
      titleLower: "road to immortal ranked",
      gameId: "valorant",
      gameTitle: "Valorant",
      gameTitleLower: "valorant",
      categoryGenre: "Tactical FPS",
      region: "Global",
      status: "live",
      viewerCount: 1,
      micEnabled: true,
      cameraEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered live stream queries succeed; unfiltered fails
  await assertSucceeds(bobDb.collection("streams").where("status", "==", "live").get());
  await assertSucceeds(
    bobDb.collection("streams").where("status", "==", "live").where("gameId", "==", "valorant").get()
  );
  await assertFails(bobDb.collection("streams").get());

  // Bob cannot hijack stream title, only increment viewerCount
  await assertFails(
    bobDb.collection("streams").doc("stream_01").update({
      title: "Hijacked",
      titleLower: "hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_01").update({
      viewerCount: 2,
      updatedAt: pastDate(1000),
    })
  );

  // Bob can send chat message & query stream messages
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_01").collection("messages").doc("msg_01").set({
      streamId: "stream_01",
      senderId: BOB_UID,
      senderUsername: "ApexPhantom",
      senderAvatarData: "preset:neon_fox",
      text: "Clutch round!",
      messageType: "chat",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_01").collection("messages").where("streamId", "==", "stream_01").get()
  );

  // Bob creates WebRTC viewer signal; Alice answers; Charlie cannot read Bob's signal
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_01").collection("viewer_signals").doc(BOB_UID).set({
      streamId: "stream_01",
      hostId: ALICE_UID,
      viewerId: BOB_UID,
      status: "requesting",
      offerSdp: "v=0...",
      viewerCandidates: [],
      hostCandidates: [],
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
  await assertFails(
    charlieDb.collection("streams").doc("stream_01").collection("viewer_signals").doc(BOB_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_01").collection("viewer_signals").doc(BOB_UID).update({
      answerSdp: "v=0 answer...",
      status: "connected",
      updatedAt: pastDate(1000),
    })
  );

  // Host ends stream -> terminal state lock blocks further updates
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_01").update({
      status: "ended",
      viewerCount: 0,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("streams").doc("stream_01").update({
      status: "live",
      updatedAt: pastDate(200),
    })
  );
});

test("Friendships: send, accept, decline, query isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const friendshipId = `${ALICE_UID}_${BOB_UID}`;
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("friendships").doc(friendshipId).set({
      requesterId: ALICE_UID,
      recipientId: BOB_UID,
      requesterName: "NovaStriker",
      requesterAvatarData: "preset:cyber_wolf",
      recipientName: "ApexPhantom",
      recipientAvatarData: "preset:neon_fox",
      memberUids: [ALICE_UID, BOB_UID],
      status: "pending",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice cannot self-accept her own request
  await assertFails(
    aliceDb.collection("friendships").doc(friendshipId).update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );

  // Charlie cannot read or accept Alice-Bob friendship
  await assertFails(charlieDb.collection("friendships").doc(friendshipId).get());
  await assertFails(charlieDb.collection("friendships").get());

  // Bob can query and accept
  await assertSucceeds(
    bobDb.collection("friendships").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("friendships").doc(friendshipId).update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );
});

test("VideoCalls: caller rings, callee answers, ICE exchange, terminal lock on ended", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("video_calls").doc("call_01").set({
      callerId: ALICE_UID,
      calleeId: BOB_UID,
      callerName: "NovaStriker",
      calleeName: "ApexPhantom",
      participantUids: [ALICE_UID, BOB_UID],
      gameContext: "Valorant",
      status: "ringing",
      offerSdp: "v=0 offer...",
      callerCandidates: [],
      calleeCandidates: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Charlie cannot read or list
  await assertFails(charlieDb.collection("video_calls").doc("call_01").get());
  await assertFails(charlieDb.collection("video_calls").get());

  // Bob lists his calls and answers
  await assertSucceeds(
    bobDb.collection("video_calls").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("video_calls").doc("call_01").update({
      status: "active",
      answerSdp: "v=0 answer...",
      calleeCandidates: ["candidate:1 1 UDP 2122252543 192.0.2.1 54400 typ host"],
      updatedAt: pastDate(1000),
    })
  );

  // Alice ends call -> terminal state locked
  await assertSucceeds(
    aliceDb.collection("video_calls").doc("call_01").update({
      status: "ended",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("video_calls").doc("call_01").update({
      status: "active",
      updatedAt: pastDate(200),
    })
  );
});

test("Leaderboard & MatchRecords: owner match logging, anti-cheat delta enforcement, global listing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Alice cannot create leaderboard with inflated 99 wins
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "NovaStriker",
      wins: 99,
      losses: 0,
      rating: 3000,
      currentStreak: 99,
      bestStreak: 99,
      listed: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid initial leaderboard entry with 1 win
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "NovaStriker",
      avatarData: "preset:cyber_wolf",
      favoriteGame: "Valorant",
      wins: 1,
      losses: 0,
      rating: 1025,
      currentStreak: 1,
      bestStreak: 1,
      listed: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob can query global listed leaderboard, cannot run unfiltered list, cannot edit Alice's stats
  await assertSucceeds(bobDb.collection("leaderboard").where("listed", "==", true).get());
  await assertFails(bobDb.collection("leaderboard").get());
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      losses: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Alice can increment losses by +1, cannot jump wins by +10
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      losses: 1,
      rating: 1010,
      currentStreak: 0,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      wins: 11,
      rating: 1275,
      updatedAt: pastDate(500),
    })
  );

  // Match records: Alice creates detailed match record (with scores & virtual Arena Points) and draw record; Bob cannot read/list/delete
  await assertSucceeds(
    aliceDb.collection("match_records").doc("match_01").set({
      userId: ALICE_UID,
      username: "NovaStriker",
      gameId: "nba_2k27",
      gameTitle: "NBA 2K27",
      outcome: "win",
      opponentName: "ApexPhantom",
      scoreSummary: "88 - 81",
      myScore: 88,
      opponentScore: 81,
      matchMode: "Pro-AM 5v5",
      wagerPoints: 500,
      payoutPoints: 950,
      notes: "Locked down perimeter in Q4.",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("match_records").doc("match_02").set({
      userId: ALICE_UID,
      username: "NovaStriker",
      gameId: "ea_fc_27",
      gameTitle: "EA Sports FC 27",
      outcome: "draw",
      opponentName: "VoidKeeper",
      scoreSummary: "2 - 2",
      myScore: 2,
      opponentScore: 2,
      wagerPoints: 200,
      payoutPoints: 200,
      createdAt: pastDate(800),
    })
  );
  await assertFails(
    aliceDb.collection("match_records").doc("match_bad").set({
      userId: ALICE_UID,
      username: "NovaStriker",
      gameId: "nba_2k27",
      gameTitle: "NBA 2K27",
      outcome: "win",
      wagerPoints: -50,
      createdAt: pastDate(600),
    })
  );
  await assertSucceeds(
    aliceDb.collection("match_records").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("match_records").doc("match_01").get());
  await assertFails(bobDb.collection("match_records").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("match_records").doc("match_01").delete());
  await assertSucceeds(aliceDb.collection("match_records").doc("match_02").delete());
});

test("SportHighlights: create, featured listing, hype delta, owner-only edit/delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("sport_highlights").doc("hl_2k27_01").set({
      authorId: ALICE_UID,
      authorUsername: "NovaStriker",
      authorAvatarData: "preset:cyber_wolf",
      gameId: "nba_2k27",
      gameTitle: "NBA 2K27",
      sportLeague: "NBA",
      title: "Logo 3PT Green Release Buzzer Beater!",
      playType: "Buzzer Beater",
      statLine: "42 PTS · 11 AST · 8 3PM",
      quarterOrTime: "Q4 · 0:00.3",
      description: "Rec 5v5 game-winner from halfcourt.",
      hypeCount: 0,
      hypedByUids: [],
      featured: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Featured query succeeds; unfiltered list fails
  await assertSucceeds(bobDb.collection("sport_highlights").where("featured", "==", true).get());
  await assertFails(bobDb.collection("sport_highlights").get());

  // Bob can toggle hype by +1, cannot edit title or jump hype by +50, cannot delete
  await assertSucceeds(
    bobDb.collection("sport_highlights").doc("hl_2k27_01").update({
      hypeCount: 1,
      hypedByUids: [BOB_UID],
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("sport_highlights").doc("hl_2k27_01").update({
      title: "Hijacked Highlight",
      updatedAt: pastDate(900),
    })
  );
  await assertFails(
    bobDb.collection("sport_highlights").doc("hl_2k27_01").update({
      hypeCount: 51,
      hypedByUids: [BOB_UID],
      updatedAt: pastDate(800),
    })
  );
  await assertFails(bobDb.collection("sport_highlights").doc("hl_2k27_01").delete());

  // Alice can delete her own highlight
  await assertSucceeds(aliceDb.collection("sport_highlights").doc("hl_2k27_01").delete());
});

test("VoiceCommentary: participants roster, host moderation mute, and isolated pair audio WebRTC signaling", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_voice_01").set({
      hostId: ALICE_UID,
      hostUsername: "NovaStriker",
      title: "NBA 2K27 Playbook Breakdown",
      titleLower: "nba 2k27 playbook breakdown",
      gameId: "nba_2k27",
      gameTitle: "NBA 2K27",
      gameTitleLower: "nba 2k27",
      categoryGenre: "Sports",
      region: "Global",
      status: "live",
      viewerCount: 2,
      micEnabled: true,
      cameraEnabled: true,
      voiceChannelEnabled: true,
      voiceTopic: "Tactics & Feedback",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice (host) and Bob (viewer) join audio-only voice commentary
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(ALICE_UID).set({
      streamId: "stream_voice_01",
      hostId: ALICE_UID,
      userId: ALICE_UID,
      username: "NovaStriker",
      avatarData: "preset:cyber_wolf",
      role: "host",
      focusTag: "Co-Cast",
      muted: false,
      hostMuted: false,
      speakingStatus: "active",
      joinedAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(BOB_UID).set({
      streamId: "stream_voice_01",
      hostId: ALICE_UID,
      userId: BOB_UID,
      username: "ApexPhantom",
      avatarData: "preset:neon_fox",
      role: "commentator",
      focusTag: "Tactics",
      muted: false,
      hostMuted: false,
      speakingStatus: "active",
      joinedAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Roster list requires streamId filter; unfiltered list fails
  await assertSucceeds(
    charlieDb.collection("streams").doc("stream_voice_01").collection("voice_participants")
      .where("streamId", "==", "stream_voice_01").get()
  );
  await assertFails(
    charlieDb.collection("streams").doc("stream_voice_01").collection("voice_participants").get()
  );

  // Bob switches his focus tag to Feedback
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(BOB_UID).update({
      focusTag: "Feedback",
      updatedAt: pastDate(2000),
    })
  );

  // Alice (host) mutes Bob; Bob cannot unmute himself while hostMuted == true
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(BOB_UID).update({
      muted: true,
      hostMuted: true,
      speakingStatus: "muted",
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(BOB_UID).update({
      muted: false,
      speakingStatus: "active",
      updatedAt: pastDate(1000),
    })
  );

  // Deterministic pair audio-only WebRTC signaling between Alice and Bob
  const signalId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_voice_01").collection("voice_signals").doc(signalId).set({
      streamId: "stream_voice_01",
      initiatorId: ALICE_UID,
      responderId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      status: "offering",
      offerSdp: "v=0 audio-only offer",
      initiatorCandidates: [],
      responderCandidates: [],
      createdAt: pastDate(1200),
      updatedAt: pastDate(1200),
    })
  );

  // Charlie cannot read or list Alice-Bob voice signaling
  await assertFails(
    charlieDb.collection("streams").doc("stream_voice_01").collection("voice_signals").doc(signalId).get()
  );
  await assertFails(
    charlieDb.collection("streams").doc("stream_voice_01").collection("voice_signals")
      .where("streamId", "==", "stream_voice_01").get()
  );

  // Bob reads his signals and sends audio-only answer + ICE candidate
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_signals")
      .where("streamId", "==", "stream_voice_01")
      .where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_signals").doc(signalId).update({
      answerSdp: "v=0 audio-only answer",
      status: "connected",
      responderCandidates: ["candidate:1 1 UDP 2122252543 192.0.2.5 49152 typ host"],
      updatedAt: pastDate(800),
    })
  );

  // Host disables voice channel -> Charlie cannot join as new commentator
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_voice_01").update({
      voiceChannelEnabled: false,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(CHARLIE_UID).set({
      streamId: "stream_voice_01",
      hostId: ALICE_UID,
      userId: CHARLIE_UID,
      username: "CharlieVoice",
      role: "commentator",
      focusTag: "Tactics",
      muted: false,
      hostMuted: false,
      speakingStatus: "active",
      joinedAt: pastDate(300),
      updatedAt: pastDate(300),
    })
  );

  // Bob leaves voice channel (delete participant + signal)
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_participants").doc(BOB_UID).delete()
  );
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_voice_01").collection("voice_signals").doc(signalId).delete()
  );
});

