package com.asadraza.streamflix.core.data.analytics

import android.os.Bundle
import com.asadraza.streamflix.core.domain.analytics.AnalyticsEvent
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

/**
 * Collection is switched on and off by the app according to the user's consent
 * (`setAnalyticsCollectionEnabled`); until then Firebase drops everything it is given.
 */
class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {
    override fun log(event: AnalyticsEvent) {
        val params = Bundle().apply { event.params.forEach { (key, value) -> putString(key, value) } }
        firebaseAnalytics.logEvent(event.name, params)
    }
}
