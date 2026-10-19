package com.aniglow.app.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityUiState(
    val loading: Boolean = true,
    val posts: List<CommunityPost> = emptyList(),
    val errorMessage: String? = null,
)

class CommunityViewModel : ViewModel() {
    private val repository = CommunityRepository(NetworkModule.communityApi)
    private val mutableUiState = MutableStateFlow(CommunityUiState())

    val uiState: StateFlow<CommunityUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (mutableUiState.value.loading && mutableUiState.value.posts.isNotEmpty()) return

        mutableUiState.value = mutableUiState.value.copy(
            loading = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            repository.loadAllPosts()
                .onSuccess { page ->
                    mutableUiState.value = CommunityUiState(
                        loading = false,
                        posts = page.items,
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "无法连接社区，请稍后重试",
                    )
                }
        }
    }
}
