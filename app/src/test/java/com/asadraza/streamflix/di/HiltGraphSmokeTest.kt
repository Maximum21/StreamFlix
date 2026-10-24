package com.asadraza.streamflix.di

import com.asadraza.streamflix.core.common.coroutines.IoDispatcher
import com.asadraza.streamflix.core.data.di.FirebaseModule
import com.asadraza.streamflix.core.database.datasource.ILocalDataSource
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.FeatureFlagsRepository
import com.asadraza.streamflix.core.domain.repository.MovieCatalogRepository
import com.asadraza.streamflix.core.domain.repository.MovieDetailRepository
import com.asadraza.streamflix.core.domain.repository.MovieSearchRepository
import com.asadraza.streamflix.core.domain.repository.MyListRepository
import com.asadraza.streamflix.core.domain.repository.NotificationsRepository
import com.asadraza.streamflix.core.domain.repository.ProfileRepository
import com.asadraza.streamflix.core.domain.repository.SearchHistoryRepository
import com.asadraza.streamflix.core.domain.repository.SettingsRepository
import com.asadraza.streamflix.core.domain.repository.WatchHistoryRepository
import com.asadraza.streamflix.core.domain.usecase.auth.LoginUseCase
import com.asadraza.streamflix.core.network.datasource.IRemoteDataSource
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds the real Hilt graph (database, network, repositories, use cases) with only the Firebase
 * SDK instances replaced, so a missing or duplicate binding fails here as well as at compile time.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34])
class HiltGraphSmokeTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var authRepository: AuthRepository

    @Inject lateinit var profileRepository: ProfileRepository

    @Inject lateinit var catalogRepository: MovieCatalogRepository

    @Inject lateinit var detailRepository: MovieDetailRepository

    @Inject lateinit var searchRepository: MovieSearchRepository

    @Inject lateinit var remoteDataSource: IRemoteDataSource

    @Inject lateinit var localDataSource: ILocalDataSource

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var loginUseCase: LoginUseCase

    @Inject lateinit var myListRepository: MyListRepository

    @Inject lateinit var watchHistoryRepository: WatchHistoryRepository

    @Inject lateinit var searchHistoryRepository: SearchHistoryRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var notificationsRepository: NotificationsRepository

    @Inject lateinit var featureFlagsRepository: FeatureFlagsRepository

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun `the object graph resolves`() {
        listOf(
            authRepository,
            profileRepository,
            catalogRepository,
            detailRepository,
            searchRepository,
            remoteDataSource,
            localDataSource,
            ioDispatcher,
            loginUseCase,
            myListRepository,
            watchHistoryRepository,
            searchHistoryRepository,
            settingsRepository,
            notificationsRepository,
            featureFlagsRepository,
            analyticsTracker
        ).forEach { assertNotNull(it) }
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [FirebaseModule::class])
object FakeFirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = mockk(relaxed = true)
}
