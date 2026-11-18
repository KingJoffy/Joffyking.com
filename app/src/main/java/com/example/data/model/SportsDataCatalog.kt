package com.example.data.model

import com.example.R

object SportsDataCatalog {

    val leagues: List<String> = listOf("ALL", "NBA", "NFL", "SOCCER", "MLB", "NHL", "CFB", "UFC")

    val scoreboards: List<SportMatchupScore> = listOf(
        SportMatchupScore(
            id = "nba_bos_okc",
            league = "NBA",
            gameId = "nba_2k27",
            awayTeam = "Boston Celtics",
            awayShort = "BOS",
            awayScore = 114,
            homeTeam = "Oklahoma City Thunder",
            homeShort = "OKC",
            homeScore = 118,
            clockStatus = "Q4 • 0:42",
            liveNow = true,
            keyPerformers = "S. Gilgeous-Alexander 38 PTS • J. Tatum 34 PTS",
            homeWinProbability = 64
        ),
        SportMatchupScore(
            id = "nba_den_lal",
            league = "NBA",
            gameId = "nba_2k27",
            awayTeam = "Denver Nuggets",
            awayShort = "DEN",
            awayScore = 109,
            homeTeam = "Los Angeles Lakers",
            homeShort = "LAL",
            homeScore = 106,
            clockStatus = "Q4 • 2:18",
            liveNow = true,
            keyPerformers = "N. Jokic 31/14/11 • L. Doncic 33/9/10",
            homeWinProbability = 41
        ),
        SportMatchupScore(
            id = "nfl_kc_buf",
            league = "NFL",
            gameId = "madden_27",
            awayTeam = "Kansas City Chiefs",
            awayShort = "KC",
            awayScore = 27,
            homeTeam = "Buffalo Bills",
            homeShort = "BUF",
            homeScore = 31,
            clockStatus = "Q4 • 1:54",
            liveNow = true,
            keyPerformers = "J. Allen 342 YDS 4 TD • P. Mahomes 318 YDS 3 TD",
            homeWinProbability = 58
        ),
        SportMatchupScore(
            id = "nfl_det_phi",
            league = "NFL",
            gameId = "madden_27",
            awayTeam = "Detroit Lions",
            awayShort = "DET",
            awayScore = 28,
            homeTeam = "Philadelphia Eagles",
            homeShort = "PHI",
            homeScore = 24,
            clockStatus = "FINAL",
            liveNow = false,
            keyPerformers = "J. Gibbs 148 YDS 2 TD • S. Barkley 132 YDS 1 TD",
            homeWinProbability = 0
        ),
        SportMatchupScore(
            id = "soccer_rma_mci",
            league = "SOCCER",
            gameId = "ea_fc_27",
            awayTeam = "Real Madrid",
            awayShort = "RMA",
            awayScore = 3,
            homeTeam = "Manchester City",
            homeShort = "MCI",
            homeScore = 2,
            clockStatus = "84'",
            liveNow = true,
            keyPerformers = "K. Mbappe 2G • Vini Jr 1G 1A • E. Haaland 2G",
            homeWinProbability = 29
        ),
        SportMatchupScore(
            id = "cfb_osu_tex",
            league = "CFB",
            gameId = "college_football_27",
            awayTeam = "Ohio State",
            awayShort = "OSU",
            awayScore = 35,
            homeTeam = "Texas Longhorns",
            homeShort = "TEX",
            homeScore = 31,
            clockStatus = "Q4 • 3:05",
            liveNow = true,
            keyPerformers = "J. Smith 162 REC YDS 2 TD • A. Manning 298 YDS 3 TD",
            homeWinProbability = 37
        ),
        SportMatchupScore(
            id = "mlb_lad_nyy",
            league = "MLB",
            gameId = "mlb_the_show_27",
            awayTeam = "LA Dodgers",
            awayShort = "LAD",
            awayScore = 6,
            homeTeam = "NY Yankees",
            homeShort = "NYY",
            homeScore = 5,
            clockStatus = "BOT 8th",
            liveNow = true,
            keyPerformers = "S. Ohtani 2-4 HR 3 RBI • A. Judge 2-3 HR 2 RBI",
            homeWinProbability = 44
        ),
        SportMatchupScore(
            id = "nhl_edm_fla",
            league = "NHL",
            gameId = "nhl_27",
            awayTeam = "Edmonton Oilers",
            awayShort = "EDM",
            awayScore = 4,
            homeTeam = "Florida Panthers",
            homeShort = "FLA",
            homeScore = 3,
            clockStatus = "3RD • 4:11",
            liveNow = true,
            keyPerformers = "C. McDavid 1G 2A • M. Tkachuk 2G",
            homeWinProbability = 35
        )
    )

