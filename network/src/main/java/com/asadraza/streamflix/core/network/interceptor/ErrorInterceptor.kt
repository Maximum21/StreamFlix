package com.asadraza.streamflix.core.network.interceptor

import com.asadraza.streamflix.core.common.error.NetworkError
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps HTTP errors to domain errors
 *
 * WHY: Centralized error mapping (addresses feedback)
 * WHEN: After API response received
 * WHERE: OkHttp interceptor chain
 * HOW: Throws domain-specific errors
 */
class ErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            val response = chain.proceed(request)

            // Handle error status codes
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                response.close()

                throw when (response.code) {
                    in 400..499 -> NetworkError.ServerError(
                        code = response.code,
                        message = "Client error: ${response.code}"
                    )
                    in 500..599 -> NetworkError.ServerError(
                        code = response.code,
                        message = "Server error: ${response.code}"
                    )
                    else -> NetworkError.Unknown("HTTP ${response.code}")
                }
            }

            return response

        } catch (e: Exception) {
            // Map exceptions to domain errors
            e.printStackTrace()
            throw when (e) {
                is UnknownHostException -> NetworkError.NoInternet
                is SocketTimeoutException -> NetworkError.Timeout
                is NetworkError -> e // Already a NetworkError
                else -> NetworkError.Unknown(e.message ?: "Unknown error")
            }
        }
    }
}