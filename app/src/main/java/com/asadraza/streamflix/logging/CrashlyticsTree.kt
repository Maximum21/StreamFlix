package com.asadraza.streamflix.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release logging: warnings and errors become Crashlytics breadcrumbs, and throwables logged at
 * those levels are recorded as non-fatal exceptions. Verbose and debug output is dropped.
 */
class CrashlyticsTree(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
) : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log("${tag ?: "App"}: $message")
        if (t != null) crashlytics.recordException(t)
    }
}
