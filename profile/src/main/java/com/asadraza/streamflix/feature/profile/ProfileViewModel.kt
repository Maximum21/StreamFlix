package com.asadraza.streamflix.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.domain.usecase.profile.DeleteProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SaveProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SetCurrentProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.model.profile.Profile

/**
 * ViewModel for Profile Management.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val setCurrentProfileUseCase: SetCurrentProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>()
    val effect: Flow<ProfileEffect> = _effect.receiveAsFlow()

    init {
        observeProfiles()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnLoadProfiles -> observeProfiles()
            is ProfileEvent.OnProfileClick -> selectProfile(event.profile)
            is ProfileEvent.OnAddProfileClick -> handleAddProfile()
            is ProfileEvent.OnCreateProfile -> createProfile(event.name, event.avatarUrl)
            is ProfileEvent.OnEditProfile -> editProfile(event.profile)
            is ProfileEvent.OnDeleteProfile -> showDeleteConfirmation(event.profile)
            is ProfileEvent.OnDeleteConfirm -> confirmDelete()
            is ProfileEvent.OnDeleteCancel -> cancelDelete()
            is ProfileEvent.OnEditModeToggle -> toggleEditMode()
            is ProfileEvent.OnDismissProfileSelector -> dismissProfileSelector()
            is ProfileEvent.OnDismissCreateProfile -> dismissCreateProfile()
            is ProfileEvent.OnErrorDismiss -> dismissError()
            is ProfileEvent.OnRetry -> observeProfiles()
        }
    }

    /**
     * Observes profiles with real-time Firestore updates.
     *
     * Flow pattern:
     * 1. Loading state
     * 2. Success with data
     * 3. Error with stale data if available
     */
    private fun observeProfiles() {
        viewModelScope.launch {
            getProfilesUseCase()
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _state.update {
                                it.copy(
                                    isLoading = true,
                                    errorType = null,
                                    profiles = result.data ?: it.profiles // Keep stale data
                                )
                            }
                        }
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    profiles = result.data,
                                    isLoading = false,
                                    errorType = null,
                                    maxProfilesReached = result.data.size >= 5
                                )
                            }
                        }
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorType = result.type,
                                    profiles = result.data ?: it.profiles // Keep stale data
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * Selects a profile and navigates to home.
     * Uses Flow for async operation with loading states.
     */
    private fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            setCurrentProfileUseCase(profile.id)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _state.update { it.copy(currentProfile = profile) }
                            _effect.send(ProfileEffect.NavigateToHome(profile.id))
                        }
                        is Result.Error -> {
                            _effect.send(ProfileEffect.ShowToast("Failed to select profile"))
                        }
                        is Result.Loading -> {
                            // Optional: Show loading indicator
                        }
                    }
                }
        }
    }

    private fun handleAddProfile() {
        if (_state.value.maxProfilesReached) {
            viewModelScope.launch {
                _effect.send(ProfileEffect.ShowToast("Maximum 5 profiles allowed"))
            }
        } else {
            _state.update { it.copy(showCreateProfile = true) }
        }
    }

    /**
     * Creates a new profile using Flow-based use case.
     */
    private fun createProfile(name: String, avatarUrl: String) {
        if (name.isBlank()) {
            viewModelScope.launch {
                _effect.send(ProfileEffect.ShowToast("Profile name cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            val profile = Profile(
                id = generateProfileId(),
                name = name,
                avatarUrl = avatarUrl
            )

            saveProfileUseCase(profile)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _state.update { it.copy(showCreateProfile = false) }
                            _effect.send(ProfileEffect.ShowToast("Profile created"))
                            // No need to manually update list - observeProfiles handles it
                        }
                        is Result.Error -> {
                            _effect.send(ProfileEffect.ShowToast("Failed to create profile"))
                        }
                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                }
        }
    }

    private fun editProfile(profile: Profile) {
        viewModelScope.launch {
            _effect.send(ProfileEffect.ShowToast("Edit feature coming soon"))
        }
    }

    private fun showDeleteConfirmation(profile: Profile) {
        _state.update {
            it.copy(
                showDeleteConfirmation = true,
                profileToDelete = profile
            )
        }
    }

    /**
     * Deletes profile using Flow-based use case.
     */
    private fun confirmDelete() {
        val profile = _state.value.profileToDelete ?: return

        viewModelScope.launch {
            deleteProfileUseCase(profile.id)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    showDeleteConfirmation = false,
                                    profileToDelete = null
                                )
                            }
                            _effect.send(ProfileEffect.ShowToast("Profile deleted"))
                            // No need to manually update list - observeProfiles handles it
                        }
                        is Result.Error -> {
                            _effect.send(ProfileEffect.ShowToast("Failed to delete profile"))
                        }
                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
                }
        }
    }

    private fun cancelDelete() {
        _state.update {
            it.copy(
                showDeleteConfirmation = false,
                profileToDelete = null
            )
        }
    }

    private fun toggleEditMode() {
        _state.update { it.copy(isEditMode = !it.isEditMode) }
    }

    private fun dismissProfileSelector() {
        _state.update { it.copy(showProfileSelector = false) }
    }

    private fun dismissCreateProfile() {
        _state.update { it.copy(showCreateProfile = false) }
    }

    private fun dismissError() {
        _state.update { it.copy(errorType = null) }
    }

    private fun generateProfileId(): String {
        return "profile_${System.currentTimeMillis()}"
    }
}

