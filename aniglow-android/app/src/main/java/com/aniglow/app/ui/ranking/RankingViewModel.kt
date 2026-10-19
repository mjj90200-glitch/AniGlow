package com.aniglow.app.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RankingUiState(
    val loading: Boolean = true,
    val fireflyMode: Boolean = false,
    val items: List<AnimeSummary> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val votingAnimeId: Long? = null,
)

class RankingViewModel : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun changeMode(firefly: Boolean) {
        if (mutableUiState.value.fireflyMode == firefly) return
        mutableUiState.value = mutableUiState.value.copy(fireflyMode = firefly)
        refresh()
    }

    fun refresh() {
        val firefly = mutableUiState.value.fireflyMode
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            repository.loadRanking(firefly)
                .onSuccess { mutableUiState.value = mutableUiState.value.copy(loading = false, items = it) }
                .onFailure { mutableUiState.value = mutableUiState.value.copy(loading = false, errorMessage = it.message ?: "加载失败") }
        }
    }

    fun vote(animeId: Long) {
        if (mutableUiState.value.votingAnimeId != null) return
        mutableUiState.value = mutableUiState.value.copy(votingAnimeId = animeId, actionMessage = null)
        viewModelScope.launch {
            repository.voteFirefly(animeId)
                .onSuccess { result ->
                    DailyStore.consumeTicket()
                    mutableUiState.value = mutableUiState.value.copy(
                        items = mutableUiState.value.items.map { anime ->
                            if (anime.id == animeId) anime.copy(fireflyVoteCount = result.fireflyVoteCount) else anime
                        },
                        actionMessage = "萤火票已经送达",
                        votingAnimeId = null,
                    )
                }
                .onFailure {
                    mutableUiState.value = mutableUiState.value.copy(
                        actionMessage = it.message ?: "投票失败",
                        votingAnimeId = null,
                    )
                }
        }
    }

    fun showMessage(message: String) {
        mutableUiState.value = mutableUiState.value.copy(actionMessage = message)
    }
}
