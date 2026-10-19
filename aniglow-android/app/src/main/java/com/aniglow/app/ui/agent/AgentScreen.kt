package com.aniglow.app.ui.agent

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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AgentCharacter
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.data.remote.resolveCharacterImageUrl
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SakuraPink
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun AgentRoute(
    onCharacterClick: (AgentCharacter) -> Unit,
    viewModel: AgentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AgentScreen(uiState, viewModel::refresh, onCharacterClick)
}

@Composable
private fun AgentScreen(uiState: AgentUiState, onRetry: () -> Unit, onCharacterClick: (AgentCharacter) -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF1FBF6))),
        ),
    ) {
        when {
            uiState.loading && uiState.characters.isEmpty() -> LoadingContent("正在召集角色")
            uiState.errorMessage != null && uiState.characters.isEmpty() -> ErrorContent(uiState.errorMessage, onRetry)
            uiState.characters.isEmpty() -> ErrorContent("暂时没有可会面的角色", onRetry)
            else -> {
                var selectedIndex by remember(uiState.characters) { mutableIntStateOf(0) }
                val selected = uiState.characters[selectedIndex.coerceIn(uiState.characters.indices)]
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
                ) {
                    item {
                        CharacterSelector(
                            characters = uiState.characters,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it },
                        )
                    }
                    item { FeaturedCharacter(selected, onClick = { onCharacterClick(selected) }) }
                }
            }
        }
    }
}

@Composable
private fun CharacterSelector(characters: List<AgentCharacter>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.66f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                AgentAvatar(characters[selectedIndex], Modifier.size(48.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(characters[selectedIndex].displayName, fontWeight = FontWeight.Black, color = Color(0xFF28313D))
                    Text("等待中…", color = Color(0xFF009C62), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = CircleShape, color = Color(0xFFF7F7F4)) {
                    Box(Modifier.size(43.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Search, contentDescription = "查找角色", tint = Color(0xFF8893A1))
                    }
                }
            }
            if (characters.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(start = 14.dp, top = 10.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(characters.indices.toList()) { index ->
                        Surface(
                            modifier = Modifier.clickable { onSelect(index) },
                            shape = CircleShape,
                            color = if (selectedIndex == index) FireflyGreen.copy(alpha = 0.13f) else Color(0xFFF5F4F0),
                            border = if (selectedIndex == index) androidx.compose.foundation.BorderStroke(1.dp, FireflyGreen.copy(alpha = 0.35f)) else null,
                        ) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                AgentAvatar(characters[index], Modifier.size(28.dp))
                                Text(characters[index].displayName, modifier = Modifier.padding(start = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCharacter(character: AgentCharacter, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(580.dp).clip(RoundedCornerShape(32.dp)).background(
            Brush.radialGradient(
                listOf(FireflyBright.copy(alpha = 0.18f), SkyBlue.copy(alpha = 0.09f), SakuraPink.copy(alpha = 0.07f), Color.White.copy(alpha = 0.2f)),
            ),
        ),
    ) {
        character.backgroundUrl?.takeIf(String::isNotBlank)?.let { background ->
            AsyncImage(
                model = resolveMediaUrl(background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.12f,
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AgentAvatar(character, Modifier.size(110.dp), RoundedCornerShape(28.dp))
            Spacer(Modifier.height(18.dp))
            Text(character.displayName, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF33404D))
            Text(
                "来自《${character.sourceTitle.orEmpty().ifBlank { "萤火番舍" }}》",
                color = Color(0xFF00A846),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 5.dp),
            )
            Text(
                "那个……你好……我是${character.displayName}。虽然有点紧张，但我会努力回应你的！",
                color = Color(0xFF9AA3AE),
                fontSize = 16.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 19.dp, bottom = 24.dp),
            )
            SuggestionChip("你最近在忙什么？", onClick)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip("喜欢的动漫？", onClick, Modifier.weight(1f))
                SuggestionChip("推荐一部番", onClick, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp).clickable(onClick = onClick),
                shape = CircleShape,
                color = FireflyGreen,
                shadowElevation = 0.dp,
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.White)
                    Text("  开始对话", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = FireflyGreen, modifier = Modifier.size(17.dp))
                Text(" 真实模型驱动 · 每位角色都有自己的语气", color = Color(0xFF7F8995), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = FireflyGreen.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, FireflyBright.copy(alpha = 0.5f)),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp), color = Color(0xFF00A846), fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AgentAvatar(character: AgentCharacter, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    Box(modifier.clip(shape).background(FireflyGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Text(character.displayName.take(1), color = FireflyGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        AsyncImage(
            model = resolveCharacterImageUrl(character),
            contentDescription = character.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
