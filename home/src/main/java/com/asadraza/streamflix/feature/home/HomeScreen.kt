package com.asadraza.streamflix.feature.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.ui.components.OfflineBanner
import com.asadraza.streamflix.core.ui.theme.Red
import com.asadraza.streamflix.feature.home.components.CategoryTabs
import com.asadraza.streamflix.feature.home.components.PaginatedMovieGrid
import kotlinx.coroutines.flow.collectLatest

/**
 * Home Screen with Paging 3 support
 *
 * Features:
 * - Paginated movie lists with infinite scrolling
 * - Category switching
 * - Offline support with cached data
 * - Pull-to-refresh capability
 *
 * Uses PaginatedMovieGrid for efficient memory management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Collect state
    val state by viewModel.state.collectAsStateWithLifecycle()
    val paginatedMovies = viewModel.paginatedMovies.collectAsLazyPagingItems()
    val context = LocalContext.current

    Log.e("testingsizes","==total count = home count ")
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetail -> onMovieClick(effect.movieId)
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                HomeEffect.NavigateToSearch -> onSearchClick()
                HomeEffect.NavigateToLogin -> TODO()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StreamFlix") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.OnSearchClick) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Red,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline banner
            if (state.isOffline) {
                OfflineBanner(
                    onRetry = { viewModel.onEvent(HomeEvent.OnRetryConnection) }
                )
            }

//            Log.e("testingsizes","==total count = main count ")
            // Content with pagination
            HomeContent(
                state = state,
                paginatedMovies = paginatedMovies,
                onEvent = viewModel::onEvent
            )
        }
    }
}

/**
 * Home screen content with paginated movie grid
 */
@Composable
private fun HomeContent(
    state: HomeState,
    paginatedMovies: LazyPagingItems<Movie>,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Category tabs
        CategoryTabs(
            categories = state.movieCategories.keys.toList(),
            selectedCategory = state.selectedCategory,
            onCategorySelect = { category ->
                onEvent(HomeEvent.OnCategorySelect(category))
            }
        )

        // Paginated movie grid for selected category
        PaginatedMovieGrid(
            movies = paginatedMovies,
            onMovieClick = { movieId ->
                onEvent(HomeEvent.OnMovieClick(movieId))
            },
            emptyMessage = "No movies available"
        )
    }
}