    val standings: List<SportStandingRow> = listOf(
        // NBA 2K27 / NBA Standings
        SportStandingRow(1, "Oklahoma City Thunder", "West #1", 58, 14, ".806 PCT", "+11.4 NET", "W6", "NBA"),
        SportStandingRow(2, "Boston Celtics", "East #1", 56, 16, ".778 PCT", "+10.2 NET", "W4", "NBA"),
        SportStandingRow(3, "Cleveland Cavaliers", "East #2", 54, 18, ".750 PCT", "+8.7 NET", "W3", "NBA"),
        SportStandingRow(4, "Denver Nuggets", "West #2", 50, 22, ".694 PCT", "+6.5 NET", "W2", "NBA"),
        SportStandingRow(5, "Los Angeles Lakers", "West #3", 48, 24, ".667 PCT", "+4.9 NET", "L1", "NBA"),
        SportStandingRow(6, "New York Knicks", "East #3", 47, 25, ".653 PCT", "+5.1 NET", "W2", "NBA"),

        // NFL / Madden 27 Standings
        SportStandingRow(1, "Buffalo Bills", "AFC East", 13, 3, "468 PF", "+138 DIFF", "W5", "NFL"),
        SportStandingRow(2, "Kansas City Chiefs", "AFC West", 13, 3, "432 PF", "+104 DIFF", "W3", "NFL"),
        SportStandingRow(3, "Detroit Lions", "NFC North", 14, 2, "502 PF", "+164 DIFF", "W7", "NFL"),
        SportStandingRow(4, "Philadelphia Eagles", "NFC East", 12, 4, "445 PF", "+119 DIFF", "L1", "NFL"),
        SportStandingRow(5, "Baltimore Ravens", "AFC North", 12, 4, "461 PF", "+126 DIFF", "W4", "NFL"),
        SportStandingRow(6, "San Francisco 49ers", "NFC West", 11, 5, "418 PF", "+88 DIFF", "W2", "NFL"),

        // Soccer / EA Sports FC 27
        SportStandingRow(1, "Real Madrid", "UCL Top", 19, 2, "5D • 62 PTS", "+38 GD", "W4", "SOCCER"),
        SportStandingRow(2, "Liverpool", "Premier", 20, 2, "4D • 64 PTS", "+41 GD", "W6", "SOCCER"),
        SportStandingRow(3, "FC Barcelona", "LaLiga", 18, 3, "4D • 58 PTS", "+39 GD", "W3", "SOCCER"),
        SportStandingRow(4, "Manchester City", "Premier", 17, 4, "5D • 56 PTS", "+31 GD", "L1", "SOCCER"),

        // MLB 27
        SportStandingRow(1, "Los Angeles Dodgers", "NL West", 98, 64, ".605 PCT", "+182 RD", "W4", "MLB"),
        SportStandingRow(2, "New York Yankees", "AL East", 95, 67, ".586 PCT", "+149 RD", "W2", "MLB"),
        SportStandingRow(3, "Atlanta Braves", "NL East", 92, 70, ".568 PCT", "+118 RD", "L1", "MLB"),

        // NHL 27
        SportStandingRow(1, "Florida Panthers", "Atlantic", 49, 20, "104 PTS", "+54 DIFF", "W3", "NHL"),
        SportStandingRow(2, "Edmonton Oilers", "Pacific", 48, 21, "102 PTS", "+61 DIFF", "W5", "NHL"),
        SportStandingRow(3, "New York Rangers", "Metro", 46, 23, "98 PTS", "+44 DIFF", "W1", "NHL"),

        // College Football 27
        SportStandingRow(1, "Ohio State Buckeyes", "Big Ten", 12, 1, "CFP #1", "+312 DIFF", "W6", "CFB"),
        SportStandingRow(2, "Texas Longhorns", "SEC", 11, 2, "CFP #2", "+264 DIFF", "L1", "CFB"),
        SportStandingRow(3, "Oregon Ducks", "Big Ten", 12, 1, "CFP #3", "+278 DIFF", "W8", "CFB"),

        // UFC 6
        SportStandingRow(1, "Islam Makhachev", "P4P #1", 27, 1, "15 Finishes", "Lightweight Champ", "W15", "UFC"),
        SportStandingRow(2, "Ilia Topuria", "P4P #2", 17, 0, "14 Finishes", "Featherweight Champ", "W17", "UFC"),
        SportStandingRow(3, "Alex Pereira", "P4P #3", 13, 2, "11 KOs", "Light Heavy Champ", "W6", "UFC")
    )

