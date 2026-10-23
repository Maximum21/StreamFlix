package com.asadraza.streamflix.core.data.di

import com.asadraza.streamflix.core.data.analytics.FirebaseAnalyticsTracker
import com.asadraza.streamflix.core.data.flags.RemoteConfigFeatureFlagsRepository
import com.asadraza.streamflix.core.data.repository.AuthRepositoryImpl
import com.asadraza.streamflix.core.data.repository.CacheRepositoryImpl
import com.asadraza.streamflix.core.data.repository.MovieRepositoryImpl
import com.asadraza.streamflix.core.data.repository.MyListRepositoryImpl
import com.asadraza.streamflix.core.data.repository.ProfileRepositoryImpl
import com.asadraza.streamflix.core.data.repository.WatchHistoryRepositoryImpl
import com.asadraza.streamflix.core.data.source.DataStoreSearchHistoryRepository
import com.asadraza.streamflix.core.data.source.DataStoreSettingsRepository
import com.asadraza.streamflix.core.data.source.FcmNotificationsRepository
import com.asadraza.streamflix.core.domain.analytics.AnalyticsTracker
import com.asadraza.streamflix.core.domain.repository.AuthRepository
import com.asadraza.streamflix.core.domain.repository.CacheRepository
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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dependency Injection module for repositories and data sources
 *
 * @Binds:
 * - More efficient than @Provides for simple interface-to-impl binding
 * - Tells Hilt When someone asks for MovieCatalogRepository, give MovieRepositoryImpl
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    //  Movie Repository Bindings
    @Binds
    abstract fun bindMovieCatalogRepository(movieRepositoryImpl: MovieRepositoryImpl): MovieCatalogRepository

    @Binds
    abstract fun bindMovieDetailRepository(movieRepositoryImpl: MovieRepositoryImpl): MovieDetailRepository

    @Binds
    abstract fun bindMovieSearchRepository(movieRepositoryImpl: MovieRepositoryImpl): MovieSearchRepository

    // Auth Repository Binding
    @Binds
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    // Profile Repository Binding
    @Binds
    abstract fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindCacheRepository(impl: CacheRepositoryImpl): CacheRepository

    @Binds
    abstract fun bindMyListRepository(impl: MyListRepositoryImpl): MyListRepository

    @Binds
    abstract fun bindWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository

    @Binds
    abstract fun bindSearchHistoryRepository(impl: DataStoreSearchHistoryRepository): SearchHistoryRepository

    @Binds
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker

    @Binds
    abstract fun bindFeatureFlagsRepository(impl: RemoteConfigFeatureFlagsRepository): FeatureFlagsRepository

    @Binds
    abstract fun bindNotificationsRepository(impl: FcmNotificationsRepository): NotificationsRepository
}
