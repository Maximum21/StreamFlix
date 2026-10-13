package com.asadraza.streamflix.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor (
    private val maxRetries: Int = 3
): Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response = chain.proceed(chain.request())
        while(response.code == 429 && attempt < maxRetries){
            val retryAfterMs = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                ?: (1000L * (1 shl attempt))
            response.close()
            Thread.sleep(retryAfterMs)
            attempt++
            response = chain.proceed(chain.request())
        }
        return response
    }
}