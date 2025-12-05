package com.asadraza.streamflix.core.network.interceptor

import com.asadraza.streamflix.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds API key to all requests
 *
 * Every API request
 * OkHttp interceptor chain
 * Modifies URL to add api_key parameter
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Add API key to URL
        val urlWithApiKey = originalUrl.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()

        val requestWithAuth = originalRequest.newBuilder()
            .url(urlWithApiKey)
            .build()

        return chain.proceed(requestWithAuth)
    }
}