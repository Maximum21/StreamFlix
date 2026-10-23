package com.asadraza.streamflix.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {
    private val failure = IllegalStateException("boom")
    private val unknown = ErrorType.Unknown(failure)

    @Test
    fun `getOrNull returns data from success and stale data from other states`() {
        assertEquals(1, Result.Success(1).getOrNull())
        assertEquals(2, Result.Loading(data = 2).getOrNull())
        assertEquals(3, Result.Error(failure, unknown, data = 3).getOrNull())
        assertNull(Result.Loading<Int>().getOrNull())
        assertNull(Result.Error<Int>(failure, unknown).getOrNull())
    }

    @Test
    fun `getOrThrow throws the original failure or when nothing is available`() {
        assertEquals(1, Result.Success(1).getOrThrow())
        assertEquals(2, Result.Loading(data = 2).getOrThrow())
        assertThrows(IllegalStateException::class.java) { Result.Loading<Int>().getOrThrow() }
        val thrown = assertThrows(IllegalStateException::class.java) {
            Result.Error<Int>(failure, unknown).getOrThrow()
        }
        assertEquals(failure, thrown)
    }

    @Test
    fun `map transforms data in every state and keeps the rest`() {
        assertEquals(Result.Success(2), Result.Success(1).map { it + 1 })
        assertEquals(
            Result.Loading(data = 2, progress = 0.5f),
            Result.Loading(data = 1, progress = 0.5f).map { it + 1 }
        )
        assertEquals(Result.Error(failure, unknown, data = 2), Result.Error(failure, unknown, data = 1).map { it + 1 })
        assertNull(Result.Loading<Int>().map { it + 1 }.getOrNull())
    }

    @Test
    fun `callbacks only fire for their own state`() {
        val seen = mutableListOf<String>()
        Result.Success(1).onSuccess { seen += "success" }.onError { _, _ -> seen += "error" }
            .onLoading { _, _ -> seen += "loading" }
        Result.Error<Int>(failure, unknown).onSuccess { seen += "success" }.onError { type, throwable ->
            assertEquals(unknown, type)
            assertEquals(failure, throwable)
            seen += "error"
        }
        Result.Loading<Int>(progress = 0.2f).onLoading { _, progress ->
            assertEquals(0.2f, progress)
            seen += "loading"
        }

        assertEquals(listOf("success", "error", "loading"), seen)
    }

    @Test
    fun `state predicates`() {
        assertTrue(Result.Success(1).isSuccess())
        assertTrue(Result.Loading<Int>().isLoading())
        assertTrue(Result.Error<Int>(failure, unknown).isError())
        assertFalse(Result.Success(1).isError())
        assertTrue(Result.Success(1).hasData())
        assertFalse(Result.Loading<Int>().hasData())
        assertTrue(Result.Error(failure, unknown, data = 1).hasData())
    }

    @Test
    fun `error message falls back when the exception has none`() {
        assertEquals("boom", Result.Error<Int>(failure, unknown).message)
        assertEquals("Unknown error", Result.Error<Int>(RuntimeException(), unknown).message)
    }
}
