package com.aniglow.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.AnimeFilterOptions
import com.aniglow.app.data.remote.NetworkModule
import com.aniglow.app.data.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class AnimeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val anime: List<AnimeSummary> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val genre: String = "",
    val type: String = "",
    val country: String = "",
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val filterOptions: AnimeFilterOptions = AnimeFilterOptions(),
    val errorMessage: String? = null,
)

class AnimeViewModel : ViewModel() {
    private val repository = DiscoveryRepository(NetworkModule.discoveryApi)
    private val mutableUiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadFilterOptions()
        loadAnime()
    }

    fun updateQuery(value: String) {
        mutableUiState.value = mutableUiState.value.copy(query = value)
    }

    fun search() = loadAnime(page = 0)

    fun clearSearch() {
        mutableUiState.value = mutableUiState.value.copy(query = "")
        loadAnime(page = 0)
    }

    fun retry() = loadAnime(page = mutableUiState.value.currentPage)

    fun selectGenre(value: String) {
        mutableUiState.value = mutableUiState.value.copy(genre = value)
        loadAnime(page = 0)
    }

    fun selectType(value: String) {
        mutableUiState.value = mutableUiState.value.copy(type = value)
        loadAnime(page = 0)
    }

    fun selectCountry(value: String) {
        mutableUiState.value = mutableUiState.value.copy(country = value)
        loadAnime(page = 0)
    }

    fun selectYear(year: Int?) {
        mutableUiState.value = mutableUiState.value.copy(yearFrom = year, yearTo = year)
        loadAnime(page = 0)
    }

    fun resetFilters() {
        mutableUiState.value = mutableUiState.value.copy(
            query = "",
            genre = "",
            type = "",
            country = "",
            yearFrom = null,
            yearTo = null,
        )
        loadAnime(page = 0)
    }

    fun previousPage() {
        val state = mutableUiState.value
        if (state.currentPage > 0) loadAnime(page = state.currentPage - 1)
    }

    fun nextPage() {
        val state = mutableUiState.value
        if (state.currentPage + 1 < state.totalPages) loadAnime(page = state.currentPage + 1)
    }

    fun goToPage(page: Int) {
        if (page in 0 until mutableUiState.value.totalPages) loadAnime(page = page)
    }

    private fun loadAnime(page: Int = 0) {
        loadJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(loading = true, errorMessage = null)
        loadJob = viewModelScope.launch {
            val state = mutableUiState.value
            repository.loadAnime(
                keyword = state.query,
                genre = state.genre,
                type = state.type,
                country = state.country,
                yearFrom = state.yearFrom,
                yearTo = state.yearTo,
                page = page,
            )
                .onSuccess { page ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        anime = page.content,
                        totalElements = page.totalElements,
                        totalPages = page.totalPages,
                        currentPage = page.pageNumber,
                    )
                }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "番剧数据暂时不可用",
                    )
                }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            repository.loadAnimeFilterOptions().onSuccess { options ->
                mutableUiState.value = mutableUiState.value.copy(filterOptions = options)
            }
        }
    }
}
