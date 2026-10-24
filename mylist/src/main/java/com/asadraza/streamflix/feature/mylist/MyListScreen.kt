package com.asadraza.streamflix.feature.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.common.util.ImageUrl
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.components.MovieCard
import com.asadraza.streamflix.core.ui.text.toUiText
import com.asadraza.streamflix.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MyListEffect.NavigateToDetail -> onMovieClick(effect.movieId)
            }
        }
    }

    // The scaffold supplies the top bar and keeps the grid out from under the status bar
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_list_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        MyListContent(state = state, onEvent = viewModel::onEvent, modifier = Modifier.padding(padding))
    }
}

@Composable
internal fun MyListContent(state: MyListState, onEvent: (MyListEvent) -> Unit, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    // With items on screen a failed refresh keeps them, so the failure is shown in a snackbar
    val refreshError = state.errorType.takeIf { state.items.isNotEmpty() }?.toUiText()?.asString()
    LaunchedEffect(refreshError) { refreshError?.let { snackbarHostState.showSnackbar(it) } }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.items.isEmpty() -> LoadingIndicator()
            state.errorType != null && state.items.isEmpty() -> ErrorMessage(
                message = state.errorType.toUiText().asString(),
                onRetry = { onEvent(MyListEvent.OnRetry) }
            )
            state.items.isEmpty() -> Text(
                text = stringResource(R.string.my_list_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.Large)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GRID_MIN_CELL),
                contentPadding = PaddingValues(Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items, key = { it.movieId }) { item ->
                    MovieCard(
                        posterUrl = ImageUrl.poster(item.posterPath),
                        title = item.title,
                        onClick = { onEvent(MyListEvent.OnItemClick(item.movieId)) }
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private val GRID_MIN_CELL = 120.dp
