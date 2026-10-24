package com.asadraza.streamflix.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.model.movie.Genre
import com.asadraza.streamflix.core.ui.components.PaginatedMovieGrid

/**
 * Search with infinite scrolling results, a genre filter and recent searches. Debouncing lives in
 * the ViewModel; the screen only renders state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    // A different query or genre selection is a different list, so start it from the top
    LaunchedEffect(state.query.trim(), state.selectedGenres) { gridState.scrollToItem(0) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetail -> onMovieClick(effect.movieId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SearchField(
                query = state.query,
                onQueryChange = { viewModel.onEvent(SearchEvent.OnQueryChange(it)) },
                onClear = {
                    viewModel.onEvent(SearchEvent.OnClearSearch)
                    focusManager.clearFocus()
                }
            )

            GenreChips(
                selected = state.selectedGenres,
                onToggle = { viewModel.onEvent(SearchEvent.OnGenreToggle(it)) }
            )

            if (state.query.isBlank() && state.selectedGenres.isEmpty() && state.searchHistory.isNotEmpty()) {
                SearchHistory(
                    history = state.searchHistory,
                    onClick = { viewModel.onEvent(SearchEvent.OnHistoryItemClick(it)) },
                    onRemove = { viewModel.onEvent(SearchEvent.OnHistoryItemRemove(it)) },
                    onClear = { viewModel.onEvent(SearchEvent.OnClearHistory) }
                )
            } else {
                PaginatedMovieGrid(
                    movies = searchResults,
                    gridState = gridState,
                    onMovieClick = { viewModel.onEvent(SearchEvent.OnMovieClick(it)) },
                    emptyMessage = when {
                        state.selectedGenres.isNotEmpty() -> stringResource(R.string.search_no_genre_results)
                        state.query.trim().length >= Constants.SEARCH_MIN_LENGTH -> stringResource(
                            R.string.search_no_results,
                            state.query
                        )
                        else -> stringResource(R.string.search_min_chars, Constants.SEARCH_MIN_LENGTH)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(R.string.search_icon_description)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
internal fun GenreChips(selected: Set<Genre>, onToggle: (Genre) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Genre.ALL_GENRES, key = { it.id }) { genre ->
            FilterChip(
                selected = genre in selected,
                onClick = { onToggle(genre) },
                label = { Text(genre.name) }
            )
        }
    }
}

@Composable
internal fun SearchHistory(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.search_recent),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onClear) { Text(stringResource(R.string.search_clear_history)) }
            }
        }
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(query) }
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Text(text = query, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                IconButton(onClick = { onRemove(query) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_remove_history_item, query)
                    )
                }
            }
        }
    }
}
