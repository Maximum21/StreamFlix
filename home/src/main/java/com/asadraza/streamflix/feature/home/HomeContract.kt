package com.asadraza.streamflix.feature.home

import com.asadraza.streamflix.core.model.library.MyListItem
import com.asadraza.streamflix.core.model.library.WatchedMovie

/**
 * The category rows are paged straight from Room, so only the profile-specific rows live in state.
 */
data class HomeState(
    val showHeroBanner: Boolean = true,
    val continueWatching: List<WatchedMovie> = emptyList(),
    val myList: List<MyListItem> = emptyList()
)

sealed class HomeEvent {
    data class OnMovieClick(val movieId: String) : HomeEvent()

    data class OnPlayClick(val movieId: String) : HomeEvent()

    data class OnRemoveContinueWatching(val movieId: String) : HomeEvent()

    data object OnSearchClick : HomeEvent()
}

sealed class HomeEffect {
    data class NavigateToDetail(val movieId: String) : HomeEffect()

    data class NavigateToPlayer(val movieId: String) : HomeEffect()

    data object NavigateToSearch : HomeEffect()
}
