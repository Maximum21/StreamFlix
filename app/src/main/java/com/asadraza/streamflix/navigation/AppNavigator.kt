package com.asadraza.streamflix.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Thin wrapper over [NavController] that names the navigation operations the app uses.
 * It is created per composition from the NavController, so nothing outlives the NavHost.
 * Failures are not swallowed: an invalid navigation is a bug that should surface.
 */
class AppNavigator(private val navController: NavController) {
    fun navigateTo(screen: Screen) = navController.navigate(screen)

    fun navigateBack(): Boolean = navController.popBackStack()

    /** Replaces the whole back stack, e.g. after login or sign-out. */
    fun clearBackStackAndNavigateTo(screen: Screen) = navController.navigate(screen) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }

    /** Replaces the current screen so it is not reachable with Back. */
    fun replaceWith(screen: Screen) {
        val currentId = navController.currentDestination?.id
        navController.navigate(screen) {
            if (currentId != null) popUpTo(currentId) { inclusive = true }
            launchSingleTop = true
        }
    }
}
