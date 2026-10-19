package com.aniglow.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.AnimeRating
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnimeDetailUiState(
    val loading: Boolean = true,
    val anime: AnimeSummary? = null,
    val reviews: List<AnimeRating> = emptyList(),
    val errorMessage: String? = null,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null,
)

class AnimeDetailViewModel(private val animeId: Long) : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            val detailResult = repository.loadAnimeDetail(animeId)
            if (detailResult.isFailure) {
                mutableUiState.value = AnimeDetailUiState(false, errorMessage = detailResult.exceptionOrNull()?.message ?: "加载失败")
                return@launch
            }
            val reviews = repository.loadReviews(animeId).getOrDefault(emptyList())
            mutableUiState.value = AnimeDetailUiState(
                loading = false,
                anime = detailResult.getOrNull(),
                reviews = reviews,
            )
        }
    }

    fun submitRating(score: Double, review: String, user: AuthUser) {
        if (mutableUiState.value.actionLoading) return
        mutableUiState.value = mutableUiState.value.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            repository.submitRating(animeId, score, review, user)
                .onSuccess { rating ->
                    if (review.isNotBlank()) DailyStore.completeTask(DailyStore.PUBLISH_COMMENT)
                    mutableUiState.value = mutableUiState.value.copy(
                        reviews = listOf(rating) + mutableUiState.value.reviews.filterNot { it.id == rating.id || it.userId == rating.userId },
                        actionLoading = false,
                        actionMessage = "评分已保存",
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(actionLoading = false, actionMessage = error.message ?: "评分失败")
                }
        }
    }

    fun vote() {
        if (mutableUiState.value.actionLoading) return
        mutableUiState.value = mutableUiState.value.copy(actionLoading = true, actionMessage = null)
        viewModelScope.launch {
            repository.voteFirefly(animeId)
                .onSuccess { vote ->
                    DailyStore.consumeTicket()
                    mutableUiState.value = mutableUiState.value.copy(
                        anime = mutableUiState.value.anime?.copy(fireflyVoteCount = vote.fireflyVoteCount),
                        actionLoading = false,
                        actionMessage = "已投出一张萤火票",
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(actionLoading = false, actionMessage = error.message ?: "投票失败")
                }
        }
    }

    fun showMessage(message: String) {
        mutableUiState.value = mutableUiState.value.copy(actionMessage = message)
    }

    class Factory(private val animeId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AnimeDetailViewModel(animeId) as T
    }
}
