package com.aniglow.app.ui.daily

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SakuraPink
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun DailyScreen(
    onBrowseAnime: () -> Unit,
    onOpenRanking: () -> Unit,
) {
    val dailyState by DailyStore.state.collectAsStateWithLifecycle()
    val browseCompleted = DailyStore.BROWSE_FIVE_MINUTES in dailyState.completedTaskIds
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WarmBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7)),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Box {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(170.dp).background(
                            Brush.radialGradient(listOf(FireflyBright.copy(alpha = 0.14f), Color.Transparent)),
                        ),
                    )
                    Column(Modifier.padding(26.dp)) {
                        Surface(shape = CircleShape, color = FireflyGreen.copy(alpha = 0.12f)) {
                            Row(
                                Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(9.dp).background(FireflyBright, CircleShape))
                                Text("  DAILY FIREFLY", color = Color(0xFF00A846), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("今日番舍小任务", fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black, color = Color(0xFF202A38))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "每天做一点轻轻的互动，完成任务获得投票萤火，把今天的喜欢投给最想推上榜的番剧。",
                            color = Color(0xFF747D8B),
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DailyStat("${dailyState.completedCount} / 3", "今日完成", Modifier.weight(1f))
                            DailyStat(dailyState.tickets.toString(), "可用投票", Modifier.weight(1f), FireflyGreen)
                            DailyStat("3", "今日可得", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("VOTE TICKETS", color = Color(0xFF00A846), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
                            Text("投票萤火袋", fontSize = 23.sp, fontWeight = FontWeight.Black)
                        }
                        Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = FireflyGreen, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(FireflyGreen.copy(alpha = 0.13f), SkyBlue.copy(alpha = 0.14f))),
                            RoundedCornerShape(24.dp),
                        ).padding(20.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("当前可投", color = Color(0xFF747D8B))
                            Text(dailyState.tickets.toString(), fontSize = 42.sp, fontWeight = FontWeight.Black)
                        }
                        Text("张番剧投票", color = Color(0xFF00A846), fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 7.dp))
                    }
                }
            }
        }
        item { Text("今日任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
        item {
            DailyTask(
                icon = Icons.Outlined.Visibility,
                title = "去评论区偷听萤火",
                description = "打开任意一部番剧详情，看看同好们正在怎样理解它。",
                accent = SkyBlue,
                completed = DailyStore.VIEW_COMMENTS in dailyState.completedTaskIds,
                onClick = onBrowseAnime,
            )
        }
        item {
            DailyTask(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "写下一句今日感想",
                description = "在任意作品下发布一条文字评价，把你的喜欢认真留住。",
                accent = SakuraPink,
                completed = DailyStore.PUBLISH_COMMENT in dailyState.completedTaskIds,
                onClick = onBrowseAnime,
            )
        }
        item {
            DailyTask(
                icon = Icons.Outlined.AccessTime,
                title = "番舍散步 5 分钟",
                description = if (browseCompleted) "今天的番舍散步已经完成。" else "已浏览 ${formatDuration(dailyState.browseSeconds)} / 05:00，停留在应用内会自动累计。",
                accent = FireflyGreen,
                completed = browseCompleted,
                progress = dailyState.browseSeconds / DailyStore.BROWSE_TARGET_SECONDS.toFloat(),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).background(FireflyGreen.copy(alpha = 0.09f), RoundedCornerShape(22.dp)).clickable(onClick = onOpenRanking).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = FireflyGreen)
                Text("带着 ${dailyState.tickets} 张萤火票去排行榜投票", modifier = Modifier.padding(start = 10.dp), color = Color(0xFF657068), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DailyStat(value: String, label: String, modifier: Modifier, valueColor: Color = Color(0xFF202A38)) {
    Column(
        modifier.background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(20.dp)).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(label, fontSize = 10.sp, color = Color(0xFF9AA3AE))
    }
}

@Composable
private fun DailyTask(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    completed: Boolean,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(46.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, color = Color(0xFF28313D))
                    Surface(shape = CircleShape, color = if (completed) FireflyGreen.copy(alpha = 0.12f) else Color(0xFFF6F5F1)) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (completed) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FireflyGreen, modifier = Modifier.size(13.dp))
                            Text(if (completed) " 已完成" else "+1 投票", fontSize = 10.sp, color = if (completed) FireflyGreen else Color(0xFF8A929C), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(description, modifier = Modifier.padding(top = 5.dp), color = Color(0xFF747D8B), fontSize = 13.sp, lineHeight = 20.sp)
                if (progress != null && !completed) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.14f),
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
