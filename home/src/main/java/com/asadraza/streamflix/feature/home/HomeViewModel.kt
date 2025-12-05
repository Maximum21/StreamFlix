package com.asadraza.streamflix.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.getUserMessage
import com.asadraza.streamflix.core.common.result.isRetryable
import com.asadraza.streamflix.core.domain.usecase.home.GetMoviesByCategoryUseCase
import com.asadraza.streamflix.core.domain.usecase.home.RefreshMoviesUseCase
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel with enhanced Result handling
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMoviesByCategoryUseCase: GetMoviesByCategoryUseCase,
    private val refreshMoviesUseCase: RefreshMoviesUseCase
) : ViewModel() {

    // UI State
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Side Effects (one-time events)
    private val _effect = Channel<HomeEffect>()
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        // Load all categories on init
        loadAllCategories()
    }

    /**
     * Handle user events
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnMovieClick -> handleMovieClick(event.movieId)
            is HomeEvent.OnCategorySelect -> handleCategorySelect(event.category)
            is HomeEvent.OnRefresh -> handleRefresh()
            is HomeEvent.OnRetryConnection -> handleRetryConnection()
            is HomeEvent.OnSearchClick -> handleSearchClick()
        }
    }

    /**
     * Load movies for all categories
     */
    private fun loadAllCategories() {
        MovieCategory.getAll().forEach { category ->
            loadMoviesForCategory(category)
        }
    }

    /**
     * Load movies for specific category
     */
    private fun loadMoviesForCategory(category: MovieCategory) {
        viewModelScope.launch {
            getMoviesByCategoryUseCase(category).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        updateCategoryState(category) {
                            copy(
                                isLoading = true,
                                error = null,
                                // Keep showing old movies while loading
                                movies = result.data ?: movies
                            )
                        }
                    }

                    is Result.Success -> {
                        updateCategoryState(category) {
                            copy(
                                movies = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        _state.update { it.copy(isOffline = false) }
                    }

                    is Result.Error -> {
                        // Type-safe error handling
                        handleError(category, result)
                    }
                }
            }
        }
    }

    /**
     * Handle errors with type-specific logic
     *
     * DIFFERENT HANDLING FOR DIFFERENT ERROR TYPES:
     * - Network errors → show offline banner, keep cached data
     * - Database errors → show warning, don't retry
     * - Auth errors → navigate to login
     * - Unknown errors → show generic error
     */
    private fun handleError(category: MovieCategory, error: Result.Error<*>) {
        updateCategoryState(category) {
            copy(
                isLoading = false,
                // Keep showing cached movies even on error
                movies = error.data as? List<Movie> ?: movies,
                error = error.type.getUserMessage()
            )
        }
        val message = error.type.getUserMessage()
        when (error.type) {
            is ErrorType.Network -> {
                // Show offline banner
                _state.update { it.copy(isOffline = true) }

                // If retryable, offer to retry
                if (error.type.isRetryable()) {
                    viewModelScope.launch {
                        _effect.send(
                            HomeEffect.ShowToast(message)
                        )
                    }
                }
            }

            is ErrorType.Database -> {
                println(message)
            }

            else -> {
                viewModelScope.launch {
                    _effect.send(
                        HomeEffect.ShowToast(message)
                    )
                }
            }
        }
    }

    /**
     * Handle movie click
     */
    private fun handleMovieClick(movieId: String) {
        viewModelScope.launch {
            _effect.send(HomeEffect.NavigateToDetail(movieId))
        }
    }

    /**
     * Handle category selection
     */
    private fun handleCategorySelect(category: MovieCategory) {
        _state.update { it.copy(selectedCategory = category) }
    }

    /**
     * Handle refresh (pull-to-refresh)
     */
    private fun handleRefresh() {
        viewModelScope.launch {
            val category = _state.value.selectedCategory
            try {
                refreshMoviesUseCase(category)
                // Reload category after refresh
                loadMoviesForCategory(category)
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowToast("Failed to refresh: ${e.message}")
                )
            }
        }
    }

    /**
     * Handle retry connection
     */
    private fun handleRetryConnection() {
        _state.update { it.copy(isOffline = false) }
        loadAllCategories()
    }

    /**
     * Handle retry connection
     */
    private fun handleSearchClick() {
        viewModelScope.launch {
            _effect.send(
                HomeEffect.NavigateToSearch
            )
        }
    }

    /**
     * Helper to update category state
     */
    private fun updateCategoryState(
        category: MovieCategory,
        update: HomeState.CategoryState.() -> HomeState.CategoryState
    ) {
        _state.update { currentState ->
            val currentCategoryState = currentState.movieCategories[category]
                ?: HomeState.CategoryState()
            val newCategoryState = currentCategoryState.update()
            currentState.copy(
                movieCategories = currentState.movieCategories + (category to newCategoryState)
            )
        }
    }
}