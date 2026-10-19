package com.aniglow.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AgentCharacter
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgentUiState(
    val loading: Boolean = true,
    val characters: List<AgentCharacter> = emptyList(),
    val errorMessage: String? = null,
)

class AgentViewModel : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            repository.loadCharacters()
                .onSuccess { characters ->
                    mutableUiState.value = AgentUiState(false, characters)
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "角色数据暂时不可用",
                    )
                }
        }
    }
}
