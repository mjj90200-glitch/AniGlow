package com.aniglow.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aniglow.app.ui.agent.AgentRoute
import com.aniglow.app.ui.agent.ChatRoute
import com.aniglow.app.ui.anime.AnimeRoute
import com.aniglow.app.ui.anime.AnimeDetailRoute
import com.aniglow.app.ui.community.CommunityRoute
import com.aniglow.app.ui.community.PostDetailRoute
import com.aniglow.app.ui.community.CreatePostRoute
import com.aniglow.app.ui.home.HomeRoute
import com.aniglow.app.ui.profile.ProfileScreen
import com.aniglow.app.ui.auth.AuthViewModel
import com.aniglow.app.ui.auth.LoginScreen
import com.aniglow.app.ui.theme.WarmBackground
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.ranking.RankingRoute
import com.aniglow.app.ui.profile.MyPostsRoute
import com.aniglow.app.ui.profile.FavoritesScreen
import com.aniglow.app.ui.profile.EditProfileRoute
import com.aniglow.app.ui.daily.DailyScreen
import com.aniglow.app.ui.common.AppBrandBar
import com.aniglow.app.data.daily.DailyStore
import kotlinx.coroutines.delay

private data class MainTab(val label: String, val icon: ImageVector)

private val mainTabs = listOf(
    MainTab("首页", Icons.Outlined.Home),
    MainTab("番剧", Icons.Outlined.LiveTv),
    MainTab("排行", Icons.Outlined.EmojiEvents),
    MainTab("每日", Icons.Outlined.CalendarMonth),
    MainTab("AIGC", Icons.Outlined.AutoAwesome),
    MainTab("社区", Icons.Outlined.Groups),
)

