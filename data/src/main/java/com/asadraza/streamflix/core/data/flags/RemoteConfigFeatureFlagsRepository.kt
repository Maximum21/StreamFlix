package com.asadraza.streamflix.core.data.flags

import com.asadraza.streamflix.core.domain.repository.FeatureFlagsRepository
import com.asadraza.streamflix.core.model.settings.FeatureFlags
import com.google.firebase.FirebaseException
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Flags come from Remote Config. The defaults are applied locally, so a first launch without a
 * network behaves exactly like the compiled-in values, and real-time updates reach a running app.
 */
class RemoteConfigFeatureFlagsRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : FeatureFlagsRepository {
    init {
        remoteConfig.setDefaultsAsync(mapOf(KEY_SHOW_HERO_BANNER to FeatureFlags().showHeroBanner))
    }

    override val flags: Flow<FeatureFlags> = callbackFlow {
        trySend(read())
        val registration = remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    remoteConfig.activate().addOnCompleteListener { trySend(read()) }
                }

                // Listening is best effort: values still refresh on the next explicit fetch
                override fun onError(error: FirebaseRemoteConfigException) = Unit
            }
        )
        awaitClose { registration.remove() }
    }

    override suspend fun refresh() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (ignored: FirebaseException) {
            // Offline or throttled: keep the values already active
        }
    }

    private fun read() = FeatureFlags(showHeroBanner = remoteConfig.getBoolean(KEY_SHOW_HERO_BANNER))

    private companion object {
        const val KEY_SHOW_HERO_BANNER = "show_hero_banner"
    }
}
