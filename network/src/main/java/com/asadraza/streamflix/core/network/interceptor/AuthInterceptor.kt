package com.asadraza.streamflix.core.network.interceptor

import com.asadraza.streamflix.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds AccessToken to all requests headers
 *
 * Every API request
 * OkHttp interceptor chain
 * Adds new header with Authorization and Token
 */
class AuthInterceptor(
    private val accessToken: String
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithAuth = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(requestWithAuth)
    }
}