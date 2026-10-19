package com.aniglow.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyPostsUiState(
    val loading: Boolean = true,
    val posts: List<CommunityPost> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

class MyPostsViewModel : ViewModel() {
    private val repository = CommunityRepository(NetworkModule.communityApi)
    private val mutableUiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            repository.loadMyPosts()
                .onSuccess { mutableUiState.value = MyPostsUiState(false, posts = it) }
                .onFailure { mutableUiState.value = MyPostsUiState(false, errorMessage = it.message ?: "加载失败") }
        }
    }

    fun delete(postId: Long) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess {
                    mutableUiState.value = mutableUiState.value.copy(
                        posts = mutableUiState.value.posts.filterNot { it.id == postId },
                        actionMessage = "帖子已删除",
                    )
                }
                .onFailure { mutableUiState.value = mutableUiState.value.copy(actionMessage = it.message ?: "删除失败") }
        }
    }
}
