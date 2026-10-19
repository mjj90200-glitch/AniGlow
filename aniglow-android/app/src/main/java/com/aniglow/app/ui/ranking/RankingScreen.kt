package com.aniglow.app.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun RankingRoute(
    user: AuthUser?,
    onBack: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onLoginRequired: () -> Unit,
    showBack: Boolean = true,
    viewModel: RankingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dailyState by DailyStore.state.collectAsStateWithLifecycle()
    RankingScreen(
        uiState = uiState,
        user = user,
        onBack = onBack,
        showBack = showBack,
        onAnimeClick = onAnimeClick,
        onModeChange = viewModel::changeMode,
        onRetry = viewModel::refresh,
        tickets = dailyState.tickets,
        onVote = { id ->
            when {
                user == null -> onLoginRequired()
                dailyState.tickets <= 0 -> viewModel.showMessage("先完成每日任务，获得萤火票后再来投票吧")
                else -> viewModel.vote(id)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingScreen(
    uiState: RankingUiState,
    user: AuthUser?,
    onBack: () -> Unit,
    showBack: Boolean,
    onAnimeClick: (Long) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    tickets: Int,
    onVote: (Long) -> Unit,
) {
    Scaffold(
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showBack) {
                TopAppBar(
                    navigationIcon = { TextButton(onClick = onBack) { Text("‹ 返回") } },
                    title = { Text("番剧排行榜", fontWeight = FontWeight.Black) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground),
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFBF8), WarmBackground, Color(0xFFF4FBF9))),
            ),
        ) {
            when {
                uiState.loading && uiState.items.isEmpty() -> LoadingContent("正在计算排行")
                uiState.errorMessage != null && uiState.items.isEmpty() -> ErrorContent(uiState.errorMessage, onRetry)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(Modifier.padding(horizontal = 2.dp, vertical = 3.dp)) {
                            Text("RANKINGS", color = Color(0xFF00A846), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("番剧排行榜", color = Color(0xFF202A38), fontSize = 31.sp, fontWeight = FontWeight.Black)
                                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.padding(start = 8.dp).size(31.dp))
                            }
                            Text("站内评分与萤火投票共同组成真实用户排行", color = Color(0xFF747D8B), fontSize = 13.sp)
                            Spacer(Modifier.height(15.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.66f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                                shadowElevation = 0.dp,
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = FireflyGreen, modifier = Modifier.size(18.dp))
                                    Text("  今日投票萤火 $tickets", color = Color(0xFF47515E), fontWeight = FontWeight.Black)
                                    Text("  完成每日任务获取", color = Color(0xFF00A846), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                RankingModeButton("中国用户评分", !uiState.fireflyMode, { onModeChange(false) })
                                RankingModeButton("萤火投票榜", uiState.fireflyMode, { onModeChange(true) })
                            }
                            uiState.actionMessage?.let {
                                Text(it, color = FireflyGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                            }
                        }
                    }
                    itemsIndexed(uiState.items, key = { _, anime -> anime.id }) { index, anime ->
                        RankingCard(
                            anime = anime,
                            index = index,
                            fireflyMode = uiState.fireflyMode,
                            loggedIn = user != null,
                            hasTicket = tickets > 0,
                            voting = uiState.votingAnimeId == anime.id,
                            onClick = { onAnimeClick(anime.id) },
                            onVote = { onVote(anime.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) FireflyGreen else Color.White.copy(alpha = 0.76f),
            contentColor = if (selected) Color.White else Color(0xFF737C88),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun RankingCard(
    anime: AnimeSummary,
    index: Int,
    fireflyMode: Boolean,
    loggedIn: Boolean,
    hasTicket: Boolean,
    voting: Boolean,
    onClick: () -> Unit,
    onVote: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(rankColor(index)),
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", color = if (index < 3) Color(0xFF7A5200) else Color(0xFF59616C), fontWeight = FontWeight.Black) }
            AsyncImage(
                model = resolveMediaUrl(anime.coverImage),
                contentDescription = anime.displayTitle,
                modifier = Modifier.padding(start = 10.dp).width(55.dp).height(73.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(anime.displayTitle, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF27313E))
                Text(
                    listOfNotNull(anime.type, anime.episodes?.let { "$it 集" }, anime.studio).joinToString("  "),
                    color = Color(0xFF9AA3AE),
                    fontSize = 11.sp,
                    maxLines = 2,
                )
                Text("${anime.fireflyVoteCount} 票萤火", color = Color(0xFF009C3B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (fireflyMode) anime.fireflyVoteCount.toString() else anime.communityMeanRating?.let { "%.1f".format(it) } ?: "—",
                    color = if (fireflyMode) FireflyGreen else Color(0xFFFFB300),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(if (fireflyMode) "萤火票" else "站内评分", color = Color(0xFF9AA3AE), fontSize = 9.sp)
                Surface(
                    modifier = Modifier.padding(top = 5.dp).clickable(enabled = !voting, onClick = onVote),
                    shape = CircleShape,
                    color = FireflyGreen.copy(alpha = 0.09f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FireflyGreen.copy(alpha = 0.2f)),
                ) {
                    Text(
                        if (voting) "投票中…" else if (!loggedIn) "登录投票" else if (hasTicket) "投票" else "暂无票",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (loggedIn && !hasTicket) Color(0xFF9AA3AE) else FireflyGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

private fun rankColor(index: Int): Color = when (index) {
    0 -> Color(0xFFFFC233)
    1 -> Color(0xFFB8B8B8)
    2 -> Color(0xFFFFB04A)
    else -> Color(0xFFF0EEE8)
}
