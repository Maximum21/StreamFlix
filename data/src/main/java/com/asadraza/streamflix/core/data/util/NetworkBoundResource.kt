package com.asadraza.streamflix.core.data.util

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.common.result.toErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Enhanced Network Bound Resource Pattern
 *
 * @param query Database query that returns Flow (can be nullable)
 * @param fetch Network call (suspend function)
 * @param saveFetchResult Saves network result to database
 * @param shouldFetch Whether to fetch from network
 * @param onFetchFailed Error handling callback
 */
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType?>,  
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { true },
    crossinline onFetchFailed: (Throwable) -> Unit = { }
): Flow<Result<ResultType>> = flow {

    // Step 1: Get initial cached data (can be null)
    val cachedData = query().first()

    // Step 2: Emit Loading with cached data (stale data support)
    emit(Result.Loading(data = cachedData))

    // Step 3: Decide whether to fetch
    if (shouldFetch(cachedData)) {

        // Step 4: Try to fetch fresh data
        try {
            val freshData = fetch()
            saveFetchResult(freshData)

            // Step 5: Emit fresh data from database (single source of truth)
            emitAll(query().map { data ->
                if (data != null) {
                    Result.Success(data)
                } else {
                    // Rare case: saved but query returns null
                    Result.Error(
                        exception = IllegalStateException("Data not found after save"),
                        type = com.asadraza.streamflix.core.common.result.ErrorType.Unknown(
                            IllegalStateException("Data not found after save")
                        ),
                        data = null
                    )
                }
            })

        } catch (throwable: Throwable) {
            // Step 6: Handle network error
            onFetchFailed(throwable)

            // Emit error with cached data (keeps showing stale data)
            emit(
                Result.Error(
                    exception = throwable,
                    type = throwable.toErrorType(),
                    data = cachedData  // Keep showing cached data on error
                )
            )
        }

    } else {
        // Step 7: Cache is valid, just emit cached data
        emitAll(query().map { data ->
            if (data != null) {
                Result.Success(data)
            } else {
                Result.Error(
                    exception = IllegalStateException("No data available"),
                    type = com.asadraza.streamflix.core.common.result.ErrorType.Unknown(
                        IllegalStateException("No data available")
                    ),
                    data = null
                )
            }
        })
    }
}

/**
 * Simplified version for operations that don't need caching
 * Directly emits network result
 */
inline fun <T> networkResult(
    crossinline fetch: suspend () -> T
): Flow<Result<T>> = flow {
    emit(Result.Loading())

    try {
        val data = fetch()
        emit(Result.Success(data))
    } catch (throwable: Throwable) {
        emit(
            Result.Error(
                exception = throwable,
                type = throwable.toErrorType(),
                data = null
            )
        )
    }
}