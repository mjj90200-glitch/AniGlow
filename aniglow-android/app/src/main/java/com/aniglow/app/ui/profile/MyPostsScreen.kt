package com.aniglow.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground
import com.aniglow.app.ui.common.GlassBackTopBar

@Composable
fun MyPostsRoute(
    onBack: () -> Unit,
    onPostClick: (Long) -> Unit,
    viewModel: MyPostsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyPostsScreen(uiState, onBack, onPostClick, viewModel::refresh, viewModel::delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyPostsScreen(
    uiState: MyPostsUiState,
    onBack: () -> Unit,
    onPostClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("我的帖子", onBack, "MY POSTS")
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
            ),
        ) {
            when {
                uiState.loading && uiState.posts.isEmpty() -> LoadingContent("正在加载我的帖子")
                uiState.errorMessage != null && uiState.posts.isEmpty() -> ErrorContent(uiState.errorMessage, onRetry)
                else -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    uiState.actionMessage?.let { item { Text(it, color = FireflyGreen) } }
                    if (uiState.posts.isEmpty()) item { Text("你还没有发布帖子", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
                    items(uiState.posts, key = { it.id }) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPostClick(post.id) },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        ) {
                            Column(Modifier.padding(15.dp)) {
                                Text(post.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                Text(post.content, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${post.likeCount} 点亮 · ${post.replyCount} 回复", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { onDelete(post.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
