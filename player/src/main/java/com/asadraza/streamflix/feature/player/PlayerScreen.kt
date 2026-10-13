package com.asadraza.streamflix.feature.player

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.asadraza.streamflix.feature.player.components.BottomControls
import com.asadraza.streamflix.feature.player.components.ErrorOverlay
import com.asadraza.streamflix.feature.player.components.PlayerControls
import com.asadraza.streamflix.feature.player.components.QualitySelector
import com.asadraza.streamflix.feature.player.components.SpeedSelector
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PlayerEffect.ShowError -> {
                    // Handled by error overlay
                }

                is PlayerEffect.NavigateBack -> onBack()
                is PlayerEffect.ShowQualitySelector -> {
                    // Handled by state
                }

                is PlayerEffect.ShowSpeedSelector -> {
                    // Handled by state
                }

                is PlayerEffect.UpdateProgress -> {
                    // Handled by ExoPlayer listener
                }
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> viewModel.onEvent(PlayerEvent.OnBuffering)
                        Player.STATE_READY -> viewModel.onEvent(PlayerEvent.OnReady)
                        Player.STATE_ENDED -> viewModel.onEvent(PlayerEvent.OnVideoEnded)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        viewModel.onEvent(PlayerEvent.OnPlay)
                    } else {
                        viewModel.onEvent(PlayerEvent.OnPause)
                    }
                }
            })
        }
    }

    // Load video when URL is available
    LaunchedEffect(state.streamUrl) {
        if (state.streamUrl.isNotEmpty() && state.videoType != "youtube") {
            val mediaItem = MediaItem.fromUri(state.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Update playback state
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Update playback speed
    LaunchedEffect(state.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(state.playbackSpeed)
    }

    // Progress tracking
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            viewModel.updateProgress(
                position = exoPlayer.currentPosition,
                buffered = exoPlayer.bufferedPosition,
                duration = exoPlayer.duration.coerceAtLeast(0)
            )
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.movieTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(PlayerEvent.OnBackPressed) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.videoType.lowercase() == "youtube") {
            YouTubePlayerEmbed(
                { state.streamUrl },
                { state.isLoading },
                Modifier.padding(paddingValues)
            )
        } else {
            ExoPlayerComposable(state, exoPlayer, Modifier.padding(paddingValues), viewModel)
        }
    }


}

@Composable
fun ExoPlayerComposable(
    state: PlayerState, exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier, viewModel: PlayerViewModel
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    viewModel.onEvent(PlayerEvent.OnScreenTap)
                }
        )

        Log.e("loadingindicataor","==1==${state.isLoading}")
        // Loading Indicator
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Player Controls (Play/Pause, Seek)
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PlayerControls(
                isPlaying = state.isPlaying,
                currentPosition = state.currentPosition,
                duration = state.duration,
                bufferedPosition = state.bufferedPosition,
                onPlayPauseClick = {
                    viewModel.onEvent(PlayerEvent.OnPlayPauseClick)
                },
                onSeek = { position ->
                    viewModel.onEvent(PlayerEvent.OnSeek(position))
                    exoPlayer.seekTo(position)
                },
                onRewind = {
                    viewModel.onEvent(PlayerEvent.OnRewind)
                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                },
                onForward = {
                    viewModel.onEvent(PlayerEvent.OnForward)
                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                }
            )
        }

        // Bottom Controls (Quality, Speed, Fullscreen)
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            BottomControls(
                currentQuality = state.currentQuality,
                playbackSpeed = state.playbackSpeed,
                isFullscreen = state.isFullscreen,
                onQualityClick = { viewModel.onEvent(PlayerEvent.OnQualityClick) },
                onSpeedClick = { viewModel.onEvent(PlayerEvent.OnSpeedClick) },
                onFullscreenClick = { viewModel.onEvent(PlayerEvent.OnFullscreenToggle) }
            )
        }

        // Quality Selector Bottom Sheet
        if (state.showQualitySelector) {
            QualitySelector(
                availableQualities = state.availableQualities,
                currentQuality = state.currentQuality,
                onQualitySelected = { quality ->
                    viewModel.onEvent(PlayerEvent.OnQualitySelected(quality))
                },
                onDismiss = { viewModel.onEvent(PlayerEvent.OnQualityClick) }
            )
        }

        // Speed Selector Bottom Sheet
        if (state.showSpeedSelector) {
            SpeedSelector(
                currentSpeed = state.playbackSpeed,
                onSpeedSelected = { speed ->
                    viewModel.onEvent(PlayerEvent.OnSpeedSelected(speed))
                },
                onDismiss = { viewModel.onEvent(PlayerEvent.OnSpeedClick) }
            )
        }

        // Error Display
        state.errorType?.let { errorType ->
            ErrorOverlay(
                errorType = errorType,
                onRetry = { viewModel.onEvent(PlayerEvent.OnRetry) },
                onDismiss = { viewModel.onEvent(PlayerEvent.OnErrorDismiss) }
            )
        }
    }
}

@Composable
fun YouTubePlayerEmbed(
    videoId: () -> String,
    isLoading: () -> Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId(), 0f)
                        }
                    })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .padding(16.dp)
        )

        Log.e("loadingindicataor","==2==${isLoading()}")

        // Loading Indicator
        if (isLoading()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
