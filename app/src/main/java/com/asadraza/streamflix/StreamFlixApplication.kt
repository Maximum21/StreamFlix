package com.asadraza.streamflix

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.asadraza.streamflix.core.domain.usecase.flags.RefreshFeatureFlagsUseCase
import com.asadraza.streamflix.core.domain.usecase.settings.ObserveAnalyticsConsentUseCase
import com.asadraza.streamflix.logging.CrashlyticsTree
import com.asadraza.streamflix.messaging.StreamFlixMessagingService
import com.asadraza.streamflix.security.installAppCheckProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import timber.log.Timber

@HiltAndroidApp
class StreamFlixApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject lateinit var observeAnalyticsConsent: ObserveAnalyticsConsentUseCase

    @Inject lateinit var refreshFeatureFlags: RefreshFeatureFlagsUseCase

    // Lives as long as the process; everything launched here is cheap start-up housekeeping
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        installAppCheckProvider()

        // Debug builds log to Logcat; release builds report to Crashlytics instead
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashlyticsTree())

        StreamFlixMessagingService.ensureChannel(this)

        // Analytics is off by default (manifest) and follows the stored consent from here on
        observeAnalyticsConsent()
            .onEach { firebaseAnalytics.setAnalyticsCollectionEnabled(it == true) }
            .launchIn(applicationScope)
        applicationScope.launch { refreshFeatureFlags() }
    }

    // Posters are the app's heaviest content, so the loader gets explicit, bounded caches
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve(IMAGE_CACHE_DIR).absolutePath.toPath())
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.20
        const val IMAGE_CACHE_DIR = "image_cache"
        const val DISK_CACHE_BYTES = 100L * 1024 * 1024
    }
}
