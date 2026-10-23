package com.asadraza.streamflix.core.network.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthInterceptorTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `adds a bearer authorization header to every request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor("token-123")).build()

        client.newCall(Request.Builder().url(server.url("/movie")).build()).execute().close()

        assertEquals("Bearer token-123", server.takeRequest().getHeader("Authorization"))
    }
}
