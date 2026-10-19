package com.aniglow.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniglow.app.ui.common.GlassBackTopBar
import com.aniglow.app.ui.theme.FireflyBright
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SakuraPink
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = WarmBackground,
        topBar = { GlassBackTopBar(if (registerMode) "注册账号" else "登录", onBack, "ANIGLOW ACCOUNT") },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(72.dp).background(Brush.linearGradient(listOf(FireflyBright, FireflyGreen)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Text(
                    if (registerMode) "加入萤火番舍" else "欢迎回来",
                    color = Color(0xFF222C38),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    if (registerMode) "创建一个账号，在网页和 App 之间同步你的相遇。" else "使用网页端的用户名和密码登录。",
                    color = Color(0xFF7B8592),
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 7.dp, bottom = 20.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.87f)),
                    elevation = CardDefaults.cardElevation(3.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth().background(Color(0xFFF5F5F1), CircleShape).padding(4.dp)) {
                            AuthMode("登录", !registerMode, Modifier.weight(1f)) { registerMode = false; onClearError() }
                            AuthMode("注册", registerMode, Modifier.weight(1f)) { registerMode = true; onClearError() }
                        }
                        Spacer(Modifier.height(18.dp))
                        AuthField(username, { username = it; onClearError() }, "用户名", Icons.Outlined.Person)
                        if (registerMode) {
                            Spacer(Modifier.height(12.dp))
                            AuthField(displayName, { displayName = it; onClearError() }, "昵称（可选）", Icons.Outlined.PersonAdd)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; onClearError() },
                            label = { Text("密码") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF8C96A3)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = authFieldColors(),
                        )
                        uiState.errorMessage?.let {
                            Surface(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(14.dp), color = SakuraPink.copy(alpha = 0.13f)) {
                                Text(it, color = Color(0xFFB74862), modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = {
                                if (registerMode) onRegister(username, displayName, password) else onLogin(username, password)
                            },
                            enabled = !uiState.loading,
                            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(54.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = FireflyGreen),
                        ) {
                            Text(if (uiState.loading) "请稍候…" else if (registerMode) "注册并登录" else "登录", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
                Text("登录即表示你愿意与同好友善交流", color = Color(0xFF9AA3AE), fontSize = 11.sp, modifier = Modifier.padding(top = 15.dp))
            }
        }
    }
}

@Composable
private fun AuthMode(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(if (selected) Color.White else Color.Transparent, CircleShape),
    ) { Text(text, color = if (selected) FireflyGreen else Color(0xFF8F98A4), fontWeight = FontWeight.Black) }
}

@Composable
private fun AuthField(value: String, onChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF8C96A3)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = authFieldColors(),
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFFBFAF7),
    focusedBorderColor = FireflyGreen.copy(alpha = 0.55f),
    unfocusedBorderColor = Color(0xFFE7E6E1),
)
