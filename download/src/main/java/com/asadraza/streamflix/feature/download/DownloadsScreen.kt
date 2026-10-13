package com.asadraza.streamflix.feature.download

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.asadraza.streamflix.core.common.result.ErrorType
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayMovie: (String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DownloadsEffect.NavigateToPlayer -> onPlayMovie(effect.movieId)
                is DownloadsEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is DownloadsEffect.ShowStorageFull -> {
                    snackbarHostState.showSnackbar("Storage is full")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    if (!state.isSelectionMode) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    } else {
                        IconButton(onClick = { viewModel.onEvent(DownloadsEvent.OnExitSelectionMode) }) {
                            Icon(Icons.Default.Close, "Exit selection")
                        }
                    }
                },
                actions = {
                    if (state.isSelectionMode) {
                        Text(
                            text = "${state.selectedDownloads.size} selected",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { viewModel.onEvent(DownloadsEvent.OnSelectAll) }) {
                            Icon(Icons.Default.SelectAll, "Select all")
                        }
                        IconButton(onClick = { viewModel.onEvent(DownloadsEvent.OnDeleteSelectedClick) }) {
                            Icon(Icons.Default.Delete, "Delete selected")
                        }
                    } else if (state.downloads.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(DownloadsEvent.OnDeleteAllClick) }) {
                            Icon(Icons.Default.DeleteSweep, "Delete all")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading && state.downloads.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.downloads.isEmpty()) {
                EmptyDownloads(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Storage Info
                    StorageInfo(
                        totalUsed = state.totalStorageUsed,
                        available = state.availableStorage,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Downloads List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.downloads, key = { it.id }) { download ->
                            DownloadItem(
                                download = download,
                                isSelected = download.id in state.selectedDownloads,
                                isSelectionMode = state.isSelectionMode,
                                onClick = {
                                    viewModel.onEvent(DownloadsEvent.OnDownloadClick(download))
                                },
                                onLongClick = {
                                    viewModel.onEvent(DownloadsEvent.OnDownloadLongClick(download))
                                },
                                onPause = {
                                    viewModel.onEvent(DownloadsEvent.OnPauseDownload(download.id))
                                },
                                onResume = {
                                    viewModel.onEvent(DownloadsEvent.OnResumeDownload(download.id))
                                },
                                onCancel = {
                                    viewModel.onEvent(DownloadsEvent.OnCancelDownload(download.id))
                                },
                                onDelete = {
                                    viewModel.onEvent(DownloadsEvent.OnDeleteDownload(download))
                                }
                            )
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            if (state.showDeleteConfirmation) {
                DeleteConfirmationDialog(
                    title = state.downloadToDelete?.movie?.title ?: "",
                    onConfirm = { viewModel.onEvent(DownloadsEvent.OnDeleteConfirm) },
                    onDismiss = { viewModel.onEvent(DownloadsEvent.OnDeleteCancel) }
                )
            }

            // Delete All Confirmation
            if (state.showDeleteAllConfirmation) {
                DeleteAllConfirmationDialog(
                    count = state.downloads.size,
                    onConfirm = { viewModel.onEvent(DownloadsEvent.OnDeleteAllConfirm) },
                    onDismiss = { viewModel.onEvent(DownloadsEvent.OnDeleteAllCancel) }
                )
            }

            // Error Display
            state.errorType?.let { errorType ->
                ErrorCard(
                    errorType = errorType,
                    onRetry = { viewModel.onEvent(DownloadsEvent.OnRetry) },
                    onDismiss = { viewModel.onEvent(DownloadsEvent.OnErrorDismiss) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadItem(
    download: Download,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Image(
                painter = rememberAsyncImagePainter(download.movie.posterPath),
                contentDescription = null,
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = download.movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.quality.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress or Status
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        Column {
                            LinearProgressIndicator(
                                progress = { download.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(download.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = formatFileSize(download.downloadedBytes) + " / " +
                                            formatFileSize(download.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatFileSize(download.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DownloadStatus.FAILED -> {
                        Text(
                            text = "Failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    DownloadStatus.QUEUED -> {
                        Text(
                            text = "Queued",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DownloadStatus.CANCELLED -> {
                        Text(
                            text = "Cancelled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions
            if (!isSelectionMode) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, "Pause")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, "Resume")
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.Refresh, "Retry")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StorageInfo(
    totalUsed: Long,
    available: Long,
    modifier: Modifier = Modifier
) {
    val total = totalUsed + available
    val usedPercentage = if (total > 0) totalUsed.toFloat() / total else 0f

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatFileSize(totalUsed)} / ${formatFileSize(total)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { usedPercentage },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyDownloads(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Downloads",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Downloaded movies will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Download?") },
        text = {
            Text("Are you sure you want to delete '$title'? This will free up storage space.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAllConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete All Downloads?") },
        text = {
            Text("Are you sure you want to delete all $count downloads? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorCard(
    errorType: ErrorType,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (errorType) {
                    is ErrorType.Network -> "Network Error"
                    is ErrorType.Database -> "Storage Error"
                    else -> "Error"
                },
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format(Locale.getDefault(),"%.2f GB", gb)
        mb >= 1 -> String.format(Locale.getDefault(),"%.2f MB", mb)
        kb >= 1 -> String.format(Locale.getDefault(),"%.2f KB", kb)
        else -> "$bytes B"
    }
}
