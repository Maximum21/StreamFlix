package com.asadraza.streamflix.feature.profile.selection

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.model.profile.Profile

/**
 * Profile Selection Contract
 *
 * Defines the state, events, and effects for the Profile Selection screen.
 * This screen is shown after login for users to choose which profile to use.
 *
 * Pattern: MVI (Model-View-Intent)
 * - State: Immutable data class representing UI state
 * - Event: User actions that trigger state changes
 * - Effect: One-time side effects (navigation, toasts, etc.)
 *
 * SOLID Principles:
 * - SRP: Each sealed class has a single responsibility
 * - OCP: Easy to add new events/effects without modifying existing code
 * - ISP: Focused contracts for this specific screen
 */

/**
 * Represents the UI state for Profile Selection screen
 *
 * Immutable state that drives the UI rendering
 */
data class ProfileSelectionState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val errorType: ErrorType? = null,
    val hasProfiles: Boolean = false
)

/**
 * User events/actions on Profile Selection screen
 *
 * Sealed class ensures all possible events are known at compile-time
 */
sealed class ProfileSelectionEvent {
    /**
     * Triggered when screen is first loaded or refreshed
     */
    data object OnLoadProfiles : ProfileSelectionEvent()

    /**
     * User clicks on a profile to select it
     * @param profile The selected profile
     */
    data class OnProfileClick(val profile: Profile) : ProfileSelectionEvent()

    /**
     * User clicks "Manage Profiles" button
     */
    data object OnManageProfilesClick : ProfileSelectionEvent()

    /**
     * User dismisses an error message
     */
    data object OnErrorDismiss : ProfileSelectionEvent()

    /**
     * User clicks retry after an error
     */
    data object OnRetry : ProfileSelectionEvent()
}

/**
 * One-time side effects for Profile Selection screen
 *
 * These are consumed once and don't affect state
 */
sealed class ProfileSelectionEffect {
    /**
     * Navigate to home screen with selected profile
     * @param profileId The ID of the selected profile
     */
    data class NavigateToHome(val profileId: String) : ProfileSelectionEffect()

    /**
     * Navigate to Profile Management screen
     */
    data object NavigateToManagement : ProfileSelectionEffect()

    /**
     * Show a toast message
     * @param message The message to display
     */
    data class ShowToast(val message: String) : ProfileSelectionEffect()
}