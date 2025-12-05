package com.asadraza.streamflix.di

import android.content.Context
import com.asadraza.streamflix.AppStateProvider
import com.asadraza.streamflix.AppStateProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * AppModule - Provides app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide Application Context
     * Safe to inject anywhere in the app
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context

    /**
     * Provide IO Dispatcher for IO operations
     * Use for: Network calls, database operations, file operations
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provide Default Dispatcher for CPU-intensive work
     * Use for: Heavy computations, data processing
     */
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provide Main Dispatcher for UI updates
     * Use for: UI updates, navigation
     */
    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provide Main Immediate Dispatcher for immediate UI updates
     * Use for: Critical UI updates that can't be delayed
     */
    @Provides
    @Singleton
    @MainImmediateDispatcher
    fun provideMainImmediateDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}

/**
 * Module for binding interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    @Singleton
    abstract fun bindAppStateProvider(
        impl: AppStateProviderImpl
    ): AppStateProvider
}

// ══════════════════════════════════════════════════════════
//  Dispatcher Qualifiers
// ══════════════════════════════════════════════════════════

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainImmediateDispatcher