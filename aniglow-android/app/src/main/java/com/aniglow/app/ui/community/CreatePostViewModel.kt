package com.aniglow.app.ui.community

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.model.CommunityInfo
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val loading: Boolean = true,
    val publishing: Boolean = false,
    val communities: List<CommunityInfo> = emptyList(),
    val selectedSlug: String? = null,
    val selectedImages: List<Uri> = emptyList(),
    val errorMessage: String? = null,
)

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommunityRepository(NetworkModule.communityApi)
    private val mutableUiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = mutableUiState.asStateFlow()

    init { loadCommunities() }

    fun selectCommunity(slug: String) {
        mutableUiState.value = mutableUiState.value.copy(selectedSlug = slug, errorMessage = null)
    }

    fun selectImages(uris: List<Uri>) {
        mutableUiState.value = mutableUiState.value.copy(
            selectedImages = uris.distinct().take(9),
            errorMessage = if (uris.size > 9) "最多选择 9 张图片" else null,
        )
    }

    fun removeImage(uri: Uri) {
        mutableUiState.value = mutableUiState.value.copy(
            selectedImages = mutableUiState.value.selectedImages - uri,
            errorMessage = null,
        )
    }

    fun publish(title: String, content: String, user: AuthUser, onSuccess: (CommunityPost) -> Unit) {
        val slug = mutableUiState.value.selectedSlug
        when {
            slug == null -> mutableUiState.value = mutableUiState.value.copy(errorMessage = "请选择社区")
            title.isBlank() -> mutableUiState.value = mutableUiState.value.copy(errorMessage = "请输入标题")
            content.isBlank() -> mutableUiState.value = mutableUiState.value.copy(errorMessage = "请输入正文")
            else -> {
                mutableUiState.value = mutableUiState.value.copy(publishing = true, errorMessage = null)
                viewModelScope.launch {
                    val selectedImages = mutableUiState.value.selectedImages
                    val uploadResult = repository.uploadImages(getApplication<Application>().contentResolver, selectedImages)
                    uploadResult.fold(
                        onSuccess = { uploadedImages -> repository.createPost(slug, title, content, uploadedImages, user) },
                        onFailure = { Result.failure(it) },
                    )
                        .onSuccess(onSuccess)
                        .onFailure { error ->
                            mutableUiState.value = mutableUiState.value.copy(
                                publishing = false,
                                errorMessage = error.message ?: "发布失败，请稍后重试",
                            )
                        }
                }
            }
        }
    }

    private fun loadCommunities() {
        viewModelScope.launch {
            repository.loadCommunities()
                .onSuccess { communities ->
                    mutableUiState.value = CreatePostUiState(
                        loading = false,
                        communities = communities,
                        selectedSlug = communities.firstOrNull()?.slug,
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = CreatePostUiState(false, errorMessage = error.message ?: "社区列表加载失败")
                }
        }
    }
}
