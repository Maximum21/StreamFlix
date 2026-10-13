package com.asadraza.streamflix.navigation

import androidx.navigation.NavOptionsBuilder

/**
 * Navigation Manager - Abstraction for navigation operations
 */
interface NavigationManager {

    /**
     * Navigate to a screen
     */
    fun navigateTo(screen: Screen, builder: NavOptionsBuilder.() -> Unit = {})

    /**
     * Navigate back to previous screen
     */
    fun navigateBack(): Boolean

    /**
     * Navigate back to a specific screen
     * @param screen The screen to navigate back to
     * @param inclusive Whether to include the screen in the pop operation
     */
    fun navigateBackTo(screen: Screen, inclusive: Boolean = false)

    /**
     * Navigate up in the navigation hierarchy
     */
    fun navigateUp(): Boolean

    /**
     * Clear the entire back stack and navigate to a screen
     * @param screen The destination screen
     */
    fun clearBackStackAndNavigateTo(screen: Screen)

    /**
     * Replace current screen with a new one
     * @param screen The destination screen
     */
    fun replaceWith(screen: Screen)

    /**
     * Check if we can navigate back
     * @return true if there are screens in the back stack
     */
    fun canNavigateBack(): Boolean

    /**
     * Get the current route
     * @return The current screen or null
     */
    fun getCurrentRoute(): Screen?
}

/**
 * Navigation Events - For one-time navigation actions
 * For event-driven navigation from ViewModels
 */
sealed interface NavigationEvent {
    data class NavigateTo(
        val screen: Screen,
        val builder: NavOptionsBuilder.() -> Unit = {}
    ) : NavigationEvent

    data object NavigateBack : NavigationEvent

    data class NavigateBackTo(
        val screen: Screen,
        val inclusive: Boolean = false
    ) : NavigationEvent

    data class ClearBackStackAndNavigateTo(
        val screen: Screen
    ) : NavigationEvent
}