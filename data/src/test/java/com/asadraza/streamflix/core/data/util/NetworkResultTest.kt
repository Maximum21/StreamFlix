package com.asadraza.streamflix.core.data.util

import com.asadraza.streamflix.core.common.result.ErrorType
import com.asadraza.streamflix.core.common.result.Result
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {
    @Test
    fun `emits loading then success`() = runTest {
        val emissions = networkResult { "data" }.toList()

        assertTrue(emissions[0] is Result.Loading)
        assertEquals(Result.Success("data"), emissions[1])
    }

    @Test
    fun `maps failures to error types`() = runTest {
        val emissions = networkResult<String> { throw UnknownHostException() }.toList()

        val error = emissions.last() as Result.Error
        assertTrue(error.type is ErrorType.Network)
    }

    @Test
    fun `lets cancellation through instead of reporting it as an error`() = runTest {
        var reportedAsError = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            networkResult<String> { awaitCancellation() }.collect { if (it is Result.Error) reportedAsError = true }
        }

        job.cancel()
        job.join()

        assertFalse(reportedAsError)
    }
}
