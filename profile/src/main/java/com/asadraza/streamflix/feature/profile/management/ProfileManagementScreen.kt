package com.asadraza.streamflix.feature.profile.management

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.common.result.getUserMessage
import com.asadraza.streamflix.core.model.profile.Profile
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator

/**
 * Profile Management Screen
 *
 * Purpose: Allow users to create, edit, and delete profiles
 *
 * Features:
 * - List of existing profiles
 * - Add new profile (FAB)
 * - Edit profile (icon button)
 * - Delete profile (icon button with confirmation)
 * - Profile validation
 * - Max 5 profiles enforcement
 * - Can't delete last profile enforcement
 * - Loading states
 * - Error states
 *
 * Clean Architecture:
 * - Pure UI layer
 * - No business logic
 * - Communicates via events
 * - Reacts to state changes
 *
 * SOLID Principles:
 * - SRP: Only responsible for rendering UI
 * - OCP: Easy to add new UI elements
 * - DIP: Depends on ViewModel abstraction
 */
@Composable
fun ProfileManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle side effects (navigation, toasts)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileManagementEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is ProfileManagementEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ProfileManagementEffect.ShowValidationError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Main content
    ProfileManagementContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )

    // Dialogs
    if (state.showCreateDialog) {
        ProfileDialog(
            title = "Create Profile",
            profile = null,
            onDismiss = { viewModel.onEvent(ProfileManagementEvent.OnDismissDialog) },
            onSave = { name, avatarUrl, isKids ->
                viewModel.onEvent(
                    ProfileManagementEvent.OnSaveProfile(
                        name = name,
                        avatarUrl = avatarUrl,
                        isKidsProfile = isKids
                    )
                )
            }
        )
    }

    if (state.showEditDialog && state.editingProfile != null) {
        ProfileDialog(
            title = "Edit Profile",
            profile = state.editingProfile,
            onDismiss = { viewModel.onEvent(ProfileManagementEvent.OnDismissDialog) },
            onSave = { name, avatarUrl, isKids ->
                viewModel.onEvent(
                    ProfileManagementEvent.OnSaveProfile(
                        name = name,
                        avatarUrl = avatarUrl,
                        isKidsProfile = isKids
                    )
                )
            }
        )
    }

    if (state.showDeleteDialog && state.profileToDelete != null) {
        DeleteConfirmationDialog(
            profile = state.profileToDelete!!,
            onConfirm = { viewModel.onEvent(ProfileManagementEvent.OnDeleteConfirm) },
            onDismiss = { viewModel.onEvent(ProfileManagementEvent.OnDeleteCancel) }
        )
    }
}

/**
 * Main content composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileManagementContent(
    state: ProfileManagementState,
    onEvent: (ProfileManagementEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Profiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!state.maxProfilesReached && !state.isLoading) {
                FloatingActionButton(
                    onClick = { onEvent(ProfileManagementEvent.OnAddProfileClick) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Profile"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading state
                state.isLoading && state.profiles.isEmpty() -> {
                    LoadingIndicator()
                }

                // Error state
                state.errorType != null && state.profiles.isEmpty() -> {
                    ErrorMessage(
                        message = state.errorType.getUserMessage(),
                        onRetry = { onEvent(ProfileManagementEvent.OnRetry) },
//                        onDismiss = { onEvent(ProfileManagementEvent.OnErrorDismiss) }
                    )
                }

                // Success state
                state.profiles.isNotEmpty() -> {
                    ProfileList(
                        profiles = state.profiles,
                        canDelete = state.canDeleteProfile,
                        isLoading = state.isLoading,
                        onEdit = { profile ->
                            onEvent(ProfileManagementEvent.OnEditProfileClick(profile))
                        },
                        onDelete = { profile ->
                            onEvent(ProfileManagementEvent.OnDeleteProfileClick(profile))
                        }
                    )

                    // Max profiles info
                    if (state.maxProfilesReached) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Maximum 5 profiles reached",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Empty state (should not happen, but handle it)
                else -> {
                    EmptyProfilesState(
                        onAddProfile = { onEvent(ProfileManagementEvent.OnAddProfileClick) }
                    )
                }
            }
        }
    }
}

/**
 * List of profiles
 */
@Composable
private fun ProfileList(
    profiles: List<Profile>,
    canDelete: Boolean,
    isLoading: Boolean,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = profiles,
            key = { it.id }
        ) { profile ->
            ProfileListItem(
                profile = profile,
                canDelete = canDelete,
                enabled = !isLoading,
                onEdit = { onEdit(profile) },
                onDelete = { onDelete(profile) }
            )
        }
    }
}

/**
 * Individual profile list item
 */
@Composable
private fun ProfileListItem(
    profile: Profile,
    canDelete: Boolean,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = profile.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Kids profile indicator
                if (profile.isKidsProfile) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = "K",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(4.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Profile info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (profile.isKidsProfile) {
                    Text(
                        text = "Kids Profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Action buttons
            IconButton(
                onClick = onEdit,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${profile.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = enabled && canDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${profile.name}",
                    tint = if (canDelete) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

/**
 * Empty state - No profiles exist
 */
@Composable
private fun EmptyProfilesState(
    onAddProfile: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Text(
                text = "No Profiles Yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = onAddProfile) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Profile")
            }
        }
    }
}