@Composable
fun AniGlowApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchFocusRequest by rememberSaveable { mutableIntStateOf(0) }
    var animeDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var postDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showingLogin by rememberSaveable { mutableStateOf(false) }
    var chatRole by rememberSaveable { mutableStateOf<String?>(null) }
    var chatName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingChatRole by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingChatName by rememberSaveable { mutableStateOf<String?>(null) }
    var showingCreatePost by rememberSaveable { mutableStateOf(false) }
    var pendingCreatePost by rememberSaveable { mutableStateOf(false) }
    var showingRanking by rememberSaveable { mutableStateOf(false) }
    var showingMyPosts by rememberSaveable { mutableStateOf(false) }
    var pendingMyPosts by rememberSaveable { mutableStateOf(false) }
    var showingFavorites by rememberSaveable { mutableStateOf(false) }
    var showingEditProfile by rememberSaveable { mutableStateOf(false) }
    var showingProfile by rememberSaveable { mutableStateOf(false) }
    val showingDetail = animeDetailId != null || postDetailId != null || showingLogin || chatRole != null || showingCreatePost || showingRanking || showingMyPosts || showingFavorites || showingEditProfile || showingProfile

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            while (true) {
                delay(1_000)
                DailyStore.tickBrowseSecond()
            }
        }
    }

    fun finishAuthentication() {
        showingLogin = false
        pendingChatRole?.let {
            chatRole = it
            chatName = pendingChatName ?: it
        }
        pendingChatRole = null
        pendingChatName = null
        if (pendingCreatePost) showingCreatePost = true
        pendingCreatePost = false
        if (pendingMyPosts) showingMyPosts = true
        pendingMyPosts = false
    }

    BackHandler(enabled = showingDetail) {
        when {
            showingLogin -> {
                showingLogin = false
                pendingChatRole = null
                pendingChatName = null
                pendingCreatePost = false
                pendingMyPosts = false
            }
            showingCreatePost -> showingCreatePost = false
            chatRole != null -> {
                chatRole = null
                chatName = null
            }
            animeDetailId != null -> animeDetailId = null
            postDetailId != null -> postDetailId = null
            showingRanking -> showingRanking = false
            showingMyPosts -> showingMyPosts = false
            showingFavorites -> showingFavorites = false
            showingEditProfile -> showingEditProfile = false
            showingProfile -> showingProfile = false
        }
    }
    Scaffold(
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!showingDetail) {
                AppBrandBar(
                    user = authState.user,
                    onSearch = {
                        selectedTab = 1
                        searchFocusRequest += 1
                    },
                    onAccount = { if (authState.user == null) showingLogin = true else showingProfile = true },
                    onMenu = { showingProfile = true },
                )
            }
        },
        bottomBar = {
            if (!showingDetail) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WarmBackground.copy(alpha = 0.96f),
                    shadowElevation = 0.dp,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.58f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
                        shadowElevation = 0.dp,
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(68.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                        ) {
                            mainTabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                            if (selectedTab == index) {
                                                Box(
                                                    Modifier.padding(bottom = 3.dp).height(3.dp).width(28.dp)
                                                        .background(FireflyGreen, RoundedCornerShape(9.dp)),
                                                )
                                            }
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00953A),
                                        selectedTextColor = Color(0xFF008B35),
                                        indicatorColor = FireflyGreen.copy(alpha = 0.10f),
                                        unselectedIconColor = Color(0xFF98A0AB),
                                        unselectedTextColor = Color(0xFF98A0AB),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                showingLogin -> LoginScreen(
                    uiState = authState,
                    onBack = {
                        showingLogin = false
                        pendingChatRole = null
                        pendingChatName = null
                        pendingCreatePost = false
                        pendingMyPosts = false
                    },
                    onLogin = { username, password ->
                        authViewModel.login(username, password) { finishAuthentication() }
                    },
                    onRegister = { username, displayName, password ->
                        authViewModel.register(username, displayName, password) { finishAuthentication() }
                    },
                    onClearError = authViewModel::clearError,
                )
                showingCreatePost && authState.user != null -> CreatePostRoute(
                    user = authState.user!!,
                    onBack = { showingCreatePost = false },
                    onPublished = { postId ->
                        showingCreatePost = false
                        postDetailId = postId
                    },
                )
                animeDetailId != null -> AnimeDetailRoute(
                    animeId = animeDetailId!!,
                    user = authState.user,
                    onBack = { animeDetailId = null },
                    onLoginRequired = { showingLogin = true },
                )
                postDetailId != null -> PostDetailRoute(
                    postId = postDetailId!!,
                    user = authState.user,
                    onBack = { postDetailId = null },
                    onLoginRequired = { showingLogin = true },
                )
                chatRole != null -> ChatRoute(
                    role = chatRole!!,
                    characterName = chatName ?: chatRole!!,
                    userId = authState.user?.id ?: -1L,
                    onBack = { chatRole = null; chatName = null },
                )
                showingRanking -> RankingRoute(
                    user = authState.user,
                    onBack = { showingRanking = false },
                    onAnimeClick = { animeDetailId = it },
                    onLoginRequired = { showingLogin = true },
                )
                showingMyPosts && authState.user != null -> MyPostsRoute(
                    onBack = { showingMyPosts = false },
                    onPostClick = { postDetailId = it },
                )
                showingFavorites -> FavoritesScreen(
                    onBack = { showingFavorites = false },
                    onAnimeClick = { animeDetailId = it },
                )
                showingEditProfile && authState.user != null -> EditProfileRoute(
                    user = authState.user!!,
                    onBack = { showingEditProfile = false },
                    onSaved = { showingEditProfile = false },
                )
                showingProfile -> ProfileScreen(
                    user = authState.user,
                    loading = authState.loading,
                    onBack = { showingProfile = false },
                    onLogin = { showingLogin = true },
                    onLogout = authViewModel::logout,
                    onFavorites = { showingFavorites = true },
                    onMyPosts = {
                        if (authState.user == null) {
                            pendingMyPosts = true
                            showingLogin = true
                        } else {
                            showingMyPosts = true
                        }
                    },
                    onEditProfile = { showingEditProfile = true },
                )
                selectedTab == 0 -> HomeRoute(
                    onAnimeClick = { animeDetailId = it },
                    onBrowseAnime = { selectedTab = 1 },
                    onAigcVote = { selectedTab = 4 },
                )
                selectedTab == 1 -> AnimeRoute(
                    onAnimeClick = { animeDetailId = it },
                    onRankingClick = { showingRanking = true },
                    focusSearchRequest = searchFocusRequest,
                )
                selectedTab == 2 -> RankingRoute(
                    user = authState.user,
                    onBack = { selectedTab = 1 },
                    onAnimeClick = { animeDetailId = it },
                    onLoginRequired = { showingLogin = true },
                    showBack = false,
                )
                selectedTab == 3 -> DailyScreen(
                    onBrowseAnime = { selectedTab = 1 },
                    onOpenRanking = { selectedTab = 2 },
                )
                selectedTab == 4 -> AgentRoute(onCharacterClick = { character ->
                    if (authState.user == null) {
                        pendingChatRole = character.code
                        pendingChatName = character.displayName
                        showingLogin = true
                    } else {
                        chatRole = character.code
                        chatName = character.displayName
                    }
                })
                else -> CommunityRoute(
                    onPostClick = { postDetailId = it },
                    onCreatePost = {
                        if (authState.user == null) {
                            pendingCreatePost = true
                            showingLogin = true
                        } else {
                            showingCreatePost = true
                        }
                    },
                )
            }
        }
    }
}
