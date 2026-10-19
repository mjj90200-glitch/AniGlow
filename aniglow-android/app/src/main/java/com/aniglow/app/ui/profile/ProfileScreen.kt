package com.aniglow.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.favorite.FavoriteStore
import com.aniglow.app.data.remote.resolveMediaUrl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aniglow.app.ui.common.GlassBackTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: AuthUser?,
    loading: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onFavorites: () -> Unit,
    onMyPosts: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val favorites by FavoriteStore.items.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("我的", onBack, "MY ANIGLOW")
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
            ).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(FireflyGreen.copy(alpha = 0.13f), Color(0xFFE9F6FA))),
                    ).padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.size(62.dp).clip(CircleShape).background(FireflyGreen.copy(alpha = 0.12f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(user?.visibleName?.take(1) ?: "萤", color = FireflyGreen, fontWeight = FontWeight.Black)
                        user?.avatarUrl?.takeIf(String::isNotBlank)?.let { avatarUrl ->
                            AsyncImage(
                                model = resolveMediaUrl(avatarUrl),
                                contentDescription = user.visibleName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Column(Modifier.padding(start = 15.dp)) {
                        Text(user?.visibleName ?: "游客", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            if (user == null) "登录后同步你的收藏与创作" else "@${user.username}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (user == null) {
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("登录或注册") }
            } else {
                OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth(), shape = CircleShape) { Text("编辑个人资料") }
            }
            Text("我的空间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
            ProfileRow("收藏的番剧", "${favorites.size} 部 · 保存在本机", onClick = onFavorites)
            ProfileRow("我的帖子", if (user == null) "登录后管理" else "查看帖子", onClick = onMyPosts)
            ProfileRow("对话记录", "即将接入")
            Spacer(Modifier.height(6.dp))
            if (user == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = FireflyGreen.copy(alpha = 0.09f)),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("一个账号，多端同步", color = FireflyGreen, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text("App 已经与网页端共用账号体系。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                OutlinedButton(onClick = onLogout, enabled = !loading, modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
                    Text(if (loading) "正在退出…" else "退出登录")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
