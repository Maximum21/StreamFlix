package com.asadraza.streamflix.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination

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
                            contentDescription = stringResource(item.titleRes)
                        )
                    },
                    label = { Text(stringResource(item.titleRes)) },
                    alwaysShowLabel = true
                )
            }
        },
        layoutType = layoutType,
        modifier = modifier,
        content = { content() }
    )
}

// Single top, pop to Home and restore state, per the Material navigation guidance. Home rather than the
// graph start, which is Login when the app was launched signed out
private fun NavController.navigateToTopLevel(screen: Screen) {
    navigate(screen) {
        popUpTo<Screen.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
