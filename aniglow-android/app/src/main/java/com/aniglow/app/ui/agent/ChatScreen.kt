package com.aniglow.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aniglow.app.ui.common.GlassBackTopBar
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun ChatRoute(role: String, characterName: String, userId: Long, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel(
        key = "chat-$userId-$role",
        factory = ChatViewModel.Factory(role, userId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(characterName, uiState, onBack, viewModel::send, viewModel::clearError, viewModel::clearHistory)
}

@Composable
private fun ChatScreen(
    characterName: String,
    uiState: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onClearError: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size, uiState.sending) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }
    fun submit() {
        if (input.isNotBlank() && !uiState.sending) {
            onSend(input)
            input = ""
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = WarmBackground,
        topBar = { GlassBackTopBar(characterName, onBack, uiState.quotaRemaining?.let { "本时段剩余 $it 次" } ?: "角色对话") },
        bottomBar = {
            Surface(color = Color.White.copy(alpha = 0.94f), shadowElevation = 12.dp) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                    uiState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it; onClearError() },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("对 $characterName 说点什么…", color = Color(0xFF9AA3AE)) },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAF7),
                                unfocusedContainerColor = Color(0xFFF8FAF7),
                                focusedBorderColor = FireflyGreen.copy(alpha = 0.45f),
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submit() }),
                        )
                        Button(
                            onClick = { submit() },
                            enabled = input.isNotBlank() && !uiState.sending,
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FireflyGreen),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "发送", modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF1FBF7))),
            ),
        ) {
            if (uiState.messages.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(112.dp).background(
                            Brush.radialGradient(listOf(FireflyBright.copy(alpha = 0.22f), SkyBlue.copy(alpha = 0.09f))),
                            RoundedCornerShape(34.dp),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = FireflyGreen, modifier = Modifier.size(48.dp))
                    }
                    Text("和 $characterName 开始一段对话", fontWeight = FontWeight.Black, fontSize = 23.sp, color = Color(0xFF2A3441), modifier = Modifier.padding(top = 20.dp))
                    Text(
                        "每一次回复都由真实模型生成。聊聊今天、动漫或任何让你在意的事情。",
                        color = Color(0xFF8C96A3),
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                    Surface(modifier = Modifier.padding(top = 18.dp), shape = CircleShape, color = FireflyGreen.copy(alpha = 0.08f)) {
                        Text("请勿发送隐私信息", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF00A846), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onClearHistory) { Text("清空聊天记录", color = Color(0xFF8C96A3), fontSize = 11.sp) }
                        }
                    }
                    items(uiState.messages.size) { index -> MessageBubble(uiState.messages[index], characterName) }
                    if (uiState.sending) item {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.8f)) {
                            Text("$characterName 正在思考…", color = Color(0xFF7F8995), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, characterName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
            Text(
                if (message.fromUser) "我" else characterName,
                fontSize = 10.sp,
                color = Color(0xFF929BA6),
                modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 4.dp),
            )
            Text(
                text = message.content,
                modifier = Modifier.widthIn(max = 310.dp).background(
                    if (message.fromUser) Brush.linearGradient(listOf(FireflyGreen, Color(0xFF1FD17B))) else Brush.linearGradient(listOf(Color.White, Color(0xFFF9FBF9))),
                    RoundedCornerShape(20.dp),
                ).padding(horizontal = 15.dp, vertical = 12.dp),
                color = if (message.fromUser) Color.White else Color(0xFF35404C),
                lineHeight = 22.sp,
            )
        }
    }
}
