package ccc.client.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ApiClientInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        ApiClient.setBaseUrlForTests(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun addsAuthorizationHeaderWhenTokenIsSet() = runBlocking {
        ApiClient.setToken("abc")
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true}")
        )

        ApiClient.api.healthz()

        val request = server.takeRequest()
        assertEquals("Bearer abc", request.getHeader("Authorization"))
    }

    @Test
    fun omitsAuthorizationHeaderWhenTokenIsNull() = runBlocking {
        ApiClient.setToken(null)
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true}")
        )

        ApiClient.api.healthz()

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
    }
}
