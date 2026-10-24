package com.asadraza.streamflix.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.asadraza.streamflix.core.model.movie.Movie
import com.asadraza.streamflix.core.model.movie.MovieCategory
import com.asadraza.streamflix.core.ui.R as UiR
import com.asadraza.streamflix.core.ui.components.OfflineBanner
import com.asadraza.streamflix.core.ui.theme.Red
import com.asadraza.streamflix.feature.home.components.CategoryRow
import com.asadraza.streamflix.feature.home.components.ContinueWatchingRow
import com.asadraza.streamflix.feature.home.components.HeroBanner
import com.asadraza.streamflix.feature.home.components.MyListRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trending = viewModel.categoryFeeds.getValue(MovieCategory.Trending).collectAsLazyPagingItems()
    var refreshTick by rememberSaveable { mutableIntStateOf(0) }
    val feeds = remember(viewModel) { viewModel.categoryFeeds.entries.toList() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetail -> onMovieClick(effect.movieId)
                is HomeEffect.NavigateToPlayer -> onPlayClick(effect.movieId)
                HomeEffect.NavigateToSearch -> onSearchClick()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.brand_name)) },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.OnSearchClick) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.home_search))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Red, titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = trending.loadState.refresh is LoadState.Loading && trending.itemCount > 0,
            onRefresh = { refreshTick++ },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // The mediator failing while rows are on screen means the cache is being shown offline
                if (trending.loadState.mediator?.refresh is LoadState.Error && trending.itemCount > 0) {
                    item { OfflineBanner(onRetry = { trending.retry() }) }
                }

                heroRow(trending = trending, showHero = state.showHeroBanner, onEvent = viewModel::onEvent)
                libraryRows(state = state, onEvent = viewModel::onEvent)

                items(feeds, key = { it.key.id }) { (category, feed) ->
                    CategoryRow(
                        title = stringResource(category.titleRes()),
                        feed = feed,
                        refreshTick = refreshTick,
                        onMovieClick = { viewModel.onEvent(HomeEvent.OnMovieClick(it)) }
                    )
                }
            }
        }
    }
}

@StringRes
private fun MovieCategory.titleRes(): Int = when (this) {
    MovieCategory.Trending -> R.string.category_trending
    MovieCategory.TopRated -> R.string.category_top_rated
    MovieCategory.Popular -> R.string.category_popular
    MovieCategory.Upcoming -> R.string.category_upcoming
    MovieCategory.NowPlaying -> R.string.category_now_playing
}

private fun LazyListScope.heroRow(trending: LazyPagingItems<Movie>, showHero: Boolean, onEvent: (HomeEvent) -> Unit) {
    // peek throws for an index outside the loaded items, so an empty first load must be ruled out first
    val featured = if (showHero && trending.itemCount > 0) trending.peek(0) else null
    if (featured != null) {
        item(key = "hero") {
            HeroBanner(
                movie = featured,
                onPlay = { onEvent(HomeEvent.OnPlayClick(featured.id)) },
                onDetails = { onEvent(HomeEvent.OnMovieClick(featured.id)) }
            )
        }
    }
}

private fun LazyListScope.libraryRows(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    if (state.continueWatching.isNotEmpty()) {
        item(key = "continue") {
            ContinueWatchingRow(
                items = state.continueWatching,
                onPlay = { onEvent(HomeEvent.OnPlayClick(it)) },
                onRemove = { onEvent(HomeEvent.OnRemoveContinueWatching(it)) }
            )
        }
    }
    if (state.myList.isNotEmpty()) {
        item(key = "my_list") {
            MyListRow(items = state.myList, onMovieClick = { onEvent(HomeEvent.OnMovieClick(it)) })
        }
    }
}
