package com.asadraza.streamflix.feature.profile.management

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.profile.Profile

/**
 * Profile Management Contract
 */

/**
 * Represents the UI state for Profile Management screen
 *
 * Immutable state that drives the UI rendering
 */
data class ProfileManagementState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val maxProfilesReached: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val editingProfile: Profile? = null,
    val profileToDelete: Profile? = null,
    val canDeleteProfile: Boolean = true
)

/**
 * User events/actions on Profile Management screen
 */
sealed class ProfileManagementEvent {
    /**
     * Triggered when screen is first loaded or refreshed
     */
    data object OnLoadProfiles : ProfileManagementEvent()

    /**
     * User clicks "Add Profile" button
     */
    data object OnAddProfileClick : ProfileManagementEvent()

    /**
     * User clicks edit button on a profile
     * @param profile The profile to edit
     */
    data class OnEditProfileClick(val profile: Profile) : ProfileManagementEvent()

    /**
     * User clicks delete button on a profile
     * @param profile The profile to delete
     */
    data class OnDeleteProfileClick(val profile: Profile) : ProfileManagementEvent()

    /**
     * User confirms deletion in the dialog
     */
    data object OnDeleteConfirm : ProfileManagementEvent()

    /**
     * User cancels deletion
     */
    data object OnDeleteCancel : ProfileManagementEvent()

    /**
     * User saves a new or edited profile
     * @param name Profile name
     * @param avatarUrl Avatar URL or identifier
     * @param isKidsProfile Whether this is a kids profile
     */
    data class OnSaveProfile(
        val name: String,
        val avatarUrl: String = "",
        val isKidsProfile: Boolean = false
    ) : ProfileManagementEvent()

    /**
     * User dismisses create/edit dialog
     */
    data object OnDismissDialog : ProfileManagementEvent()

    /**
     * User clicks back button
     */
    data object OnBackClick : ProfileManagementEvent()

    /**
     * User dismisses an error message
     */
    data object OnErrorDismiss : ProfileManagementEvent()

    /**
     * User clicks retry after an error
     */
    data object OnRetry : ProfileManagementEvent()
}

/**
 * One-time side effects for Profile Management screen
 *
 * These are consumed once and don't affect state
 */
sealed class ProfileManagementEffect {
    /**
     * Navigate back to previous screen
     */
    data object NavigateBack : ProfileManagementEffect()

    /**
     * Show a toast message
     * @param message The message to display
     */
    data class ShowToast(val message: String) : ProfileManagementEffect()

    /**
     * Show validation error
     * @param message The validation error message
     */
    data class ShowValidationError(val message: String) : ProfileManagementEffect()
}