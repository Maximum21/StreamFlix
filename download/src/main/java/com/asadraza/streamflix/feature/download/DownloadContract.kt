package com.asadraza.streamflix.feature.download

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.Movie

data class DownloadsState(
    val downloads: List<Download> = emptyList(),
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val totalStorageUsed: Long = 0L,
    val availableStorage: Long = 0L,
    val showDeleteConfirmation: Boolean = false,
    val downloadToDelete: Download? = null,
    val showDeleteAllConfirmation: Boolean = false,
    val selectedQuality: DownloadQuality = DownloadQuality.MEDIUM,
    val isSelectionMode: Boolean = false,
    val selectedDownloads: Set<String> = emptySet()
)

sealed class DownloadsEvent {
    data object OnLoadDownloads : DownloadsEvent()
    data class OnDownloadClick(val download: Download) : DownloadsEvent()
    data class OnDownloadLongClick(val download: Download) : DownloadsEvent()
    data class OnDeleteDownload(val download: Download) : DownloadsEvent()
    data object OnDeleteConfirm : DownloadsEvent()
    data object OnDeleteCancel : DownloadsEvent()
    data object OnDeleteAllClick : DownloadsEvent()
    data object OnDeleteAllConfirm : DownloadsEvent()
    data object OnDeleteAllCancel : DownloadsEvent()
    data object OnDeleteSelectedClick : DownloadsEvent()
    data class OnPauseDownload(val downloadId: String) : DownloadsEvent()
    data class OnResumeDownload(val downloadId: String) : DownloadsEvent()
    data class OnCancelDownload(val downloadId: String) : DownloadsEvent()
    data class OnRetryDownload(val downloadId: String) : DownloadsEvent()
    data class OnSelectDownload(val downloadId: String) : DownloadsEvent()
    data object OnExitSelectionMode : DownloadsEvent()
    data object OnSelectAll : DownloadsEvent()
    data object OnDeselectAll : DownloadsEvent()
    data object OnErrorDismiss : DownloadsEvent()
    data object OnRetry : DownloadsEvent()
}

sealed class DownloadsEffect {
    data class NavigateToPlayer(val movieId: String) : DownloadsEffect()
    data class ShowToast(val message: String) : DownloadsEffect()
    data object ShowStorageFull : DownloadsEffect()
}

data class Download(
    val id: String,
    val movie: Movie,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val quality: DownloadQuality,
    val sizeBytes: Long,
    val downloadedBytes: Long = 0L,
    val downloadPath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DownloadQuality(val label: String, val sizeMb: Int) {
    LOW("Low (480p)", 300),
    MEDIUM("Medium (720p)", 800),
    HIGH("High (1080p)", 1500)
}