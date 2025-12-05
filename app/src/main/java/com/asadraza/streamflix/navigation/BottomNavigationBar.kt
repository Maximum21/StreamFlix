package com.asadraza.streamflix.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import timber.log.Timber

/**
 * Bottom Navigation Bar Component
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(
            modifier = modifier,
            tonalElevation = NavigationBarDefaults.Elevation
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            Screen.BottomNav.items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route?.contains(item.route::class.simpleName ?: "") == true
                } == true

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = { Text(item.title) },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navigateToBottomNavItem(navController, item.route)
                        }
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}

/**
 * Navigate to bottom navigation item
 *
 * Best Practices:
 * - Launch single top (no duplicate screens)
 * - Pop to start destination (clean back stack)
 * - Restore state (preserve scroll position, etc.)
 */
private fun navigateToBottomNavItem(navController: NavController, screen: Screen) {
    try {
        navController.navigate(screen) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
        Timber.d("Navigated to bottom nav item: ${screen::class.simpleName}")
    } catch (e: Exception) {
        Timber.e(e, "Error navigating to bottom nav item: ${screen::class.simpleName}")
    }
}