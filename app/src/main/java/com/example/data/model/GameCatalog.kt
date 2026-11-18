package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.R

object GameCatalog {
    val categories: List<GameCategory> = listOf(
        GameCategory(
            id = "nba_2k27",
            title = "NBA 2K27",
            genre = "Basketball",
            coverRes = R.drawable.img_nba_2k27,
            accentHex = 0xFFFF6B00,
            tags = listOf("MyCAREER", "Pro-AM", "Rec 5v5", "MyTEAM"),
            isSports = true
        ),
        GameCategory(
            id = "madden_27",
            title = "Madden NFL 27",
            genre = "Football",
            coverRes = R.drawable.img_madden_27,
            accentHex = 0xFF10B981,
            tags = listOf("MUT 27", "Franchise", "H2H Ranked", "Superstar"),
            isSports = true
        ),
        GameCategory(
            id = "ea_fc_27",
            title = "EA Sports FC 27",
            genre = "Soccer",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFF00E5FF,
            tags = listOf("Ultimate Team", "Clubs", "WL"),
            isSports = true
        ),
        GameCategory(
            id = "college_football_27",
            title = "College Football 27",
            genre = "Football",
            coverRes = R.drawable.img_madden_27,
            accentHex = 0xFFF59E0B,
            tags = listOf("Dynasty", "Road to CFP", "Heisman"),
            isSports = true
        ),
        GameCategory(
            id = "mlb_the_show_27",
            title = "MLB The Show 27",
            genre = "Baseball",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFFFF2E63,
            tags = listOf("Diamond Dynasty", "Ranked", "Co-Op"),
            isSports = true
        ),
        GameCategory(
            id = "nhl_27",
            title = "NHL 27",
            genre = "Hockey",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFF38BDF8,
            tags = listOf("HUT", "World of Chel", "6v6"),
            isSports = true
        ),
        GameCategory(
            id = "ufc_6",
            title = "EA Sports UFC 6",
            genre = "Combat Sports",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFFA855F7,
            tags = listOf("Ranked", "Career", "KO"),
            isSports = true
        ),
        GameCategory(
            id = "rocket_league",
            title = "Rocket League",
            genre = "Sports Arena",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFF00E5FF,
            tags = listOf("3v3", "Freestyle", "Tourney"),
            isSports = true
        ),
        GameCategory(
            id = "valorant",
            title = "Valorant",
            genre = "Tactical FPS",
            coverRes = R.drawable.img_cat_fps,
            accentHex = 0xFFFF2E63,
            tags = listOf("5v5", "Ranked", "Esports")
        ),
        GameCategory(
            id = "apex_legends",
            title = "Apex Legends",
            genre = "Battle Royale",
            coverRes = R.drawable.img_cat_fps,
            accentHex = 0xFF00E5FF,
            tags = listOf("Squads", "Movement", "FPS")
        ),
        GameCategory(
            id = "elden_ring",
            title = "Elden Ring",
            genre = "Action RPG",
            coverRes = R.drawable.img_cat_rpg,
            accentHex = 0xFF8B5CF6,
            tags = listOf("Boss Rush", "Co-Op", "Souls")
        ),
        GameCategory(
            id = "minecraft",
            title = "Minecraft",
            genre = "Sandbox",
            coverRes = R.drawable.img_cat_sandbox,
            accentHex = 0xFF10B981,
            tags = listOf("SMP", "Speedrun", "Creative")
        ),
        GameCategory(
            id = "league_of_legends",
            title = "League of Legends",
            genre = "MOBA",
            coverRes = R.drawable.img_cat_rpg,
            accentHex = 0xFF38BDF8,
            tags = listOf("SoloQ", "Clash", "Pro Play")
        ),
        GameCategory(
            id = "counter_strike_2",
            title = "Counter-Strike 2",
            genre = "Tactical FPS",
            coverRes = R.drawable.img_cat_fps,
            accentHex = 0xFFF59E0B,
            tags = listOf("Premier", "FACEIT", "Clutch")
        ),
        GameCategory(
            id = "fortnite",
            title = "Fortnite",
            genre = "Battle Royale",
            coverRes = R.drawable.img_cat_sandbox,
            accentHex = 0xFFEC4899,
            tags = listOf("Zero Build", "Arena", "Creative")
        ),
        GameCategory(
            id = "overwatch_2",
            title = "Overwatch 2",
            genre = "Hero Shooter",
            coverRes = R.drawable.img_cat_fps,
            accentHex = 0xFFFF7A00,
            tags = listOf("Competitive", "6v6", "Squad")
        ),
        GameCategory(
            id = "irl_just_chatting",
            title = "IRL & Squad Lounge",
            genre = "IRL",
            coverRes = R.drawable.img_hero_arena,
            accentHex = 0xFFA78BFA,
            tags = listOf("Facecam", "Q&A", "Podcast")
        )
    )

