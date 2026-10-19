package com.aniglow.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.AnimeRating
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.favorite.FavoriteStore
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.common.GlassBackTopBar
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun AnimeDetailRoute(
    animeId: Long,
    user: AuthUser?,
    onBack: () -> Unit,
    onLoginRequired: () -> Unit,
) {
    val viewModel: AnimeDetailViewModel = viewModel(
        key = "anime-detail-$animeId",
        factory = AnimeDetailViewModel.Factory(animeId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by FavoriteStore.items.collectAsStateWithLifecycle()
    val dailyState by DailyStore.state.collectAsStateWithLifecycle()
    LaunchedEffect(animeId) {
        DailyStore.completeTask(DailyStore.VIEW_COMMENTS)
    }
    AnimeDetailScreen(
        uiState = uiState,
        user = user,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onLoginRequired = onLoginRequired,
        onRating = { score, review ->
            if (user == null) onLoginRequired() else viewModel.submitRating(score, review, user)
        },
        onVote = {
            when {
                user == null -> onLoginRequired()
                dailyState.tickets <= 0 -> viewModel.showMessage("先完成每日任务，获得萤火票后再来投票吧")
                else -> viewModel.vote()
            }
        },
        tickets = dailyState.tickets,
        isFavorite = favorites.any { it.id == animeId },
        onFavorite = { uiState.anime?.let(FavoriteStore::toggle) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeDetailScreen(
    uiState: AnimeDetailUiState,
    user: AuthUser?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoginRequired: () -> Unit,
    onRating: (Double, String) -> Unit,
    onVote: () -> Unit,
    tickets: Int,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
) {
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("番剧详情", onBack, "ANIME DETAILS")
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.loading && uiState.anime == null -> LoadingContent("正在加载番剧详情")
                uiState.errorMessage != null && uiState.anime == null -> ErrorContent(uiState.errorMessage, onRetry)
                uiState.anime != null -> AnimeDetailBody(
                    uiState = uiState,
                    user = user,
                    onLoginRequired = onLoginRequired,
                    onRating = onRating,
                    onVote = onVote,
                    tickets = tickets,
                    isFavorite = isFavorite,
                    onFavorite = onFavorite,
                )
            }
        }
    }
}

@Composable
private fun AnimeDetailBody(
    uiState: AnimeDetailUiState,
    user: AuthUser?,
    onLoginRequired: () -> Unit,
    onRating: (Double, String) -> Unit,
    onVote: () -> Unit,
    tickets: Int,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
) {
    val anime = uiState.anime ?: return
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFFFFF9F4), Color(0xFFF0FBF5))),
                ).padding(18.dp),
            ) {
                AsyncImage(
                    model = resolveMediaUrl(anime.coverImage),
                    contentDescription = anime.displayTitle,
                    modifier = Modifier.width(132.dp).aspectRatio(0.72f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFEDEBE5)),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text("ANIME", color = FireflyGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    Text(anime.displayTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFF27313E))
                    anime.title.takeIf { it.isNotBlank() && it != anime.displayTitle }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoLine("类型", anime.type)
                    InfoLine("年份", anime.year?.toString())
                    InfoLine("集数", anime.episodes?.let { "$it 集" })
                    InfoLine("制作", anime.studio)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreCard("综合评分", anime.displayRating?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
            ScoreCard("社区评分", anime.communityMeanRating?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
            ScoreCard("萤火票", anime.fireflyVoteCount.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onFavorite, modifier = Modifier.weight(1f).height(50.dp), shape = CircleShape) {
                Text(if (isFavorite) "已收藏" else "收藏")
            }
            Button(onClick = onVote, enabled = !uiState.actionLoading, modifier = Modifier.weight(1f).height(50.dp), shape = CircleShape) {
                Text(if (user == null) "登录后投票" else if (tickets > 0) "投萤火票 · $tickets" else "暂无萤火票")
            }
        }
        uiState.actionMessage?.let {
            Text(it, color = FireflyGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }
        if (anime.genres.isNotEmpty()) {
            Text(anime.genres.joinToString(" · "), color = FireflyGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
        }
        Text("剧情简介", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        ) {
            Text(
                text = anime.synopsisCn?.takeIf(String::isNotBlank) ?: anime.synopsis.orEmpty().ifBlank { "暂无简介" },
                modifier = Modifier.padding(17.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RatingSection(
            user = user,
            reviews = uiState.reviews,
            actionLoading = uiState.actionLoading,
            onLoginRequired = onLoginRequired,
            onRating = onRating,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun RatingSection(
    user: AuthUser?,
    reviews: List<AnimeRating>,
    actionLoading: Boolean,
    onLoginRequired: () -> Unit,
    onRating: (Double, String) -> Unit,
) {
    var score by rememberSaveable { mutableFloatStateOf(8f) }
    var review by rememberSaveable { mutableStateOf("") }
    Text("我的评分", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 28.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${score.toInt()} / 10", color = FireflyGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Slider(
                value = score,
                onValueChange = { score = it },
                valueRange = 1f..10f,
                steps = 8,
                enabled = !actionLoading,
            )
            OutlinedTextField(
                value = review,
                onValueChange = { review = it.take(2000) },
                label = { Text("写一句评价（可选）") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Button(
                onClick = {
                    if (user == null) onLoginRequired() else onRating(score.toDouble(), review)
                },
                enabled = !actionLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp),
                shape = CircleShape,
            ) { Text(if (user == null) "登录后评分" else if (actionLoading) "正在保存…" else "保存评分") }
        }
    }
    Text("大家的评价 · ${reviews.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
    if (reviews.isEmpty()) {
        Text("还没有文字评价", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        reviews.forEach { rating ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(rating.authorName, fontWeight = FontWeight.Bold)
                        Text("★ %.1f".format(rating.score), color = FireflyGreen, fontWeight = FontWeight.Black)
                    }
                    rating.review?.takeIf(String::isNotBlank)?.let {
                        Text(it, modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) Text("$label · $value", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun ScoreCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, color = FireflyGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}
