package com.asadraza.streamflix.feature.profile

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.profile.Profile

data class ProfileState(
    val profiles: List<Profile> = emptyList(),
    val currentProfile: Profile? = null,
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val isEditMode: Boolean = false,
    val showProfileSelector: Boolean = false,
    val showCreateProfile: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val profileToDelete: Profile? = null,
    val maxProfilesReached: Boolean = false
)

sealed class ProfileEvent {
    data object OnLoadProfiles : ProfileEvent()
    data class OnProfileClick(val profile: Profile) : ProfileEvent()
    data object OnAddProfileClick : ProfileEvent()
    data class OnCreateProfile(val name: String, val avatarUrl: String) : ProfileEvent()
    data class OnEditProfile(val profile: Profile) : ProfileEvent()
    data class OnDeleteProfile(val profile: Profile) : ProfileEvent()
    data object OnDeleteConfirm : ProfileEvent()
    data object OnDeleteCancel : ProfileEvent()
    data object OnEditModeToggle : ProfileEvent()
    data object OnDismissProfileSelector : ProfileEvent()
    data object OnDismissCreateProfile : ProfileEvent()
    data object OnErrorDismiss : ProfileEvent()
    data object OnRetry : ProfileEvent()
}

sealed class ProfileEffect {
    data class NavigateToHome(val profileId: String) : ProfileEffect()
    data class ShowToast(val message: String) : ProfileEffect()
    data object NavigateBack : ProfileEffect()
}
