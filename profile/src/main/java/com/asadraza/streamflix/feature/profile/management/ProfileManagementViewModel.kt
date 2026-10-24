package com.asadraza.streamflix.feature.profile.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.ValidationReason
import com.asadraza.streamflix.core.domain.usecase.profile.DeleteProfileUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.GetProfilesUseCase
import com.asadraza.streamflix.core.domain.usecase.profile.SaveProfileUseCase
import com.asadraza.streamflix.core.model.profile.Profile
import com.asadraza.streamflix.core.ui.text.UiText
import com.asadraza.streamflix.core.ui.text.toUiText
import com.asadraza.streamflix.feature.profile.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Profile Management Screen
 */
@HiltViewModel
class ProfileManagementViewModel
@Inject
constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase
) : ViewModel() {
    //  State Management
    private val _state = MutableStateFlow(ProfileManagementState())
    val state: StateFlow<ProfileManagementState> = _state.asStateFlow()

    private val _effect = Channel<ProfileManagementEffect>()
    val effect: Flow<ProfileManagementEffect> = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    // A repeated tap must not start a second write while one is still running
    private var saveJob: Job? = null
    private var deleteJob: Job? = null

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
            is ProfileManagementEvent.OnSaveProfile ->
                saveProfile(
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
        // A retry replaces the running collector instead of adding a second one beside it
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
                                    maxProfilesReached = profiles.size >= Profile.MAX_PROFILES,
                                    canDeleteProfile = profiles.size > 1
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
                    ProfileManagementEffect.ShowToast(
                        ErrorType.Validation(ValidationReason.MaxProfilesReached).toUiText()
                    )
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
                    ProfileManagementEffect.ShowToast(ErrorType.Validation(ValidationReason.LastProfile).toUiText())
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
        if (saveJob?.isActive == true) return
        val editing = _state.value.editingProfile
        val profile =
            editing?.copy(name = name.trim(), avatarUrl = avatarUrl, isKidsProfile = isKidsProfile)
                ?: Profile(
                    id = Profile.NEW_ID,
                    name = name.trim(),
                    avatarUrl = avatarUrl,
                    isKidsProfile = isKidsProfile,
                    createdAt = System.currentTimeMillis()
                )
        val successMessage = if (editing != null) R.string.profile_updated else R.string.profile_created

        saveJob = viewModelScope.launch {
            saveProfileUseCase(profile)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> _state.update { it.copy(isLoading = true) }

                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showCreateDialog = false,
                                    showEditDialog = false,
                                    editingProfile = null
                                )
                            }
                            _effect.send(ProfileManagementEffect.ShowToast(UiText.Resource(successMessage)))
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(ProfileManagementEffect.ShowToast(result.type.toUiText()))
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
        if (deleteJob?.isActive == true) return

        deleteJob = viewModelScope.launch {
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
                                ProfileManagementEffect.ShowToast(UiText.Resource(R.string.profile_deleted))
                            )
                        }

                        is Result.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(
                                ProfileManagementEffect.ShowToast(result.type.toUiText())
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
        // A write that never completes (offline) must not block the next attempt. This only stops
        // waiting for the result: a write already queued by the data layer can still go through
        val deleteWasRunning = deleteJob?.isActive == true
        deleteJob?.cancel()
        _state.update {
            it.copy(
                // isLoading is shared with the list load, so it is only cleared for the write that was running
                isLoading = if (deleteWasRunning) false else it.isLoading,
                showDeleteDialog = false,
                profileToDelete = null
            )
        }
    }

    /**
     * Dismiss create/edit dialog
     */
    private fun dismissDialog() {
        val saveWasRunning = saveJob?.isActive == true
        saveJob?.cancel()
        _state.update {
            it.copy(
                isLoading = if (saveWasRunning) false else it.isLoading,
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
}
