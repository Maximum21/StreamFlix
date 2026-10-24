package com.asadraza.streamflix.app

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.asadraza.streamflix.R
import com.asadraza.streamflix.navigation.AppNavGraph
import com.asadraza.streamflix.navigation.AppNavigationSuite
import com.asadraza.streamflix.navigation.AppNavigator
import com.asadraza.streamflix.navigation.Screen
import com.asadraza.streamflix.navigation.showsBottomBar

@Composable
fun StreamFlixApp(viewModel: AppViewModel) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    // The splash screen covers this until the session is resolved
    val start = startDestination ?: return

    // A deep link must not skip sign-in, so it is honoured only when the app starts on Home.
    // This runs before the NavHost is created, which is when it reads the launching intent.
    val activity = LocalActivity.current
    if (start != Screen.Home) activity?.intent?.data = null

    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val showConsentPrompt by viewModel.showConsentPrompt.collectAsStateWithLifecycle()

    LaunchedEffect(navigator) {
        viewModel.redirects.collect { navigator.clearBackStackAndNavigateTo(it) }
    }

    AppNavigationSuite(
        navController = navController,
        currentDestination = currentDestination,
        visible = currentDestination.showsBottomBar()
    ) {
        // Screens own their insets (each has its own Scaffold/top bar); the suite only adds the navigation
        AppNavGraph(
            navigator = navigator,
            navController = navController,
            startDestination = start
        )
    }

    // Asked once the user is inside the app, never over the sign-in screens
    if (showConsentPrompt && currentDestination.showsBottomBar()) {
        ConsentDialog(onAnswer = viewModel::onConsentAnswered)
    }
}

@Composable
private fun ConsentDialog(onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        // Answering is required, so tapping outside does not dismiss it
        onDismissRequest = {},
        title = { Text(stringResource(R.string.consent_title, stringResource(R.string.app_name))) },
        text = { Text(stringResource(R.string.consent_message)) },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.consent_allow)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.consent_decline)) } }
    )
}
