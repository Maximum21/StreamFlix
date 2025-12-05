package com.asadraza.streamflix

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AppStateProvider
 */
@Singleton
class AppStateProviderImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AppStateProvider {

    private val _isReadyFlow = MutableStateFlow(false)
    override val isReadyFlow: Flow<Boolean> = _isReadyFlow.asStateFlow()

    override val isReady: Boolean
        get() = _isReadyFlow.value

    init {
        checkAppReadiness()
    }

    /**
     * Check if the app is ready to be displayed
     */
    private fun checkAppReadiness() {
        try {
            // Check Firebase Auth state
            // The auth object being non-null means Firebase is initialized
            val authInitialized = auth != null

            if (authInitialized) {
                Timber.d("App is ready")
                _isReadyFlow.value = true
            } else {
                Timber.w("App initialization incomplete")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking app readiness")
            _isReadyFlow.value = true
        }
    }

    /**
     * Mark the app as ready
     * Can be called after completing initialization tasks
     */
    fun setReady() {
        Timber.d("App marked as ready")
        _isReadyFlow.value = true
    }

    /**
     * Mark the app as not ready
     * Useful for re-initialization scenarios
     */
    fun setNotReady() {
        Timber.d("App marked as not ready")
        _isReadyFlow.value = false
    }
}