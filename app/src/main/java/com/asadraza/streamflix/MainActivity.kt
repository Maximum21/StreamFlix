package com.asadraza.streamflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.app.StreamFlixApp
import com.asadraza.streamflix.core.ui.theme.StreamFlixTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity - Single Activity Architecture
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appState: AppStateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep splash screen visible until app is ready
        splashScreen.setKeepOnScreenCondition {
            !appState.isReady
        }

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set content
        setContent {
            StreamFlixTheme {
                val isReady by appState.isReadyFlow.collectAsStateWithLifecycle(false)

                if (isReady) {
                    StreamFlixApp()
                }
            }
        }
    }
}

/**
 * AppStateProvider - Provides app-level state
 * Injected to check if the app is ready to be displayed
 */
interface AppStateProvider {
    val isReady: Boolean
    val isReadyFlow: kotlinx.coroutines.flow.Flow<Boolean>
}