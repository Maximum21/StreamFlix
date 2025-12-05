package com.asadraza.streamflix.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asadraza.streamflix.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AppViewModel - Manages app-level state
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    
    //  State
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        initializeApp()
    }

    
    //  Initialization
    private fun initializeApp() {
        viewModelScope.launch {
            try {
                Timber.d("Initializing app...")

                // Determine start destination based on auth state
                val startDestination = determineStartDestination()

                _appState.update { state ->
                    state.copy(
                        startDestination = startDestination,
                        isLoading = false
                    )
                }

                Timber.d("App initialized with start destination: ${startDestination::class.simpleName}")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing app")
                _appState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Determine start destination based on authentication state
     */
    private fun determineStartDestination(): Screen {
        val currentUser = auth.currentUser

        return when {
            // No user logged in → Login screen
            currentUser == null -> {
                Timber.d("No user logged in, starting with Login")
                Screen.Login
            }

            // User logged in but email not verified → Email Verification screen
            !currentUser.isEmailVerified -> {
                Timber.d("User email not verified, starting with EmailVerification")
                Screen.EmailVerification
            }

            // User logged in and verified → Home screen
            else -> {
                Timber.d("User authenticated, starting with Home")
                Screen.Home
            }
        }
    }

    
    //  Public API
    fun onScreenChanged(screen: Screen?) {
        _appState.update { it.copy(currentScreen = screen) }
        Timber.d("Screen changed to: ${screen?.let { it::class.simpleName }}")
    }

    fun onAuthStateChanged(isAuthenticated: Boolean) {
        viewModelScope.launch {
            val newDestination = if (isAuthenticated) {
                Screen.Home
            } else {
                Screen.Login
            }

            _appState.update { it.copy(startDestination = newDestination) }
            Timber.d("Auth state changed, new destination: ${newDestination::class.simpleName}")
        }
    }
}

/**
 * AppState - Represents the app-level state
 * Immutable data class for predictable state management
 */
data class AppState(
    val startDestination: Screen = Screen.Login,
    val currentScreen: Screen? = null,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)