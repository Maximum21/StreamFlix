package com.asadraza.streamflix.core.network.interceptor

import java.io.IOException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries idempotent requests that fail with an IOException, 429 or a 5xx response, using
 * exponential backoff with jitter. A Retry-After header (seconds) is honoured but capped, so a
 * misbehaving server cannot park a request thread for long.
 */
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    private val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    private val random: Random = Random.Default,
    private val sleep: (Long) -> Unit = Thread::sleep
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in IDEMPOTENT_METHODS) return chain.proceed(request)

        var attempt = 0
        while (true) {
            var response: Response? = null
            var failure: IOException? = null
            try {
                response = chain.proceed(request)
            } catch (e: IOException) {
                failure = e
            }

            val retryable = failure != null || response?.code.let { it == TOO_MANY_REQUESTS || it in SERVER_ERRORS }
            if (!retryable || attempt >= maxRetries || chain.call().isCanceled()) {
                return response ?: throw failure ?: IOException("Request failed")
            }

            val delayMs = delayFor(attempt, response?.header("Retry-After"))
            response?.close()
            pause(delayMs)
            attempt++
        }
    }

    private fun delayFor(attempt: Int, retryAfter: String?): Long {
        val serverDelayMs = retryAfter?.trim()?.toLongOrNull()?.times(MILLIS_PER_SECOND)
        if (serverDelayMs != null) return serverDelayMs.coerceIn(0, maxDelayMs)

        val ceiling = (baseDelayMs shl attempt).coerceAtMost(maxDelayMs)
        // Equal jitter: at least half the backoff, so retries spread out without collapsing to zero
        return ceiling / 2 + random.nextLong(ceiling / 2 + 1)
    }

    private fun pause(delayMs: Long) {
        try {
            sleep(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while waiting to retry", e)
        }
    }

    private companion object {
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_BASE_DELAY_MS = 500L
        const val DEFAULT_MAX_DELAY_MS = 5_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val TOO_MANY_REQUESTS = 429
        val SERVER_ERRORS = 500..599
        val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
