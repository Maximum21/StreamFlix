package com.asadraza.streamflix.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Top-level navigation that adapts to the window: a bottom bar on phones and a rail on wider
 * windows. It is hidden on screens outside the top-level set (sign-in, detail, player).
 */
@Composable
fun AppNavigationSuite(
    navController: NavController,
    currentDestination: NavDestination?,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = if (visible) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { item ->
                val selected = item.isSelected(currentDestination)
                item(
                    selected = selected,
                    onClick = { if (!selected) navController.navigateToTopLevel(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = { Text(item.title) },
                    alwaysShowLabel = true
                )
            }
        },
        layoutType = layoutType,
        modifier = modifier,
        content = { content() }
    )
}

// Single top, pop to the start destination and restore state, per the Material navigation guidance
private fun NavController.navigateToTopLevel(screen: Screen) {
    navigate(screen) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
