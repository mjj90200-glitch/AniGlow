package com.aniglow.app.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun CommunityRoute(
    onPostClick: (Long) -> Unit,
    onCreatePost: () -> Unit,
    viewModel: CommunityViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CommunityScreen(uiState = uiState, onRetry = viewModel::refresh, onPostClick = onPostClick, onCreatePost = onCreatePost)
}

@Composable
fun CommunityScreen(
    uiState: CommunityUiState,
    onRetry: () -> Unit,
    onPostClick: (Long) -> Unit,
    onCreatePost: () -> Unit,
) {
    var sortIndex by remember { mutableIntStateOf(0) }
    val posts = if (sortIndex == 0) uiState.posts else uiState.posts.sortedByDescending { it.likeCount }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF4FBFA))),
        ),
    ) {
        when {
            uiState.loading && uiState.posts.isEmpty() -> LoadingState()
            uiState.errorMessage != null && uiState.posts.isEmpty() -> ErrorState(uiState.errorMessage, onRetry)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { CommunityHero() }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = Color.White.copy(alpha = 0.86f),
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFF0F1EE)),
                        ) {
                            Row {
                                SortTab("最新回复", selected = sortIndex == 0) { sortIndex = 0 }
                                SortTab("最多点亮", selected = sortIndex == 1) { sortIndex = 1 }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onCreatePost,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = FireflyGreen),
                            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 9.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" 发布", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (posts.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(items = posts, key = CommunityPost::id) { post ->
                        PostCard(post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityHero() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF292725), Color(0xFF172A42), Color(0xFF123758))),
            ).padding(24.dp),
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).size(150.dp).clip(CircleShape).background(Color(0xFF2F69A2).copy(alpha = 0.18f)),
            )
            Column {
                Box(
                    Modifier.size(66.dp).background(Color(0xFFFFD21F), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF184838), modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text("全部帖子", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    "浏览六个固定社区中的全部帖子，发现此刻大家正在讨论的内容。",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(Modifier.padding(top = 17.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroTag("全部")
                    HeroTag("最新")
                    HeroTag("热门")
                }
            }
        }
    }
}

@Composable
private fun HeroTag(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.11f)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
    }
}

@Composable
private fun SortTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = if (selected) FireflyGreen else Color(0xFF929BA7), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Box(Modifier.padding(top = 8.dp).width(52.dp).height(2.dp).background(if (selected) FireflyGreen else Color.Transparent, CircleShape))
    }
}

@Composable
private fun PostCard(post: CommunityPost, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.76f)),
        border = BorderStroke(1.dp, Color(0xFFEFF1EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(FireflyGreen.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(post.authorName.take(1), color = FireflyGreen, fontWeight = FontWeight.Black)
                    post.avatarUrl?.takeIf(String::isNotBlank)?.let { avatar ->
                        AsyncImage(
                            model = resolveMediaUrl(avatar),
                            contentDescription = "${post.authorName}的头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text("  ${post.communityName}", fontSize = 11.sp, color = FireflyGreen, fontWeight = FontWeight.Bold)
                    }
                    Text(formatCreatedAt(post.createdAt), fontSize = 10.sp, color = Color(0xFFA0A8B3))
                }
            }
            Spacer(Modifier.height(15.dp))
            Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF27313E))
            Text(
                post.content,
                modifier = Modifier.padding(top = 6.dp),
                color = Color(0xFF747D8B),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            post.images.firstOrNull()?.let { imageUrl ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F3EF)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = resolveMediaUrl(imageUrl),
                        contentDescription = "${post.title}的配图",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        contentScale = ContentScale.Fit,
                    )
                    if (post.images.size > 1) {
                        Text(
                            "+${post.images.size - 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                MetaItem(Icons.Outlined.FavoriteBorder, "${post.likeCount}人点亮")
                MetaItem(Icons.Outlined.ChatBubbleOutline, "${post.replyCount}条回复")
                Spacer(Modifier.weight(1f))
                MetaItem(Icons.Outlined.RemoveRedEye, post.viewCount.toString())
            }
        }
    }
}

@Composable
private fun MetaItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFA0A8B3), modifier = Modifier.size(17.dp))
        Text(" $text", color = Color(0xFFA0A8B3), fontSize = 11.sp)
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FireflyGreen) }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("社区暂时没有连接上", fontWeight = FontWeight.Black)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 18.dp), colors = ButtonDefaults.buttonColors(containerColor = FireflyGreen)) { Text("重新加载") }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Groups, contentDescription = null, tint = Color(0xFFB6BDC5), modifier = Modifier.size(46.dp))
        Text("还没有帖子", fontWeight = FontWeight.Black, color = Color(0xFF8D96A2), modifier = Modifier.padding(top = 10.dp))
    }
}

private fun formatCreatedAt(value: String): String = if (value.length < 10) value else value.take(10).replace('-', '.')
