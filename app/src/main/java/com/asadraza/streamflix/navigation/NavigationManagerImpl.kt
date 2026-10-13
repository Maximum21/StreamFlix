package com.asadraza.streamflix.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NavigationManager using NavController
 *
 * This class wraps NavController and provides a clean interface
 * for navigation operations throughout the app.
 */
@Singleton
class NavigationManagerImpl @Inject constructor() : NavigationManager {

    // NavController is set by the composable, not through constructor
    private var navController: NavController? = null

    /**
     * Set the NavController
     * Called from the root composable when NavController is created
     */
    fun setNavController(controller: NavController) {
        this.navController = controller
        Timber.d("NavController set in NavigationManager")
    }

    /**
     * Clear the NavController
     * Called when the NavHost is disposed
     */
    fun clearNavController() {
        this.navController = null
        Timber.d("NavController cleared from NavigationManager")
    }

    override fun navigateTo(screen: Screen, builder: NavOptionsBuilder.() -> Unit) {
        try {
            navController?.navigate(route = screen, builder = builder) ?: run {
                Timber.w("Attempted to navigate but NavController is null")
            }
            Timber.d("Navigated to: ${screen::class.simpleName}")
        } catch (e: Exception) {
            Timber.e(e, "Navigation error to ${screen::class.simpleName}")
        }
    }

    override fun navigateBack(): Boolean {
        return try {
            val result = navController?.popBackStack() ?: false
            if (result) {
                Timber.d("Navigated back successfully")
            } else {
                Timber.w("Cannot navigate back - back stack is empty or NavController is null")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error navigating back")
            false
        }
    }

    override fun navigateBackTo(screen: Screen, inclusive: Boolean) {
        try {
            navController?.navigate(route = screen) {
                popUpTo(screen::class) {
                    this.inclusive = inclusive
                }
            } ?: run {
                Timber.w("Attempted to navigate back but NavController is null")
            }
            Timber.d("Navigated back to: ${screen::class.simpleName}, inclusive: $inclusive")
        } catch (e: Exception) {
            Timber.e(e, "Error navigating back to ${screen::class.simpleName}")
        }
    }

    override fun navigateUp(): Boolean {
        return try {
            val result = navController?.navigateUp() ?: false
            if (result) {
                Timber.d("Navigated up successfully")
            } else {
                Timber.w("Cannot navigate up - NavController is null or at root")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error navigating up")
            false
        }
    }

    override fun clearBackStackAndNavigateTo(screen: Screen) {
        try {
            val controller = navController
            if (controller != null) {
                controller.navigate(route = screen) {
                    // Clear entire back stack by popping to graph's start destination
                    popUpTo(controller.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    // Avoid multiple copies of the same destination
                    launchSingleTop = true
                }
                Timber.d("Cleared back stack and navigated to: ${screen::class.simpleName}")
            } else {
                Timber.w("Attempted to clear back stack but NavController is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing back stack and navigating to ${screen::class.simpleName}")
        }
    }

    override fun replaceWith(screen: Screen) {
        try {
            val controller = navController
            val currentDestId = controller?.currentDestination?.id

            if (controller != null && currentDestId != null) {
                controller.navigate(route = screen) {
                    // Pop the current screen
                    popUpTo(currentDestId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
                Timber.d("Replaced current screen with: ${screen::class.simpleName}")
            } else {
                Timber.w("Attempted to replace screen but NavController is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error replacing screen with ${screen::class.simpleName}")
        }
    }

    override fun canNavigateBack(): Boolean {
        return try {
            val canNavigate = navController?.previousBackStackEntry != null
            Timber.d("Can navigate back: $canNavigate")
            canNavigate
        } catch (e: Exception) {
            Timber.e(e, "Error checking if can navigate back")
            false
        }
    }

    override fun getCurrentRoute(): Screen? {
        return try {
            // This is a simplified version - in real implementation,
            // you'd need to properly deserialize the current destination to a Screen
            val currentRoute = navController?.currentBackStackEntry?.destination?.route
            Timber.d("Current route: $currentRoute")
            // Return null for now - proper implementation would deserialize the route
            null
        } catch (e: Exception) {
            Timber.e(e, "Error getting current route")
            null
        }
    }
}