package com.asadraza.streamflix.core.data.util

import com.asadraza.streamflix.core.common.result.Result
import com.asadraza.streamflix.core.data.mapper.toDataErrorType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Wraps a one-shot operation: emits Loading, then Success with the block's value, or Error with
 * the mapped failure. The block may also return its own Error for rule violations.
 */
internal fun <T> operationFlow(dispatcher: CoroutineDispatcher, block: suspend () -> Result<T>): Flow<Result<T>> =
    flow {
        emit(Result.Loading())
        emit(block())
    }
        .catch { emit(Result.Error(it, it.toDataErrorType())) }
        .flowOn(dispatcher)

/**
 * Turns a stream of values into Loading, then Success per value. If the stream fails, the error
 * carries the last good value so the UI can keep showing it.
 */
internal fun <T> Flow<T>.asResultFlow(): Flow<Result<T>> = flow {
    var lastKnown: T? = null
    emit(Result.Loading())
    emitAll(
        this@asResultFlow
            .map<T, Result<T>> { value ->
                lastKnown = value
                Result.Success(value)
            }
            .catch { error -> emit(Result.Error(error, error.toDataErrorType(), lastKnown)) }
    )
}
