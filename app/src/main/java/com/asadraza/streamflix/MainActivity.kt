package com.asadraza.streamflix

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.app.AppViewModel
import com.asadraza.streamflix.app.StreamFlixApp
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.core.ui.theme.StreamFlixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate; the splash stays up until the session is resolved
        installSplashScreen().setKeepOnScreenCondition { appViewModel.startDestination.value == null }

        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Transparent system bars whose icons follow the active scheme
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = barStyle(darkTheme),
                    navigationBarStyle = barStyle(darkTheme)
                )
                onDispose {}
            }

            StreamFlixTheme(darkTheme = darkTheme) {
                StreamFlixApp(appViewModel)
            }
        }
    }
}

private fun barStyle(darkTheme: Boolean): SystemBarStyle = if (darkTheme) {
    SystemBarStyle.dark(Color.TRANSPARENT)
} else {
    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
}
