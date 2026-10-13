package com.asadraza.streamflix.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.profile.Profile
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen(
    onProfileSelected: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.NavigateToHome -> onProfileSelected(effect.profileId)
                is ProfileEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is ProfileEffect.NavigateBack -> { /* Handle if needed */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Who's watching?",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (state.isLoading && state.profiles.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.profiles) { profile ->
                            ProfileCard(
                                profile = profile,
                                isEditMode = state.isEditMode,
                                onClick = {
                                    if (state.isEditMode) {
                                        viewModel.onEvent(ProfileEvent.OnEditProfile(profile))
                                    } else {
                                        viewModel.onEvent(ProfileEvent.OnProfileClick(profile))
                                    }
                                },
                                onDelete = {
                                    viewModel.onEvent(ProfileEvent.OnDeleteProfile(profile))
                                }
                            )
                        }

                        // Add Profile Card
                        if (!state.maxProfilesReached) {
                            item {
                                AddProfileCard(
                                    onClick = {
                                        viewModel.onEvent(ProfileEvent.OnAddProfileClick)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = { viewModel.onEvent(ProfileEvent.OnEditModeToggle) }
                ) {
                    Text(if (state.isEditMode) "Done" else "Manage Profiles")
                }
            }

            // Create Profile Dialog
            if (state.showCreateProfile) {
                CreateProfileDialog(
                    onConfirm = { name, avatarUrl ->
                        viewModel.onEvent(ProfileEvent.OnCreateProfile(name, avatarUrl))
                    },
                    onDismiss = {
                        viewModel.onEvent(ProfileEvent.OnDismissCreateProfile)
                    }
                )
            }

            // Delete Confirmation Dialog
            if (state.showDeleteConfirmation) {
                DeleteConfirmationDialog(
                    profileName = state.profileToDelete?.name ?: "",
                    onConfirm = { viewModel.onEvent(ProfileEvent.OnDeleteConfirm) },
                    onDismiss = { viewModel.onEvent(ProfileEvent.OnDeleteCancel) }
                )
            }

            // Error Display
            state.errorType?.let { errorType ->
                ErrorCard(
                    errorType = errorType,
                    onRetry = { viewModel.onEvent(ProfileEvent.OnRetry) },
                    onDismiss = { viewModel.onEvent(ProfileEvent.OnErrorDismiss) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(profile.avatarUrl),
                contentDescription = profile.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.error,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Profile",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add Profile",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CreateProfileDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$name"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, avatarUrl) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
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
private fun DeleteConfirmationDialog(
    profileName: String,
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
        title = { Text("Delete Profile?") },
        text = {
            Text("Are you sure you want to delete '$profileName'? This action cannot be undone.")
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
                    is ErrorType.Database -> "Database Error"
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