package com.asadraza.streamflix.core.network.di

import com.asadraza.streamflix.core.common.util.Constants
import com.asadraza.streamflix.core.network.BuildConfig
import com.asadraza.streamflix.core.network.api.TmdbApiService
import com.asadraza.streamflix.core.network.interceptor.AuthInterceptor
import com.asadraza.streamflix.core.network.interceptor.RetryInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Hilt module for network dependencies
 * Centralized network setup
 * App startup
 * Singleton scope (lives for app lifetime)
 * Hilt provides these dependencies when requested
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // API might add new fields
        coerceInputValues = true // Handle null for non-nullable
        isLenient = true // Allow unquoted strings
    }

    /**
     * Provides logging interceptor
     * Debug network requests
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor(BuildConfig.TMDB_ACCESS_TOKEN)

    /**
     * Provides OkHttpClient with all interceptors
     * Centralized HTTP client configuration
     */
    @Provides
    @Singleton
    fun providesOkHttpClient(
        authInterceptor: AuthInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    /**
     * Provides API service
     */
    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }
}
