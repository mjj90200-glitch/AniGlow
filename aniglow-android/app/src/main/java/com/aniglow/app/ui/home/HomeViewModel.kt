package com.aniglow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.PublicStats
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val stats: PublicStats = PublicStats(),
    val anime: List<AnimeSummary> = emptyList(),
    val errorMessage: String? = null,
)

class HomeViewModel : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            repository.loadHome()
                .onSuccess { (stats, anime) ->
                    mutableUiState.value = HomeUiState(
                        loading = false,
                        stats = stats,
                        anime = anime.content,
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "首页数据暂时不可用",
                    )
                }
        }
    }
}