    val statLeaders: List<SportStatLeader> = listOf(
        // NBA 2K27 Leaders
        SportStatLeader(
            playerName = "Shai Gilgeous-Alexander",
            team = "OKC",
            position = "PG",
            league = "NBA",
            gameId = "nba_2k27",
            categoryLabel = "NBA 2K27 Scoring & Guard Meta",
            primaryValue = "32.4 PPG",
            breakdown = "6.4 APG • 5.5 RPG • 53.8% FG • 2.1 SPG",
            ratingBadge = "98 OVR"
        ),
        SportStatLeader(
            playerName = "Nikola Jokic",
            team = "DEN",
            position = "C",
            league = "NBA",
            gameId = "nba_2k27",
            categoryLabel = "Triple-Double Engine",
            primaryValue = "29.1 / 12.8 / 10.4",
            breakdown = "64.2% TS • 31.8 PER • HOF Dimer & Post Playmaker",
            ratingBadge = "99 OVR"
        ),
        SportStatLeader(
            playerName = "Victor Wembanyama",
            team = "SAS",
            position = "C/PF",
            league = "NBA",
            gameId = "nba_2k27",
            categoryLabel = "Rim Protection & Stretch",
            primaryValue = "3.9 BPG",
            breakdown = "25.4 PPG • 11.2 RPG • 36.4% 3PT • HOF Paint Patroller",
            ratingBadge = "96 OVR"
        ),
        SportStatLeader(
            playerName = "Luka Doncic",
            team = "LAL",
            position = "PG",
            league = "NBA",
            gameId = "nba_2k27",
            categoryLabel = "Shot Creation & Deep 3s",
            primaryValue = "31.7 PPG",
            breakdown = "9.5 APG • 8.9 RPG • 4.2 3PM/G • HOF Limitless",
            ratingBadge = "98 OVR"
        ),

        // Madden NFL 27 Leaders
        SportStatLeader(
            playerName = "Josh Allen",
            team = "BUF",
            position = "QB",
            league = "NFL",
            gameId = "madden_27",
            categoryLabel = "Madden 27 Total QB Mastery",
            primaryValue = "4,610 YDS • 41 TD",
            breakdown = "620 RUSH YDS • 114.8 RTG • Gunslinger + Escape Artist",
            ratingBadge = "99 OVR"
        ),
        SportStatLeader(
            playerName = "Patrick Mahomes",
            team = "KC",
            position = "QB",
            league = "NFL",
            gameId = "madden_27",
            categoryLabel = "Clutch Passing & Improviser",
            primaryValue = "4,485 YDS • 38 TD",
            breakdown = "68.9% CMP • 9 INT • Dashing Deadeye + Fearless",
            ratingBadge = "99 OVR"
        ),
        SportStatLeader(
            playerName = "Saquon Barkley",
            team = "PHI",
            position = "HB",
            league = "NFL",
            gameId = "madden_27",
            categoryLabel = "Rushing Yards & Explosives",
            primaryValue = "1,845 RUSH YDS",
            breakdown = "5.8 YPC • 16 TD • Juke Box + Leap Frog X-Factor",
            ratingBadge = "98 OVR"
        ),
        SportStatLeader(
            playerName = "Justin Jefferson",
            team = "MIN",
            position = "WR",
            league = "NFL",
            gameId = "madden_27",
            categoryLabel = "Route Running & Deep Threat",
            primaryValue = "1,720 REC YDS",
            breakdown = "112 REC • 14 TD • Double Me X-Factor",
            ratingBadge = "99 OVR"
        ),

        // Soccer / MLB / NHL / CFB / UFC
        SportStatLeader(
            playerName = "Kylian Mbappe",
            team = "RMA",
            position = "ST",
            league = "SOCCER",
            gameId = "ea_fc_27",
            categoryLabel = "FC 27 Golden Boot Pace",
            primaryValue = "34 G + 11 A",
            breakdown = "97 PAC • 92 DRI • Quick Step+ PlayStyle",
            ratingBadge = "92 OVR"
        ),
        SportStatLeader(
            playerName = "Shohei Ohtani",
            team = "LAD",
            position = "DH/SP",
            league = "MLB",
            gameId = "mlb_the_show_27",
            categoryLabel = "Two-Way MVP Telemetry",
            primaryValue = "54 HR • 1.045 OPS",
            breakdown = "52 SB • 118.4 MPH Max Exit Velo",
            ratingBadge = "99 OVR"
        ),
        SportStatLeader(
            playerName = "Connor McDavid",
            team = "EDM",
            position = "C",
            league = "NHL",
            gameId = "nhl_27",
            categoryLabel = "NHL 27 Points Leader",
            primaryValue = "134 PTS",
            breakdown = "44 G • 90 A • Wheels + Elite Edges X-Factor",
            ratingBadge = "98 OVR"
        ),
        SportStatLeader(
            playerName = "Jeremiah Smith",
            team = "OSU",
            position = "WR",
            league = "CFB",
            gameId = "college_football_27",
            categoryLabel = "CFB 27 Explosive Receiver",
            primaryValue = "1,490 YDS • 17 TD",
            breakdown = "19.8 YPR • 97 SPD • Takeoff Ability",
            ratingBadge = "97 OVR"
        ),
        SportStatLeader(
            playerName = "Alex Pereira",
            team = "LHW",
            position = "STRIKER",
            league = "UFC",
            gameId = "ufc_6",
            categoryLabel = "UFC 6 Knockout Power",
            primaryValue = "84% KO RATE",
            breakdown = "Left Hook 99 PWR • 6.2 Sig Strikes/Min",
            ratingBadge = "96 OVR"
        )
    )

