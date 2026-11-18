package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.AuthManager
import com.example.data.firebase.AuthManager.authStateFlow
import com.example.data.firebase.NexusRepository
import com.example.data.model.GameCategory
import com.example.data.model.UiState
import com.example.rtc.WebRtcEngine
import com.example.ui.components.IncomingCallBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.GoLiveScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MatchHistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SportsHubScreen
import com.example.ui.screens.StreamWatchScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.AmberBusy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NexusCastTheme
import com.example.ui.viewmodel.NexusViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object NavRoutes {
    const val ROUTE_DISCOVER = "discover"
    const val ROUTE_SPORTS = "sports"
    const val ROUTE_CATEGORIES = "categories"
    const val ROUTE_GO_LIVE = "go_live"
    const val ROUTE_SEARCH = "search"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_LEADERBOARD = "leaderboard"
    const val ROUTE_MATCH_HISTORY = "match_history"
    const val ROUTE_STREAM_WATCH = "stream_watch/{streamId}"
    const val ROUTE_VIDEO_CALL = "video_call"

    fun watchStream(streamId: String): String = "stream_watch/$streamId"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(NavRoutes.ROUTE_DISCOVER, R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
    TopLevelDestination(NavRoutes.ROUTE_SPORTS, R.string.nav_sports, Icons.Filled.SportsBasketball, Icons.Outlined.SportsBasketball, "nav_sports"),
    TopLevelDestination(NavRoutes.ROUTE_GO_LIVE, R.string.nav_go_live, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_go_live"),
    TopLevelDestination(NavRoutes.ROUTE_SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    TopLevelDestination(NavRoutes.ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusCastTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedNexusApp(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedNexusApp(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NexusViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = NexusRepository(db)
                val rtc = WebRtcEngine(app)
                NexusViewModel(repo, rtc, currentUserId)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val streamsState by viewModel.liveStreamsState.collectAsStateWithLifecycle()
    val gamersState by viewModel.discoverableGamersState.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendshipsState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val matchHistoryState by viewModel.matchHistoryState.collectAsStateWithLifecycle()
    val sportHighlightsState by viewModel.sportHighlightsState.collectAsStateWithLifecycle()
    val incomingCall by viewModel.incomingCall.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val selectedGameId by viewModel.selectedGameId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchTab by viewModel.searchTab.collectAsStateWithLifecycle()
    val activeBroadcastId by viewModel.activeBroadcastId.collectAsStateWithLifecycle()
    val liveMessages by viewModel.watchingMessages.collectAsStateWithLifecycle()
    val micEnabled by viewModel.micEnabled.collectAsStateWithLifecycle()
    val cameraEnabled by viewModel.cameraEnabled.collectAsStateWithLifecycle()
    val speakerEnabled by viewModel.speakerEnabled.collectAsStateWithLifecycle()
    val voiceParticipants by viewModel.voiceParticipants.collectAsStateWithLifecycle()
    val inVoiceChannel by viewModel.inVoiceChannel.collectAsStateWithLifecycle()
    val voiceMicMuted by viewModel.voiceMicMuted.collectAsStateWithLifecycle()
    val voiceHostMutedMe by viewModel.voiceHostMutedMe.collectAsStateWithLifecycle()
    val voiceFocusTag by viewModel.voiceFocusTag.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var pendingBroadcastCategory by remember { mutableStateOf<GameCategory?>(null) }
    var pendingCategoryBrowseId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val pendingIncomingFriendCount = remember(friendshipsState, currentUserId) {
        (friendshipsState as? UiState.Success)?.data
            ?.count { it.status == "pending" && it.recipientId == currentUserId }
            ?: 0
    }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val liveStreams = (streamsState as? UiState.Success)?.data.orEmpty()
    val activeStream = liveStreams.firstOrNull { it.id == activeBroadcastId }
    val leaderboardEntries = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val myRankIndex = leaderboardEntries.indexOfFirst { it.userId == currentUserId }
    val myRankEntry = if (myRankIndex >= 0) leaderboardEntries[myRankIndex] else null

    val isTopLevelRoute = currentRoute in topLevelDestinations.map { it.route } ||
        currentRoute == NavRoutes.ROUTE_CATEGORIES

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openLeaderboard() {
        navController.navigate(NavRoutes.ROUTE_LEADERBOARD) {
            launchSingleTop = true
        }
    }

    fun openMatchHistory() {
        navController.navigate(NavRoutes.ROUTE_MATCH_HISTORY) {
            launchSingleTop = true
        }
    }

    fun launchVideoCall(userId: String, username: String, avatarData: String, gameContext: String) {
        viewModel.startVideoCall(
            targetUserId = userId,
            targetUsername = username,
            targetAvatarData = avatarData,
            gameContext = gameContext
        ) {
            navController.navigate(NavRoutes.ROUTE_VIDEO_CALL)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail && isTopLevelRoute) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == NavRoutes.ROUTE_PROFILE && pendingIncomingFriendCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = LiveCrimson) {
                                                    Text(pendingIncomingFriendCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = stringResource(dest.labelRes))
                                        }
                                    } else {
                                        Icon(icon, contentDescription = stringResource(dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (dest.route == NavRoutes.ROUTE_GO_LIVE) LiveCrimson else NeonCyan,
                                    selectedTextColor = if (dest.route == NavRoutes.ROUTE_GO_LIVE) LiveCrimson else NeonCyan,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && isTopLevelRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == NavRoutes.ROUTE_PROFILE && pendingIncomingFriendCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = LiveCrimson) {
                                                    Text(pendingIncomingFriendCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = stringResource(dest.labelRes))
                                        }
                                    } else {
                                        Icon(icon, contentDescription = stringResource(dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = if (dest.route == NavRoutes.ROUTE_GO_LIVE) LiveCrimson else NeonCyan,
                                    selectedTextColor = if (dest.route == NavRoutes.ROUTE_GO_LIVE) LiveCrimson else NeonCyan,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.ROUTE_CATEGORIES,
                            onClick = {
                                pendingCategoryBrowseId = null
                                navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.SportsEsports,
                                    contentDescription = stringResource(R.string.nav_categories)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_categories)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = ElectricViolet.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.testTag("nav_categories")
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.ROUTE_LEADERBOARD,
                            onClick = ::openLeaderboard,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = stringResource(R.string.nav_leaderboard)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_leaderboard)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AmberBusy,
                                selectedTextColor = AmberBusy,
                                indicatorColor = ElectricViolet.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.testTag("nav_leaderboard")
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.ROUTE_MATCH_HISTORY,
                            onClick = ::openMatchHistory,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = stringResource(R.string.nav_match_history)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_match_history)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = ElectricViolet.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.testTag("nav_match_history")
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isTopLevelRoute) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = ElectricViolet.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .clickable {
                                                pendingCategoryBrowseId = null
                                                navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                                            }
                                            .testTag("header_categories_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SportsEsports,
                                                contentDescription = "All Games",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = stringResource(R.string.nav_categories),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = NeonCyan
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = AmberBusy.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, AmberBusy.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .clickable(onClick = ::openLeaderboard)
                                            .testTag("header_leaderboard_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = "Open Global Leaderboard",
                                                tint = AmberBusy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (myRankIndex >= 0 && myRankEntry != null) {
                                                    "#${myRankIndex + 1} · ${myRankEntry.rating} MMR"
                                                } else {
                                                    stringResource(R.string.nav_leaderboard)
                                                },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = AmberBusy
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Real-time Incoming Gamer Video Call Banner
                    AnimatedVisibility(
                        visible = incomingCall != null && currentRoute != NavRoutes.ROUTE_VIDEO_CALL,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        incomingCall?.let { call ->
                            IncomingCallBanner(
                                call = call,
                                onAccept = {
                                    viewModel.acceptIncomingCall(call) {
                                        navController.navigate(NavRoutes.ROUTE_VIDEO_CALL)
                                    }
                                },
                                onDecline = {
                                    viewModel.declineIncomingCall(call.id)
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = NavRoutes.ROUTE_DISCOVER
                        ) {
                            composable(NavRoutes.ROUTE_DISCOVER) {
                                DiscoverScreen(
                                    myProfile = myProfile,
                                    streamsState = streamsState,
                                    friendshipsState = friendshipsState,
                                    leaderboardState = leaderboardState,
                                    selectedGameId = selectedGameId,
                                    currentUserId = currentUserId,
                                    onSelectGame = viewModel::selectGameCategory,
                                    onWatchStream = { stream ->
                                        viewModel.joinStream(stream)
                                        navController.navigate(NavRoutes.watchStream(stream.id))
                                    },
                                    onGoLiveClick = {
                                        pendingBroadcastCategory = null
                                        navigateTopLevel(NavRoutes.ROUTE_GO_LIVE)
                                    },
                                    onBrowseCategoriesClick = {
                                        pendingCategoryBrowseId = null
                                        navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                                    },
                                    onOpenSportsHub = {
                                        navigateTopLevel(NavRoutes.ROUTE_SPORTS)
                                    },
                                    onOpenCategory = { category ->
                                        pendingCategoryBrowseId = category.id
                                        navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                                    },
                                    onOpenLeaderboard = ::openLeaderboard,
                                    onOpenMatchHistory = ::openMatchHistory,
                                    onCallUser = ::launchVideoCall,
                                    onShowMessage = viewModel::showMessage
                                )
                            }

                            composable(NavRoutes.ROUTE_SPORTS) {
                                SportsHubScreen(
                                    streamsState = streamsState,
                                    highlightsState = sportHighlightsState,
                                    currentUserId = currentUserId,
                                    onWatchCategory = { category ->
                                        pendingCategoryBrowseId = category.id
                                        navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                                    },
                                    onGoLiveInCategory = { category ->
                                        pendingBroadcastCategory = category
                                        navigateTopLevel(NavRoutes.ROUTE_GO_LIVE)
                                    },
                                    onPublishHighlight = viewModel::publishSportHighlight,
                                    onToggleHype = viewModel::toggleHighlightHype,
                                    onDeleteHighlight = viewModel::deleteSportHighlight,
                                    onCallPlayer = ::launchVideoCall
                                )
                            }

                            composable(NavRoutes.ROUTE_CATEGORIES) {
                                CategoriesScreen(
                                    streamsState = streamsState,
                                    currentUserId = currentUserId,
                                    initialGameId = pendingCategoryBrowseId,
                                    onWatchStream = { stream ->
                                        viewModel.joinStream(stream)
                                        navController.navigate(NavRoutes.watchStream(stream.id))
                                    },
                                    onGoLiveInCategory = { category ->
                                        pendingBroadcastCategory = category
                                        navigateTopLevel(NavRoutes.ROUTE_GO_LIVE)
                                    },
                                    onCallUser = ::launchVideoCall
                                )
                            }

                            composable(NavRoutes.ROUTE_GO_LIVE) {
                                GoLiveScreen(
                                    webRtcEngine = viewModel.webRtcEngine,
                                    activeBroadcastId = activeBroadcastId,
                                    activeStream = activeStream,
                                    liveMessages = liveMessages,
                                    voiceParticipants = voiceParticipants,
                                    micEnabled = micEnabled,
                                    cameraEnabled = cameraEnabled,
                                    initialCategory = pendingBroadcastCategory,
                                    onStartPreview = viewModel::startBroadcastPreview,
                                    onGoLive = viewModel::goLive,
                                    onEndBroadcast = viewModel::endLiveBroadcast,
                                    onToggleMic = viewModel::toggleMic,
                                    onToggleCamera = viewModel::toggleCamera,
                                    onSwitchCamera = viewModel::switchCamera,
                                    onUpdateVoiceSettings = viewModel::updateVoiceRoomSettings,
                                    onHostMuteParticipant = viewModel::hostMuteVoiceParticipant,
                                    onHostRemoveParticipant = viewModel::hostRemoveVoiceParticipant
                                )
                            }

                            composable(NavRoutes.ROUTE_SEARCH) {
                                SearchScreen(
                                    query = searchQuery,
                                    activeTab = searchTab,
                                    streamsState = streamsState,
                                    gamersState = gamersState,
                                    friendshipsState = friendshipsState,
                                    currentUserId = currentUserId,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onTabChange = viewModel::selectSearchTab,
                                    onCategoryClick = { category ->
                                        pendingCategoryBrowseId = category.id
                                        navigateTopLevel(NavRoutes.ROUTE_CATEGORIES)
                                    },
                                    onWatchStream = { stream ->
                                        viewModel.joinStream(stream)
                                        navController.navigate(NavRoutes.watchStream(stream.id))
                                    },
                                    onSendFriendRequest = viewModel::sendFriendRequest,
                                    onAcceptFriendRequest = { id ->
                                        viewModel.respondToFriendRequest(id, accept = true)
                                    },
                                    onCallUser = ::launchVideoCall
                                )
                            }

                            composable(NavRoutes.ROUTE_PROFILE) {
                                ProfileScreen(
                                    myProfileState = myProfileState,
                                    friendshipsState = friendshipsState,
                                    leaderboardState = leaderboardState,
                                    currentUserId = currentUserId,
                                    onSaveProfile = viewModel::updateProfile,
                                    onAcceptFriend = { id -> viewModel.respondToFriendRequest(id, accept = true) },
                                    onDeclineFriend = { id -> viewModel.respondToFriendRequest(id, accept = false) },
                                    onRemoveFriend = viewModel::removeFriendship,
                                    onCallFriend = ::launchVideoCall,
                                    onOpenLeaderboard = ::openLeaderboard,
                                    onOpenMatchHistory = ::openMatchHistory,
                                    onSignOut = {
                                        AuthManager.signOut(credentialManager, scope)
                                    },
                                    onShowMessage = viewModel::showMessage
                                )
                            }

                            composable(NavRoutes.ROUTE_LEADERBOARD) {
                                LeaderboardScreen(
                                    leaderboardState = leaderboardState,
                                    matchHistoryState = matchHistoryState,
                                    friendshipsState = friendshipsState,
                                    currentUserId = currentUserId,
                                    onRecordMatch = { input ->
                                        viewModel.recordMatchResult(
                                            category = input.category,
                                            outcome = input.outcome,
                                            opponentName = input.opponentName,
                                            scoreSummary = input.scoreSummary,
                                            myScore = input.myScore,
                                            opponentScore = input.opponentScore,
                                            matchMode = input.matchMode,
                                            wagerPoints = input.wagerPoints,
                                            payoutPoints = input.payoutPoints,
                                            notes = input.notes
                                        )
                                    },
                                    onOpenMatchHistory = ::openMatchHistory,
                                    onSendFriendRequest = viewModel::sendFriendRequest,
                                    onCallPlayer = ::launchVideoCall,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(NavRoutes.ROUTE_MATCH_HISTORY) {
                                MatchHistoryScreen(
                                    matchHistoryState = matchHistoryState,
                                    friendshipsState = friendshipsState,
                                    currentUserId = currentUserId,
                                    onRecordMatch = { input ->
                                        viewModel.recordMatchResult(
                                            category = input.category,
                                            outcome = input.outcome,
                                            opponentName = input.opponentName,
                                            scoreSummary = input.scoreSummary,
                                            myScore = input.myScore,
                                            opponentScore = input.opponentScore,
                                            matchMode = input.matchMode,
                                            wagerPoints = input.wagerPoints,
                                            payoutPoints = input.payoutPoints,
                                            notes = input.notes
                                        )
                                    },
                                    onDeleteMatch = viewModel::deleteMatchRecord,
                                    onOpenLeaderboard = ::openLeaderboard,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = NavRoutes.ROUTE_STREAM_WATCH,
                                arguments = listOf(navArgument("streamId") { type = NavType.StringType })
                            ) { entry ->
                                val streamId = entry.arguments?.getString("streamId").orEmpty()
                                val watchedStream = liveStreams.firstOrNull { it.id == streamId }
                                if (watchedStream == null) {
                                    LaunchedEffect(Unit) {
                                        viewModel.leaveStream(null)
                                        navController.popBackStack()
                                    }
                                } else {
                                    val isFriend = (friendshipsState as? UiState.Success)?.data
                                        ?.any { it.status == "accepted" && it.memberUids.contains(watchedStream.hostId) }
                                        ?: false
                                    StreamWatchScreen(
                                        stream = watchedStream,
                                        messages = liveMessages,
                                        voiceParticipants = voiceParticipants,
                                        inVoiceChannel = inVoiceChannel,
                                        voiceMicMuted = voiceMicMuted,
                                        voiceHostMutedMe = voiceHostMutedMe,
                                        voiceFocusTag = voiceFocusTag,
                                        speakerEnabled = speakerEnabled,
                                        webRtcEngine = viewModel.webRtcEngine,
                                        currentUserId = currentUserId,
                                        isAlreadyFriend = isFriend,
                                        onBack = {
                                            viewModel.leaveStream(watchedStream)
                                            navController.popBackStack()
                                        },
                                        onSendMessage = { text, type ->
                                            viewModel.sendChatMessage(watchedStream.id, text, type)
                                        },
                                        onAddFriend = viewModel::sendFriendRequest,
                                        onCallHost = {
                                            viewModel.leaveStream(watchedStream)
                                            navController.popBackStack()
                                            launchVideoCall(
                                                watchedStream.hostId,
                                                watchedStream.hostUsername,
                                                watchedStream.hostAvatarData,
                                                watchedStream.gameTitle
                                            )
                                        },
                                        onJoinVoice = { withMic, focusTag ->
                                            viewModel.joinVoiceCommentary(watchedStream, withMic, focusTag)
                                        },
                                        onLeaveVoice = {
                                            viewModel.leaveVoiceCommentary(releaseMicIfViewer = true)
                                        },
                                        onToggleVoiceMic = { hasAudioPermission ->
                                            viewModel.toggleVoiceMic(hasAudioPermission)
                                        },
                                        onSelectVoiceFocus = viewModel::setVoiceFocusTag,
                                        onToggleSpeakerphone = viewModel::toggleSpeakerphone
                                    )
                                }
                            }

                            composable(NavRoutes.ROUTE_VIDEO_CALL) {
                                VideoCallScreen(
                                    call = activeCall,
                                    currentUserId = currentUserId,
                                    webRtcEngine = viewModel.webRtcEngine,
                                    micEnabled = micEnabled,
                                    cameraEnabled = cameraEnabled,
                                    speakerEnabled = speakerEnabled,
                                    onToggleMic = viewModel::toggleMic,
                                    onToggleCamera = viewModel::toggleCamera,
                                    onSwitchCamera = viewModel::switchCamera,
                                    onToggleSpeaker = viewModel::toggleSpeakerphone,
                                    onEndCall = {
                                        viewModel.endActiveCall()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
