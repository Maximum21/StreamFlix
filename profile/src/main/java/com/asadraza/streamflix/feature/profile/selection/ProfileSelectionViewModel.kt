package com.asadraza.streamflix.feature.profile.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SetCurrentProfileUseCase
import com.asadraza.streamflix.core.model.profile.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile Selection Screen
 *
 * Responsibilities:
 * - Load available profiles
 * - Handle profile selection
 * - Navigate to Home or Management screen
 * - Handle errors and loading states
 *
 * Clean Architecture:
 * - Depends on domain layer (UseCases)
 * - No Android framework dependencies
 * - Testable with mocked use cases
 *
 * SOLID Principles:
 * - SRP: Only manages profile selection logic
 * - DIP: Depends on abstractions (UseCases)
 * - OCP: Easy to extend with new events
 *
 * Pattern: MVI (Model-View-Intent)
 * - State: Single source of truth via StateFlow
 * - Events: User actions processed through onEvent()
 * - Effects: One-time side effects via Channel
 */
@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val setCurrentProfileUseCase: SetCurrentProfileUseCase
) : ViewModel() {

    // ══════════════════════════════════════════════════════════
    //  State Management
    // ══════════════════════════════════════════════════════════

    private val _state = MutableStateFlow(ProfileSelectionState())
    val state: StateFlow<ProfileSelectionState> = _state.asStateFlow()

    private val _effect = Channel<ProfileSelectionEffect>()
    val effect: Flow<ProfileSelectionEffect> = _effect.receiveAsFlow()

    init {
        loadProfiles()
    }

    // ══════════════════════════════════════════════════════════
    //  Event Handling
    // ══════════════════════════════════════════════════════════

    /**
     * Single entry point for all user events
     * Follows MVI pattern for predictable state management
     */
    fun onEvent(event: ProfileSelectionEvent) {
        when (event) {
            is ProfileSelectionEvent.OnLoadProfiles -> loadProfiles()
            is ProfileSelectionEvent.OnProfileClick -> selectProfile(event.profile)
            is ProfileSelectionEvent.OnManageProfilesClick -> navigateToManagement()
            is ProfileSelectionEvent.OnErrorDismiss -> dismissError()
            is ProfileSelectionEvent.OnRetry -> retryLoad()
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Private Methods
    // ══════════════════════════════════════════════════════════

    /**
     * Load profiles from repository
     *
     * Flow pattern:
     * 1. Loading state (show shimmer/skeleton)
     * 2. Success state (show profiles)
     * 3. Error state (show error with retry option)
     *
     * Handles stale data: If error occurs, keep showing old data
     */
    private fun loadProfiles() {
        viewModelScope.launch {
            getProfilesUseCase()
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _state.update {
                                it.copy(
                                    isLoading = true,
                                    errorType = null,
                                    // Keep existing profiles (stale data) during reload
                                    profiles = result.data ?: it.profiles
                                )
                            }
                        }

                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    profiles = result.data,
                                    isLoading = false,
                                    errorType = null,
                                    hasProfiles = result.data.isNotEmpty()
                                )
                            }

                            // If no profiles exist, navigate to management to create one
                            if (result.data.isEmpty()) {
                                viewModelScope.launch {
                                    _effect.send(ProfileSelectionEffect.ShowToast("Create your first profile"))
                                    _effect.send(ProfileSelectionEffect.NavigateToManagement)
                                }
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorType = result.type,
                                    // Keep existing profiles (stale data) if available
                                    profiles = result.data ?: it.profiles
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * Select a profile and set it as current
     *
     * Steps:
     * 1. Call SetCurrentProfileUseCase
     * 2. On success, navigate to home
     * 3. On error, show toast and retry
     */
    private fun selectProfile(profile: Profile) {

        viewModelScope.launch {
            setCurrentProfileUseCase(profile.id)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is Result.Success -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(ProfileSelectionEffect.NavigateToHome(profile.id))
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(
                                ProfileSelectionEffect.ShowToast("Failed to select profile. Please try again.")
                            )
                        }
                    }
                }
        }
    }

    /**
     * Navigate to Profile Management screen
     */
    private fun navigateToManagement() {
        viewModelScope.launch {
            _effect.send(ProfileSelectionEffect.NavigateToManagement)
        }
    }

    /**
     * Dismiss error message
     */
    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }

    /**
     * Retry loading profiles after an error
     */
    private fun retryLoad() {
        loadProfiles()
    }

    // ══════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
    }
}