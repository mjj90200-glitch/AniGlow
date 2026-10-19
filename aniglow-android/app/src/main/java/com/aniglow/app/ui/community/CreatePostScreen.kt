package com.aniglow.app.ui.community

import android.net.Uri
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.common.GlassBackTopBar
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.WarmBackground
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun CreatePostRoute(
    user: AuthUser,
    onBack: () -> Unit,
    onPublished: (Long) -> Unit,
    viewModel: CreatePostViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
        onResult = viewModel::selectImages,
    )
    CreatePostScreen(
        uiState = uiState,
        title = title,
        content = content,
        onTitleChange = { title = it.take(120) },
        onContentChange = { content = it.take(5000) },
        onCommunitySelect = viewModel::selectCommunity,
        onPickImages = {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemoveImage = viewModel::removeImage,
        onBack = onBack,
        onPublish = { viewModel.publish(title, content, user) { onPublished(it.id) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostScreen(
    uiState: CreatePostUiState,
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCommunitySelect: (String) -> Unit,
    onPickImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("发布帖子", onBack, "NEW POST")
        },
    ) { padding ->
        if (uiState.loading) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) { LoadingContent("正在加载社区") }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).background(
                    Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
                ).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = FireflyGreen.copy(alpha = 0.09f)),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("NEW POST", color = FireflyGreen, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                        Text("分享此刻的想法", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                        Text("安利、提问、考据或一张喜欢的图片，都可以成为新的萤火。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                    }
                }
                Text("选择社区", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.communities, key = { it.id }) { community ->
                        val selected = uiState.selectedSlug == community.slug
                        Card(
                            modifier = Modifier.clickable { onCommunitySelect(community.slug) },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) FireflyGreen else Color.White,
                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            ),
                        ) { Text(community.name, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.Bold) }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("标题") },
                    supportingText = { Text("${title.length}/120") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("正文") },
                    minLines = 8,
                    maxLines = 16,
                    supportingText = { Text("${content.length}/5000") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                )
                Text("帖子图片", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                if (uiState.selectedImages.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.selectedImages, key = { it.toString() }) { uri ->
                            Box(Modifier.size(112.dp).clip(RoundedCornerShape(14.dp))) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "已选择的帖子图片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Card(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).clickable { onRemoveImage(uri) },
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.68f)),
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text("移除", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = onPickImages,
                    enabled = !uiState.publishing && uiState.selectedImages.size < 9,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FireflyGreen),
                ) {
                    Text(if (uiState.selectedImages.isEmpty()) "从相册选择图片" else "重新选择（${uiState.selectedImages.size}/9）")
                }
                Text("支持 JPG、PNG、GIF、WebP；最多 9 张，单张不超过 10MB。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = onPublish,
                    enabled = !uiState.publishing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) { Text(if (uiState.publishing) "正在上传并发布…" else "发布") }
            }
        }
    }
}
