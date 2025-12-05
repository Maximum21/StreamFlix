package com.asadraza.streamflix.feature.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.domain.usecase.downloads.GetDownloadsUseCase
import com.asadraza.streamflix.domain.usecase.downloads.DeleteDownloadUseCase
import com.asadraza.streamflix.domain.usecase.downloads.PauseDownloadUseCase
import com.asadraza.streamflix.domain.usecase.downloads.ResumeDownloadUseCase
import com.asadraza.streamflix.domain.usecase.downloads.CancelDownloadUseCase
import com.asadraza.streamflix.domain.usecase.downloads.GetStorageInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    private val _effect = Channel<DownloadsEffect>()
    val effect: Flow<DownloadsEffect> = _effect.receiveAsFlow()

    init {
        loadDownloads()
        loadStorageInfo()
    }

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.OnLoadDownloads -> loadDownloads()
            is DownloadsEvent.OnDownloadClick -> handleDownloadClick(event.download)
            is DownloadsEvent.OnDownloadLongClick -> enterSelectionMode(event.download)
            is DownloadsEvent.OnDeleteDownload -> showDeleteConfirmation(event.download)
            is DownloadsEvent.OnDeleteConfirm -> confirmDelete()
            is DownloadsEvent.OnDeleteCancel -> cancelDelete()
            is DownloadsEvent.OnDeleteAllClick -> showDeleteAllConfirmation()
            is DownloadsEvent.OnDeleteAllConfirm -> deleteAll()
            is DownloadsEvent.OnDeleteAllCancel -> cancelDeleteAll()
            is DownloadsEvent.OnDeleteSelectedClick -> deleteSelected()
            is DownloadsEvent.OnPauseDownload -> pauseDownload(event.downloadId)
            is DownloadsEvent.OnResumeDownload -> resumeDownload(event.downloadId)
            is DownloadsEvent.OnCancelDownload -> cancelDownload(event.downloadId)
            is DownloadsEvent.OnRetryDownload -> retryDownload(event.downloadId)
            is DownloadsEvent.OnSelectDownload -> toggleSelection(event.downloadId)
            is DownloadsEvent.OnExitSelectionMode -> exitSelectionMode()
            is DownloadsEvent.OnSelectAll -> selectAll()
            is DownloadsEvent.OnDeselectAll -> deselectAll()
            is DownloadsEvent.OnErrorDismiss -> dismissError()
            is DownloadsEvent.OnRetry -> loadDownloads()
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorType = null) }

            when (val result = getDownloadsUseCase()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            downloads = result.data,
                            isLoading = false,
                            errorType = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorType = result.type,
                            downloads = result.data ?: emptyList()
                        )
                    }
                }
                is Result.Loading -> {
                    _state.update {
                        it.copy(
                            isLoading = true,
                            downloads = result.data ?: emptyList()
                        )
                    }
                }
            }
        }
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            when (val result = getStorageInfoUseCase()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            totalStorageUsed = result.data.first,
                            availableStorage = result.data.second
                        )
                    }
                }
                else -> { /* No-op */ }
            }
        }
    }

    private fun handleDownloadClick(download: Download) {
        if (_state.value.isSelectionMode) {
            toggleSelection(download.id)
        } else {
            when (download.status) {
                DownloadStatus.COMPLETED -> {
                    viewModelScope.launch {
                        _effect.send(DownloadsEffect.NavigateToPlayer(download.movie.id))
                    }
                }
                DownloadStatus.FAILED -> {
                    retryDownload(download.id)
                }
                else -> {
                    // Show download details
                }
            }
        }
    }

    private fun enterSelectionMode(download: Download) {
        _state.update {
            it.copy(
                isSelectionMode = true,
                selectedDownloads = setOf(download.id)
            )
        }
    }

    private fun exitSelectionMode() {
        _state.update {
            it.copy(
                isSelectionMode = false,
                selectedDownloads = emptySet()
            )
        }
    }

    private fun toggleSelection(downloadId: String) {
        _state.update { state ->
            val newSelection = if (downloadId in state.selectedDownloads) {
                state.selectedDownloads - downloadId
            } else {
                state.selectedDownloads + downloadId
            }

            state.copy(
                selectedDownloads = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    private fun selectAll() {
        _state.update {
            it.copy(selectedDownloads = it.downloads.map { d -> d.id }.toSet())
        }
    }

    private fun deselectAll() {
        _state.update {
            it.copy(
                selectedDownloads = emptySet(),
                isSelectionMode = false
            )
        }
    }

    private fun showDeleteConfirmation(download: Download) {
        _state.update {
            it.copy(
                showDeleteConfirmation = true,
                downloadToDelete = download
            )
        }
    }

    private fun confirmDelete() {
        val download = _state.value.downloadToDelete ?: return

        viewModelScope.launch {
            when (deleteDownloadUseCase(download.id)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmation = false,
                            downloadToDelete = null,
                            downloads = it.downloads.filter { d -> d.id != download.id }
                        )
                    }
                    _effect.send(DownloadsEffect.ShowToast("Download deleted"))
                    loadStorageInfo()
                }
                is Result.Error -> {
                    _effect.send(DownloadsEffect.ShowToast("Failed to delete download"))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun cancelDelete() {
        _state.update {
            it.copy(
                showDeleteConfirmation = false,
                downloadToDelete = null
            )
        }
    }

    private fun showDeleteAllConfirmation() {
        _state.update { it.copy(showDeleteAllConfirmation = true) }
    }

    private fun deleteAll() {
        viewModelScope.launch {
            val downloads = _state.value.downloads
            downloads.forEach { download ->
                deleteDownloadUseCase(download.id)
            }
            _state.update {
                it.copy(
                    showDeleteAllConfirmation = false,
                    downloads = emptyList()
                )
            }
            _effect.send(DownloadsEffect.ShowToast("All downloads deleted"))
            loadStorageInfo()
        }
    }

    private fun deleteSelected() {
        viewModelScope.launch {
            val selectedIds = _state.value.selectedDownloads
            selectedIds.forEach { id ->
                deleteDownloadUseCase(id)
            }
            _state.update {
                it.copy(
                    downloads = it.downloads.filter { d -> d.id !in selectedIds },
                    selectedDownloads = emptySet(),
                    isSelectionMode = false
                )
            }
            _effect.send(DownloadsEffect.ShowToast("${selectedIds.size} downloads deleted"))
            loadStorageInfo()
        }
    }

    private fun cancelDeleteAll() {
        _state.update { it.copy(showDeleteAllConfirmation = false) }
    }

    private fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            when (pauseDownloadUseCase(downloadId)) {
                is Result.Success -> {
                    _effect.send(DownloadsEffect.ShowToast("Download paused"))
                }
                is Result.Error -> {
                    _effect.send(DownloadsEffect.ShowToast("Failed to pause download"))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            when (resumeDownloadUseCase(downloadId)) {
                is Result.Success -> {
                    _effect.send(DownloadsEffect.ShowToast("Download resumed"))
                }
                is Result.Error -> {
                    _effect.send(DownloadsEffect.ShowToast("Failed to resume download"))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            when (cancelDownloadUseCase(downloadId)) {
                is Result.Success -> {
                    _effect.send(DownloadsEffect.ShowToast("Download cancelled"))
                }
                is Result.Error -> {
                    _effect.send(DownloadsEffect.ShowToast("Failed to cancel download"))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            when (resumeDownloadUseCase(downloadId)) {
                is Result.Success -> {
                    _effect.send(DownloadsEffect.ShowToast("Download retrying"))
                }
                is Result.Error -> {
                    _effect.send(DownloadsEffect.ShowToast("Failed to retry download"))
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }
}
