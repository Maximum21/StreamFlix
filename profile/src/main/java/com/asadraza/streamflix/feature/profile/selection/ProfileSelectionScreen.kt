package com.asadraza.streamflix.feature.profile.selection

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.common.result.getUserMessage
import com.asadraza.streamflix.core.model.profile.Profile
import com.asadraza.streamflix.core.ui.components.ErrorMessage
import com.asadraza.streamflix.core.ui.components.LoadingIndicator

/**
 * Profile Selection Screen
 *
 * Purpose: Allow users to select which profile they want to use
 * Similar to Netflix's "Who's watching?" screen
 *
 * Features:
 * - Grid of profile avatars
 * - Profile names
 * - "Manage Profiles" button
 * - Loading state
 * - Error state with retry
 * - Empty state (redirects to management)
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
fun ProfileSelectionScreen(
    onProfileSelected: (String) -> Unit,
    onNavigateToManageProfiles: () -> Unit,
    viewModel: ProfileSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle side effects (navigation, toasts)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileSelectionEffect.NavigateToHome -> {
                    onProfileSelected(effect.profileId)
                }
                is ProfileSelectionEffect.NavigateToManagement -> {
                    onNavigateToManageProfiles()
                }
                is ProfileSelectionEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Main content
    ProfileSelectionContent(
        state = state,
        onEvent = viewModel::onEvent,
        onManageProfiles = onNavigateToManageProfiles
    )
}

/**
 * Main content composable
 * Separated for easier preview and testing
 */
@Composable
private fun ProfileSelectionContent(
    state: ProfileSelectionState,
    onEvent: (ProfileSelectionEvent) -> Unit,
    onManageProfiles: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
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

                // Error state (with retry)
                state.errorType != null && state.profiles.isEmpty() -> {
                    ErrorMessage(
                        message = state.errorType.getUserMessage(),
                        onRetry = { onEvent(ProfileSelectionEvent.OnRetry) },
//                        onDismiss = { onEvent(ProfileSelectionEvent.OnErrorDismiss) }
                    )
                }

                // Success state (show profiles)
                state.hasProfiles -> {
                    ProfileSelectionSuccess(
                        profiles = state.profiles,
                        isLoading = state.isLoading,
                        onProfileClick = { profile ->
                            onEvent(ProfileSelectionEvent.OnProfileClick(profile))
                        },
                        onManageProfiles = onManageProfiles
                    )
                }

                // Empty state (no profiles - should auto-navigate to management)
                else -> {
                    EmptyProfilesState(
                        onCreateProfile = onManageProfiles
                    )
                }
            }
        }
    }
}

/**
 * Success state - Show profiles grid
 */
@Composable
private fun ProfileSelectionSuccess(
    profiles: List<Profile>,
    isLoading: Boolean,
    onProfileClick: (Profile) -> Unit,
    onManageProfiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Who's watching?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Profiles grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            items(
                items = profiles,
                key = { it.id }
            ) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { onProfileClick(profile) },
                    enabled = !isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Manage Profiles button
        TextButton(
            onClick = onManageProfiles,
            enabled = !isLoading
        ) {
            Text(
                text = "Manage Profiles",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Profile card component
 */
@Composable
private fun ProfileCard(
    profile: Profile,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Avatar icon or image
            if (profile.avatarUrl.isNotBlank()) {
                // TODO: Load image with Coil
                // For now, show icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = profile.name,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = profile.name,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Kids profile indicator
            if (profile.isKidsProfile) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = "KIDS",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile name
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Empty state - No profiles exist
 */
@Composable
private fun EmptyProfilesState(
    onCreateProfile: () -> Unit
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

            Text(
                text = "Create your first profile to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCreateProfile,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Profile")
            }
        }
    }
}