package com.asadraza.streamflix.core.network.utils

import com.asadraza.streamflix.core.common.error.NetworkError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(block: suspend () -> T): T {
    return try {
        block()
    }catch (e: HttpException){
        throw when(e.code()){
            401, 403 -> NetworkError.ServerError(e.code(), "Authentication failed")
            404 -> NetworkError.ServerError(e.code(), "Resource not found")
            429 -> NetworkError.RateLimitExceeded
            in 500..599 -> NetworkError.ServerError(e.code(), "Server Error")
            else -> NetworkError.ServerError(e.code(), "HTTP ${e.code()}")
        }
    } catch (e: UnknownHostException) {
        throw NetworkError.NoInternet          // DNS failed → almost always no connection
    } catch (e: SocketTimeoutException) {
        throw NetworkError.Timeout
    } catch (e: SerializationException) {
        throw NetworkError.ParsingError        // JSON didn't match the DTO
    } catch (e: IOException) {
        throw NetworkError.NoInternet          // other connectivity failures
    } catch (e: NetworkError) {
        throw e                                // already typed — pass through
    } catch (e: Exception) {
        throw NetworkError.Unknown(e.message ?: "Unknown error")
    }
}