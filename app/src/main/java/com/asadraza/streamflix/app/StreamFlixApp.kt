package com.asadraza.streamflix.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.asadraza.streamflix.navigation.AppNavGraph
import com.asadraza.streamflix.navigation.BottomNavigationBar
import com.asadraza.streamflix.navigation.NavigationManagerImpl
import com.asadraza.streamflix.navigation.showsBottomBar
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber

/**
 * StreamFlixApp - Root composable for the entire app
 */
@Composable
fun StreamFlixApp(
    viewModel: AppViewModel = hiltViewModel(),
    navigationManager: NavigationManagerImpl = hiltViewModel<NavigationManagerProvider>().navigationManager
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Log app state changes
    LaunchedEffect(appState) {
        Timber.d("App state changed: $appState")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom bar only on main screens
            val currentRoute = appState.currentScreen
            val showBottomBar = currentRoute?.showsBottomBar() == true

            BottomNavigationBar(
                navController = navController,
                visible = showBottomBar
            )
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            navigationManager = navigationManager,
            startDestination = appState.startDestination,
            modifier = Modifier
        )
    }
}

/**
 * Workaround Provider to inject NavigationManager into composables
 */
@HiltViewModel
class NavigationManagerProvider @javax.inject.Inject constructor(
    val navigationManager: NavigationManagerImpl
) : androidx.lifecycle.ViewModel()