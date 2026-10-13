package com.asadraza.streamflix.di

import com.asadraza.streamflix.navigation.NavigationManager
import com.asadraza.streamflix.navigation.NavigationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NavigationModule - Provides navigation dependencies
 */
//@Module
//@InstallIn(SingletonComponent::class)
//abstract class NavigationModule {
//
//    /**
//     * Bind NavigationManager interface to implementation
//     */
//    @Binds
//    @Singleton
//    abstract fun bindNavigationManager(
//        navigationManagerImpl: NavigationManagerImpl
//    ): NavigationManager
//}

@Module
@InstallIn(SingletonComponent::class)
object NavigationProviderModule {

    @Provides
    @Singleton
    fun provideNavigationManager(): NavigationManagerImpl {
        return NavigationManagerImpl()
    }
}