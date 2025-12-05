package com.asadraza.streamflix.core.network.interceptor

import com.asadraza.streamflix.core.common.error.NetworkError
import com.asadraza.streamflix.core.common.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Rate limiting interceptor
 *
 * Prevents API rate limit errors
 * Tracks request timestamps, delays if needed
 */
class RateLimitInterceptor : Interceptor {

    private val requestTimestamps = mutableListOf<Long>()
    private val maxRequestsPerSecond = Constants.API_RATE_LIMIT_PER_SECOND

    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(requestTimestamps) {
            val now = System.currentTimeMillis()

            // Remove timestamps older than 1 second
            requestTimestamps.removeAll {
                now - it > TimeUnit.SECONDS.toMillis(1)
            }

            // If at limit, wait
            if (requestTimestamps.size >= maxRequestsPerSecond) {
                val oldestTimestamp = requestTimestamps.first()
                val waitTime = TimeUnit.SECONDS.toMillis(1) - (now - oldestTimestamp)

                if (waitTime > 0) {
                    runBlocking {
                        delay(waitTime)
                    }
                }

                // Clean up after waiting
                requestTimestamps.removeAll {
                    System.currentTimeMillis() - it > TimeUnit.SECONDS.toMillis(1)
                }
            }

            // Add current timestamp
            requestTimestamps.add(System.currentTimeMillis())
        }

        val response = chain.proceed(chain.request())

        // Handle 429 Too Many Requests
        if (response.code == 429) {
            response.close()
            throw NetworkError.RateLimitExceeded
        }

        return response
    }
}