    val sportsCategories: List<GameCategory>
        get() = categories.filter { it.isSports }

    val regions: List<String> = listOf(
        "Global",
        "NA-West",
        "NA-East",
        "EU-Central",
        "APAC",
        "LATAM"
    )

    data class AvatarPreset(
        val key: String,
        val label: String,
        val primaryColor: Color,
        val secondaryColor: Color,
        val monogram: String
    )

    val avatarPresets: List<AvatarPreset> = listOf(
        AvatarPreset("preset:cyber_wolf", "Cyber Wolf", Color(0xFF8B5CF6), Color(0xFF00E5FF), "CW"),
        AvatarPreset("preset:neon_fox", "Neon Fox", Color(0xFFFF2E63), Color(0xFFF59E0B), "NF"),
        AvatarPreset("preset:void_knight", "Void Knight", Color(0xFF6366F1), Color(0xFFEC4899), "VK"),
        AvatarPreset("preset:pixel_mech", "Pixel Mech", Color(0xFF10B981), Color(0xFF00E5FF), "PM"),
        AvatarPreset("preset:hyper_nova", "Hyper Nova", Color(0xFFF59E0B), Color(0xFFFF2E63), "HN"),
        AvatarPreset("preset:shadow_ace", "Shadow Ace", Color(0xFF0EA5E9), Color(0xFF8B5CF6), "SA")
    )

    val webFoodPerks: List<FoodPerkOffer> = listOf(
        FoodPerkOffer(
            id = "perk_squad_fuel",
            title = "25% Off Late-Night Squad Fuel",
            partnerLabel = "NexusCast Web Portal Exclusive",
            discountBadge = "25% OFF",
            promoCode = "NEXUSGG25",
            description = "Unlock 25% off gamer meal combos & energy packs when you check out through the official NexusCast website.",
            websiteUrl = "https://ais-pre-omvrfcsbzelrnzcvraotyy-653425180842.us-east1.run.app/?perk=NEXUSGG25"
        ),
        FoodPerkOffer(
            id = "perk_streamer_drop",
            title = "$10 Free Delivery on Stream Nights",
            partnerLabel = "NexusCast Creator Perks",
            discountBadge = "$10 OFF",
            promoCode = "CASTNIGHT10",
            description = "Claim $10 off food delivery orders over $25 via our web rewards hub while hosting or watching live streams.",
            websiteUrl = "https://ais-pre-omvrfcsbzelrnzcvraotyy-653425180842.us-east1.run.app/?perk=CASTNIGHT10"
        ),
        FoodPerkOffer(
            id = "perk_duo_snack",
            title = "2-for-1 Gamer Fuel & Snacks",
            partnerLabel = "NexusCast Web Checkout",
            discountBadge = "BOGO",
            promoCode = "DUOQUEUE2X",
            description = "Redeem buy-one-get-one snacks & hydration drops for your squad through the NexusCast website.",
            websiteUrl = "https://ais-pre-omvrfcsbzelrnzcvraotyy-653425180842.us-east1.run.app/?perk=DUOQUEUE2X"
        )
    )

    fun findById(gameId: String): GameCategory {
        return categories.firstOrNull { it.id == gameId } ?: categories.first()
    }
}
