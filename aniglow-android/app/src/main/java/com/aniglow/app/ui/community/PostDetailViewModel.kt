package com.aniglow.app.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.model.CommunityReply
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val loading: Boolean = true,
    val post: CommunityPost? = null,
    val replies: List<CommunityReply> = emptyList(),
    val errorMessage: String? = null,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null,
)

class PostDetailViewModel(private val postId: Long) : ViewModel() {
    private val repository = CommunityRepository(NetworkModule.communityApi)
    private val mutableUiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            repository.loadPost(postId)
                .onSuccess { (post, replies) -> mutableUiState.value = PostDetailUiState(false, post, replies) }
                .onFailure { mutableUiState.value = PostDetailUiState(false, errorMessage = it.message ?: "加载失败") }
        }
    }

    fun like() {
        if (mutableUiState.value.actionLoading) return
        mutableUiState.value = mutableUiState.value.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            repository.likePost(postId)
                .onSuccess { post ->
                    mutableUiState.value = mutableUiState.value.copy(post = post, actionLoading = false)
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(actionLoading = false, actionMessage = error.message ?: "操作失败")
                }
        }
    }

    fun reply(content: String, user: AuthUser) {
        if (content.isBlank() || mutableUiState.value.actionLoading) return
        mutableUiState.value = mutableUiState.value.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            repository.createReply(postId, content, user)
                .onSuccess { reply ->
                    mutableUiState.value = mutableUiState.value.copy(
                        replies = mutableUiState.value.replies + reply,
                        post = mutableUiState.value.post?.let { it.copy(replyCount = it.replyCount + 1) },
                        actionLoading = false,
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(actionLoading = false, actionMessage = error.message ?: "回复失败")
                }
        }
    }

    class Factory(private val postId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PostDetailViewModel(postId) as T
    }
}
