package com.asadraza.streamflix.feature.profile.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.domain.usecase.profile.DeleteProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SaveProfileUseCase
import com.asadraza.streamflix.core.model.profile.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile Management Screen
 */
@HiltViewModel
class ProfileManagementViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase
) : ViewModel() {

    
    //  Constants
    companion object {
        private const val MAX_PROFILES = 5
        private const val MIN_PROFILES = 1
        private const val MIN_NAME_LENGTH = 1
        private const val MAX_NAME_LENGTH = 20
    }

    
    //  State Management
    private val _state = MutableStateFlow(ProfileManagementState())
    val state: StateFlow<ProfileManagementState> = _state.asStateFlow()

    private val _effect = Channel<ProfileManagementEffect>()
    val effect: Flow<ProfileManagementEffect> = _effect.receiveAsFlow()

    init {
        loadProfiles()
    }

    
    //  Event Handling
    /**
     * Single entry point for all user events
     */
    fun onEvent(event: ProfileManagementEvent) {
        when (event) {
            is ProfileManagementEvent.OnLoadProfiles -> loadProfiles()
            is ProfileManagementEvent.OnAddProfileClick -> handleAddProfile()
            is ProfileManagementEvent.OnEditProfileClick -> handleEditProfile(event.profile)
            is ProfileManagementEvent.OnDeleteProfileClick -> handleDeleteProfile(event.profile)
            is ProfileManagementEvent.OnDeleteConfirm -> confirmDelete()
            is ProfileManagementEvent.OnDeleteCancel -> cancelDelete()
            is ProfileManagementEvent.OnSaveProfile -> saveProfile(
                name = event.name,
                avatarUrl = event.avatarUrl,
                isKidsProfile = event.isKidsProfile
            )
            is ProfileManagementEvent.OnDismissDialog -> dismissDialog()
            is ProfileManagementEvent.OnBackClick -> navigateBack()
            is ProfileManagementEvent.OnErrorDismiss -> dismissError()
            is ProfileManagementEvent.OnRetry -> retryLoad()
        }
    }

    
    //  Private Methods - Data Loading
    /**
     * Load profiles from repository
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
                                    profiles = result.data ?: it.profiles
                                )
                            }
                        }

                        is Result.Success -> {
                            val profiles = result.data
                            _state.update {
                                it.copy(
                                    profiles = profiles,
                                    isLoading = false,
                                    errorType = null,
                                    maxProfilesReached = profiles.size >= MAX_PROFILES,
                                    canDeleteProfile = profiles.size > MIN_PROFILES
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorType = result.type,
                                    profiles = result.data ?: it.profiles
                                )
                            }
                        }
                    }
                }
        }
    }

    
    //  Private Methods - Profile Operations
    /**
     * Handle add profile button click
     */
    private fun handleAddProfile() {
        if (_state.value.maxProfilesReached) {
            viewModelScope.launch {
                _effect.send(
                    ProfileManagementEffect.ShowToast("Maximum $MAX_PROFILES profiles allowed")
                )
            }
        } else {
            _state.update {
                it.copy(
                    showCreateDialog = true,
                    editingProfile = null
                )
            }
        }
    }

    /**
     * Handle edit profile button click
     */
    private fun handleEditProfile(profile: Profile) {
        _state.update {
            it.copy(
                showEditDialog = true,
                editingProfile = profile
            )
        }
    }

    /**
     * Handle delete profile button click
     * Shows confirmation dialog
     */
    private fun handleDeleteProfile(profile: Profile) {
        if (!_state.value.canDeleteProfile) {
            viewModelScope.launch {
                _effect.send(
                    ProfileManagementEffect.ShowToast("Cannot delete the last profile")
                )
            }
            return
        }

        _state.update {
            it.copy(
                showDeleteDialog = true,
                profileToDelete = profile
            )
        }
    }

    /**
     * Save a new or edited profile
     */
    private fun saveProfile(name: String, avatarUrl: String, isKidsProfile: Boolean) {
        // Validate name
        val validationError = validateProfileName(name)
        if (validationError != null) {
            viewModelScope.launch {
                _effect.send(ProfileManagementEffect.ShowValidationError(validationError))
            }
            return
        }

        viewModelScope.launch {
            val profile = if (_state.value.editingProfile != null) {
                // Edit existing profile
                _state.value.editingProfile!!.copy(
                    name = name.trim(),
                    avatarUrl = avatarUrl,
                    isKidsProfile = isKidsProfile
                )
            } else {
                // Create new profile
                Profile(
                    id = generateProfileId(),
                    name = name.trim(),
                    avatarUrl = avatarUrl,
                    isKidsProfile = isKidsProfile,
                    createdAt = System.currentTimeMillis()
                )
            }

            saveProfileUseCase(profile)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showCreateDialog = false,
                                    showEditDialog = false,
                                    editingProfile = null
                                )
                            }
                            val message = if (_state.value.editingProfile != null) {
                                "Profile updated successfully"
                            } else {
                                "Profile created successfully"
                            }
                            _effect.send(ProfileManagementEffect.ShowToast(message))
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(
                                ProfileManagementEffect.ShowToast("Failed to save profile. Please try again.")
                            )
                        }
                    }
                }
        }
    }

    /**
     * Confirm profile deletion
     */
    private fun confirmDelete() {
        val profile = _state.value.profileToDelete ?: return

        viewModelScope.launch {
            deleteProfileUseCase(profile.id)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showDeleteDialog = false,
                                    profileToDelete = null
                                )
                            }
                            _effect.send(
                                ProfileManagementEffect.ShowToast("Profile deleted successfully")
                            )
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(
                                ProfileManagementEffect.ShowToast("Failed to delete profile. Please try again.")
                            )
                        }
                    }
                }
        }
    }

    /**
     * Cancel profile deletion
     */
    private fun cancelDelete() {
        _state.update {
            it.copy(
                showDeleteDialog = false,
                profileToDelete = null
            )
        }
    }

    /**
     * Dismiss create/edit dialog
     */
    private fun dismissDialog() {
        _state.update {
            it.copy(
                showCreateDialog = false,
                showEditDialog = false,
                editingProfile = null
            )
        }
    }

    /**
     * Navigate back to previous screen
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _effect.send(ProfileManagementEffect.NavigateBack)
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

    
    //  Private Methods - Validation & Utilities
    /**
     * Validate profile name
     * @return Error message if invalid, null if valid
     */
    private fun validateProfileName(name: String): String? {
        return when {
            name.isBlank() -> "Profile name cannot be empty"
            name.length < MIN_NAME_LENGTH -> "Profile name is too short"
            name.length > MAX_NAME_LENGTH -> "Profile name must be $MAX_NAME_LENGTH characters or less"
            else -> null
        }
    }

    /**
     * Generate a unique profile ID
     */
    private fun generateProfileId(): String {
        return "profile_${System.currentTimeMillis()}"
    }

    
    //  Lifecycle
    override fun onCleared() {
        super.onCleared()
    }
}