package com.aniglow.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground
import com.aniglow.app.ui.common.GlassBackTopBar

@Composable
fun EditProfileRoute(
    user: AuthUser,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(key = "edit-profile-${user.id}"),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var displayName by rememberSaveable(user.id) { mutableStateOf(user.visibleName) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = viewModel::selectAvatar,
    )
    EditProfileScreen(
        user = user,
        uiState = uiState,
        displayName = displayName,
        onDisplayNameChange = { displayName = it.take(80) },
        onPickAvatar = {
            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemoveAvatar = viewModel::removeAvatar,
        onBack = onBack,
        onSave = { viewModel.save(displayName, user.avatarUrl) { onSaved() } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileScreen(
    user: AuthUser,
    uiState: EditProfileUiState,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val avatarModel = when {
        uiState.selectedAvatar != null -> uiState.selectedAvatar
        uiState.removeAvatar -> null
        else -> user.avatarUrl?.takeIf(String::isNotBlank)?.let(::resolveMediaUrl)
    }
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("编辑资料", onBack, "PROFILE SETTINGS")
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
            ).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(112.dp).clip(CircleShape).background(FireflyGreen.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(displayName.take(1).ifBlank { "萤" }, color = FireflyGreen, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                if (avatarModel != null) {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = "头像预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPickAvatar, enabled = !uiState.saving, shape = CircleShape) { Text("选择头像") }
                OutlinedButton(onClick = onRemoveAvatar, enabled = !uiState.saving, shape = CircleShape) { Text("移除头像") }
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("昵称") },
                supportingText = { Text("${displayName.length}/80") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            Text("账号名 @${user.username} 不会改变", color = MaterialTheme.colorScheme.onSurfaceVariant)
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSave, enabled = !uiState.saving, modifier = Modifier.padding(top = 8.dp), shape = CircleShape) {
                Text(if (uiState.saving) "正在保存…" else "保存资料")
            }
        }
    }
}
