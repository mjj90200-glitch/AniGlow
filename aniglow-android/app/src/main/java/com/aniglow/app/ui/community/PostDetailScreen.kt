package com.aniglow.app.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.model.CommunityReply
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.common.GlassBackTopBar
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun PostDetailRoute(
    postId: Long,
    user: AuthUser?,
    onBack: () -> Unit,
    onLoginRequired: () -> Unit,
) {
    val viewModel: PostDetailViewModel = viewModel(
        key = "post-detail-$postId",
        factory = PostDetailViewModel.Factory(postId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PostDetailScreen(
        uiState = uiState,
        user = user,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onLike = {
            if (user == null) onLoginRequired() else viewModel.like()
        },
        onReply = { content ->
            if (user == null) onLoginRequired() else viewModel.reply(content, user)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailScreen(
    uiState: PostDetailUiState,
    user: AuthUser?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLike: () -> Unit,
    onReply: (String) -> Unit,
) {
    var replyText by rememberSaveable { mutableStateOf("") }
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("帖子详情", onBack, "COMMUNITY POST")
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF3FBF8))),
            ),
        ) {
            when {
                uiState.loading && uiState.post == null -> LoadingContent("正在加载帖子")
                uiState.errorMessage != null && uiState.post == null -> ErrorContent(uiState.errorMessage, onRetry)
                uiState.post != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { PostBody(uiState.post, uiState.actionLoading, onLike) }
                    item {
                        Text(
                            "回复 ${uiState.post.replyCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (uiState.replies.isEmpty()) {
                        item { Text("还没有回复", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
                    } else {
                        items(uiState.replies, key = { it.id }) { reply -> ReplyCard(reply) }
                    }
                    item {
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(if (user == null) "登录后参与回复" else "写回复", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it.take(1200) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    placeholder = { Text("友善交流，分享你的想法") },
                                    minLines = 2,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(18.dp),
                                )
                                uiState.actionMessage?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 7.dp))
                                }
                                Button(
                                    onClick = {
                                        if (user == null) onLike() else {
                                            onReply(replyText)
                                            replyText = ""
                                        }
                                    },
                                    enabled = user == null || (replyText.isNotBlank() && !uiState.actionLoading),
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    shape = CircleShape,
                                ) { Text(if (user == null) "登录后回复" else if (uiState.actionLoading) "正在提交…" else "发布回复") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostBody(post: CommunityPost, actionLoading: Boolean, onLike: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(post.authorName, post.avatarUrl)
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Text(post.authorName, fontWeight = FontWeight.Black)
                    Text(post.communityName, color = FireflyGreen, style = MaterialTheme.typography.labelMedium)
                }
                Text(formatDetailDate(post.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Text(post.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 18.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp))
            post.images.forEach { imageUrl ->
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF3F1EC)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = resolveMediaUrl(imageUrl),
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Color(0xFFE9E6E0))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onLike, enabled = !actionLoading, shape = CircleShape) {
                    Text(if (post.likedByMe) "♥ 已点亮 ${post.likeCount}" else "♡ 点亮 ${post.likeCount}")
                }
                Text("${post.viewCount} 次浏览", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReplyCard(reply: CommunityReply) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Avatar(reply.authorName, reply.avatarUrl)
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(reply.authorName, fontWeight = FontWeight.Bold)
                    Text(formatDetailDate(reply.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(reply.content, modifier = Modifier.padding(top = 7.dp))
                Text("♥ ${reply.likeCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 9.dp))
            }
        }
    }
}

@Composable
private fun Avatar(name: String, url: String?) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(FireflyGreen.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1), color = FireflyGreen, fontWeight = FontWeight.Black)
        if (!url.isNullOrBlank()) AsyncImage(
            model = resolveMediaUrl(url),
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun formatDetailDate(value: String): String = value.take(10).replace('-', '.')