    val officialSpotlights: List<ProHighlightSpotlight> = listOf(
        ProHighlightSpotlight(
            id = "spot_2k27_gamewinner",
            gameId = "nba_2k27",
            gameTitle = "NBA 2K27",
            league = "NBA",
            title = "Double-Stepback Green Light from 34 Feet at the Buzzer!",
            playType = "Buzzer Beater",
            playerAndTeam = "S. Gilgeous-Alexander • OKC vs BOS",
            statLine = "38 PTS · 9 AST · 62% FG · +18 Clutch Impact",
            quarterClock = "Q4 • 0:00.2",
            coverRes = R.drawable.img_nba_2k27,
            playByPlaySteps = listOf(
                "0:04.8 — OKC calls timeout trailing 114-115; side-out play drawn for iso perimeter.",
                "0:02.6 — Crosses over two defenders at the hash, creates 4.2 ft of separation.",
                "0:00.9 — Launches deep stepback 34-ft jumper over contest — 99% Green Window release.",
                "0:00.0 — SWISH! OKC wins 118-114 as crowd & MyCOURT telemetry erupt."
            ),
            peakTelemetry = "Release: 512ms • Arc: 48.2° • Win Prob Swing: +74%"
        ),
        ProHighlightSpotlight(
            id = "spot_madden27_hailmary",
            gameId = "madden_27",
            gameTitle = "Madden NFL 27",
            league = "NFL",
            title = "64-Yard Sideline Toe-Tap Laser Under Blitz Pressure!",
            playType = "Clutch TD",
            playerAndTeam = "J. Allen to K. Coleman • BUF vs KC",
            statLine = "342 YDS · 4 TD · 129.6 QBR · 61 Air Yards",
            quarterClock = "Q4 • 0:11",
            coverRes = R.drawable.img_madden_27,
            playByPlaySteps = listOf(
                "0:19 — 4th & 10 on BUF 36; Zero Blitz incoming with 2 free rushers.",
                "0:16 — Allen rolls right, breaks sack tackle, sets feet at the 29-yard line.",
                "0:13 — Fires 61-air-yard back-shoulder rocket into double coverage.",
                "0:11 — Receiver high-points the ball and drags both toes in the back corner — TOUCHDOWN!"
            ),
            peakTelemetry = "Ball Velocity: 62.4 MPH • Air Distance: 61.3 YDS • Catch Prob: 19%"
        ),
        ProHighlightSpotlight(
            id = "spot_2k27_poster",
            gameId = "nba_2k27",
            gameTitle = "NBA 2K27",
            league = "NBA",
            title = "Putback Contact Poster Over Two Seven-Footers!",
            playType = "Poster Dunk",
            playerAndTeam = "A. Edwards • MIN vs DEN",
            statLine = "41 PTS · 7 REB · 44-Inch Vert Peak",
            quarterClock = "Q3 • 3:44",
            coverRes = R.drawable.img_nba_2k27,
            playByPlaySteps = listOf(
                "3:47 — Corner three rattles off back iron; Edwards sprints cleanly from weak-side wing.",
                "3:45 — Launches from dotted line at 44.1-inch peak vertical.",
                "3:44 — One-hand tomahawk slam triggers HOF Posterizer animation & And-One foul!"
            ),
            peakTelemetry = "Peak Vert: 44.1 in • Rim Force: 118 G • Hype Surge: +95"
        ),
        ProHighlightSpotlight(
            id = "spot_madden27_pick6",
            gameId = "madden_27",
            gameTitle = "Madden NFL 27",
            league = "NFL",
            title = "98-Yard Lurker User Pick-Six Seals Division Crown!",
            playType = "Pick-Six",
            playerAndTeam = "S. Gardner • NYJ Defense",
            statLine = "98-YD INT TD · 22.1 MPH Top Speed",
            quarterClock = "Q4 • 1:02",
            coverRes = R.drawable.img_madden_27,
            playByPlaySteps = listOf(
                "1:08 — Offense lines up in Trips Tight End on the 3-yard line.",
                "1:05 — User defender baits seam route, then breaks instantly on corner post.",
                "1:02 — One-handed lurk pick at the 2-yard line, sprints 98 yards untouched!"
            ),
            peakTelemetry = "Closing Speed: 22.1 MPH • Reaction: 0.18s • Return: 98 YDS"
        ),
        ProHighlightSpotlight(
            id = "spot_fc27_bicycle",
            gameId = "ea_fc_27",
            gameTitle = "EA Sports FC 27",
            league = "SOCCER",
            title = "90th-Minute Bicycle Kick Top-Bins Winner!",
            playType = "Top Bins",
            playerAndTeam = "K. Mbappe • Real Madrid vs Man City",
            statLine = "2 Goals · 1.84 xG · 108 km/h Strike",
            quarterClock = "90+2'",
            coverRes = R.drawable.img_hero_arena,
            playByPlaySteps = listOf(
                "90+1' — Whipped trivela cross sails into the penalty spot.",
                "90+2' — Acrobatic overhead strike rockets into the top-left corner!"
            ),
            peakTelemetry = "Shot Speed: 108 km/h • Post-Shot xG: 0.94"
        )
    )
}
