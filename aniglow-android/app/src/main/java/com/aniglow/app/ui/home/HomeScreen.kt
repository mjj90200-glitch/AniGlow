package com.aniglow.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aniglow.app.data.model.PublicStats
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.HomeAnimeCard
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SakuraPink
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun HomeRoute(
    onAnimeClick: (Long) -> Unit,
    onBrowseAnime: () -> Unit,
    onAigcVote: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onAnimeClick = onAnimeClick,
        onBrowseAnime = onBrowseAnime,
        onAigcVote = onAigcVote,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onBrowseAnime: () -> Unit,
    onAigcVote: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFCF9), WarmBackground, Color(0xFFF7FCF8))),
        ),
    ) {
        Box(
            Modifier.padding(top = 92.dp, start = 260.dp).size(180.dp).clip(CircleShape)
                .background(SkyBlue.copy(alpha = 0.08f)),
        )
        Box(
            Modifier.padding(top = 210.dp).size(150.dp).clip(CircleShape)
                .background(SakuraPink.copy(alpha = 0.07f)),
        )

        when {
            uiState.loading && uiState.anime.isEmpty() -> Column {
                LoadingContent("正在准备今日推荐")
            }
            uiState.errorMessage != null && uiState.anime.isEmpty() -> Column {
                ErrorContent(uiState.errorMessage, onRetry)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item {
                    HeroCard(
                        stats = uiState.stats,
                        onBrowseAnime = onBrowseAnime,
                        onAigcVote = onAigcVote,
                        onRandomAnime = { uiState.anime.randomOrNull()?.let { onAnimeClick(it.id) } },
                    )
                }
                if (uiState.anime.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text("高分番剧", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("大家正在看的作品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("实时数据", color = FireflyGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.anime, key = { it.id }) { anime ->
                                HomeAnimeCard(anime, onClick = { onAnimeClick(anime.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(stats: PublicStats, onBrowseAnime: () -> Unit, onAigcVote: () -> Unit, onRandomAnime: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8).copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            Box(
                Modifier.align(Alignment.TopEnd).size(170.dp).clip(CircleShape)
                    .background(FireflyBright.copy(alpha = 0.055f)),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp)) {
                Surface(shape = CircleShape, color = FireflyBright.copy(alpha = 0.13f)) {
                    Row(
                        Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF42E89B)))
                        Text("2026 夏季新番上线", color = Color(0xFF00A846), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(25.dp))
                Text("萤火番舍", color = Color(0xFF202A38), fontSize = 39.sp, lineHeight = 43.sp, fontWeight = FontWeight.Black)
                Text(
                    "ANI GLOW",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Color(0xFF27B8A3), FireflyGreen, SakuraPink)),
                        fontSize = 39.sp,
                        lineHeight = 43.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
                Spacer(Modifier.height(27.dp))
                Text(
                    "以萤火般的微光，照亮每一部值得被看见的好番。探索、发现、投票，让好作品被更多人看见。",
                    color = Color(0xFF747D8B),
                    fontSize = 17.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(30.dp))
                HeroButton("浏览番剧", Icons.Outlined.LiveTv, FireflyGreen, onBrowseAnime)
                Spacer(Modifier.height(12.dp))
                HeroButton("AIGC 投票", Icons.Outlined.AutoAwesome, Color(0xFFFF9CB2), onAigcVote)
                Row(
                    modifier = Modifier.padding(top = 17.dp).clickable(onClick = onRandomAnime),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Outlined.Casino, contentDescription = null, tint = Color(0xFFA0A8B4), modifier = Modifier.size(18.dp))
                    Text("不知道看什么？随机遇见一部", color = Color(0xFFA0A8B4), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(25.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.9f)))
                Spacer(Modifier.height(21.dp))
                StatsRow(stats)
            }
        }
    }
}

@Composable
private fun HeroButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(59.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp, pressedElevation = 2.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StatsRow(stats: PublicStats) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeroStat(formatCompact(stats.animeCount), "收录番剧", Modifier.weight(1f))
        StatDivider()
        HeroStat(formatCompact(stats.agentCount), "驻站 AIGC", Modifier.weight(1f))
        StatDivider()
        HeroStat(formatCompact(stats.memberCount), "社区成员", Modifier.weight(1f))
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.size(width = 1.dp, height = 35.dp).background(Color(0xFFE5E9EC)))
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFF202A38), fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(label, color = Color(0xFF9AA3AE), fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

private fun formatCompact(value: Int): String = when {
    value >= 10_000 -> "%.1fw".format(value / 10_000.0)
    value >= 1_000 -> "%.1fk".format(value / 1_000.0)
    else -> value.toString()
}
