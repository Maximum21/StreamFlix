package com.asadraza.streamflix.feature.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator
import com.asadraza.streamflix.core.ui.text.toUiText
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private const val MILLIS_PER_SECOND = 1000f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PlayerEffect.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = state.movieTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(PlayerEvent.OnBackPressed) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.player_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val trailerKey = state.trailerKey
        val errorType = state.errorType
        when {
            state.isLoading -> LoadingIndicator(modifier = Modifier.padding(paddingValues))
            errorType != null -> ErrorMessage(
                message = errorType.toUiText().asString(),
                onRetry = { viewModel.onEvent(PlayerEvent.OnRetry) },
                modifier = Modifier.padding(paddingValues)
            )
            trailerKey == null -> NoTrailer(Modifier.padding(paddingValues))
            else -> YouTubeTrailer(
                videoId = trailerKey,
                startAtSeconds = state.resumeAtMillis / MILLIS_PER_SECOND,
                onPosition = { viewModel.onEvent(PlayerEvent.OnPositionChanged(it)) },
                onStopped = { viewModel.onEvent(PlayerEvent.OnPlaybackStopped(it)) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun NoTrailer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.player_no_trailer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Hosts the YouTube player view and ties it to the lifecycle: playback pauses when the screen
 * stops and the player is released when it leaves composition. The playback position survives
 * configuration changes.
 */
@Composable
private fun YouTubeTrailer(
    videoId: String,
    startAtSeconds: Float,
    onPosition: (Long) -> Unit,
    onStopped: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var resumeAtSeconds by rememberSaveable { mutableFloatStateOf(startAtSeconds) }
    var playbackFailed by remember { mutableStateOf(false) }

    val playerView = remember {
        YouTubePlayerView(context).apply {
            // Since YouTube started requiring an embedding origin, the player must be initialised with
            // explicit options: the library then uses this app's package as the origin. Without it
            // videos fail with "Error 152 - 4".
            enableAutomaticInitialization = false
            initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, resumeAtSeconds)
                    }

                    // Some videos cannot be embedded (the owner disabled it, or it is region-locked)
                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        playbackFailed = true
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        resumeAtSeconds = second
                        onPosition((second * MILLIS_PER_SECOND).toLong())
                    }
                },
                IFramePlayerOptions.Builder(context).controls(1).build()
            )
        }
    }

    DisposableEffect(playerView, lifecycle) {
        lifecycle.addObserver(playerView)
        onDispose {
            onStopped((resumeAtSeconds * MILLIS_PER_SECOND).toLong())
            lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (playbackFailed) {
            WatchOnYouTube(videoId = videoId)
        } else {
            AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Shown when the embedded player refuses a video: the same trailer can still be opened on YouTube. */
@Composable
private fun WatchOnYouTube(videoId: String) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.player_embed_blocked),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${Uri.encode(videoId)}"))
                )
            }
        ) {
            Text(stringResource(R.string.player_watch_on_youtube))
        }
    }
}
