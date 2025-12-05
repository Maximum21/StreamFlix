package com.asadraza.streamflix.feature.player

import com.asadraza.streamflix.core.common.result.ErrorType

data class PlayerState(
    val movieId: String = "",
    val videoType: String = "youtube",
    val movieTitle: String = "",
    val streamUrl: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val showControls: Boolean = true,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val isFullscreen: Boolean = true,
    val currentQuality: VideoQuality = VideoQuality.Auto,
    val availableQualities: List<VideoQuality> = listOf(
        VideoQuality.Auto,
        VideoQuality.SD,
        VideoQuality.HD,
        VideoQuality.FullHD,
        VideoQuality.UHD
    ),
    val playbackSpeed: Float = 1.0f,
    val showQualitySelector: Boolean = false,
    val showSpeedSelector: Boolean = false,
    val errorType: ErrorType? = null
)

sealed class PlayerEvent {
    data object OnPlayPauseClick : PlayerEvent()
    data object OnPlay : PlayerEvent()
    data object OnPause : PlayerEvent()
    data class OnSeek(val position: Long) : PlayerEvent()
    data object OnRewind : PlayerEvent()
    data object OnForward : PlayerEvent()
    data object OnScreenTap : PlayerEvent()
    data object OnFullscreenToggle : PlayerEvent()
    data object OnQualityClick : PlayerEvent()
    data class OnQualitySelected(val quality: VideoQuality) : PlayerEvent()
    data object OnSpeedClick : PlayerEvent()
    data class OnSpeedSelected(val speed: Float) : PlayerEvent()
    data object OnBackPressed : PlayerEvent()
    data object OnBuffering : PlayerEvent()
    data object OnReady : PlayerEvent()
    data object OnVideoEnded : PlayerEvent()
    data object OnRetry : PlayerEvent()
    data object OnErrorDismiss : PlayerEvent()
}

sealed class PlayerEffect {
    data class ShowError(val errorType: ErrorType) : PlayerEffect()
    data object NavigateBack : PlayerEffect()
    data object ShowQualitySelector : PlayerEffect()
    data object ShowSpeedSelector : PlayerEffect()
    data class UpdateProgress(val position: Long, val buffered: Long) : PlayerEffect()
}

sealed class VideoQuality(val height: Int) {
    data object Auto : VideoQuality(0)
    data object SD : VideoQuality(480)
    data object HD : VideoQuality(720)
    data object FullHD : VideoQuality(1080)
    data object UHD : VideoQuality(2160)
}