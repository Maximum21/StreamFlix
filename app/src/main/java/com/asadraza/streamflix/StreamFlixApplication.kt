package com.asadraza.streamflix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for StreamFlix
 */
@HiltAndroidApp
class StreamFlixApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }

    /**
     * Initialize Timber for logging
     * - Debug builds: Log everything
     * - Release builds: Only log warnings and errors
     */
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("StreamFlix application started in DEBUG mode")
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * Custom Timber tree for release builds
     * Filters out debug and verbose logs
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }
        }
    }
}