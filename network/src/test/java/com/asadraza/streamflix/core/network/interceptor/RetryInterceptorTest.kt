package com.asadraza.streamflix.core.network.interceptor

import java.io.IOException
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {
    private lateinit var server: MockWebServer
    private val delays = mutableListOf<Long>()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(maxRetries: Int = 3) = OkHttpClient.Builder()
        .addInterceptor(
            RetryInterceptor(
                maxRetries = maxRetries,
                baseDelayMs = 100,
                maxDelayMs = 1_000,
                random = Random(1),
                sleep = { delays += it }
            )
        )
        .build()

    private fun get() = Request.Builder().url(server.url("/")).build()

    @Test
    fun `retries a 503 and returns the later success`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val response = client().newCall(get()).execute()

        assertEquals(200, response.code)
        assertEquals(2, server.requestCount)
        assertEquals(1, delays.size)
    }

    @Test
    fun `gives up after the retry limit and returns the last response`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }

        val response = client(maxRetries = 3).newCall(get()).execute()

        assertEquals(500, response.code)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `does not retry client errors`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val response = client().newCall(get()).execute()

        assertEquals(404, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `does not retry non idempotent requests`() {
        server.enqueue(MockResponse().setResponseCode(503))
        val post = Request.Builder().url(server.url("/")).post("{}".toRequestBody()).build()

        val response = client().newCall(post).execute()

        assertEquals(503, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `retries a dropped connection`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.enqueue(MockResponse().setResponseCode(200))

        val response = client().newCall(get()).execute()

        assertEquals(200, response.code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `rethrows the IOException when retries are exhausted`() {
        repeat(2) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)) }

        try {
            client(maxRetries = 1).newCall(get()).execute()
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun `honours Retry-After but caps it`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "3600"))
        server.enqueue(MockResponse().setResponseCode(200))

        client().newCall(get()).execute()

        assertEquals(listOf(1_000L, 1_000L), delays)
    }

    @Test
    fun `backoff grows and stays within the jitter window`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }
        server.enqueue(MockResponse().setResponseCode(200))

        client().newCall(get()).execute()

        // base 100ms doubling: ceilings 100, 200, 400; each delay is in [ceiling / 2, ceiling]
        listOf(100L, 200L, 400L).forEachIndexed { index, ceiling ->
            assertTrue("delay ${delays[index]} outside window for $ceiling", delays[index] in ceiling / 2..ceiling)
        }
    }
}
