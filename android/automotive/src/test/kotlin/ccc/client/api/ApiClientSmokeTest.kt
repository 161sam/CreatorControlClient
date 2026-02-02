package ccc.client.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ApiClientSmokeTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        ApiClient.setBaseUrlForTests(server.url("/").toString())
        ApiClient.setToken(null)
    }

    @After
    fun tearDown() {
        ApiClient.setToken(null)
        server.shutdown()
    }

    @Test
    fun `adds bearer token header when token is set`() = runBlocking {
        ApiClient.setToken("abc")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"ok\":true}"))

        ApiClient.api.healthz()

        val request = server.takeRequest()
        assertEquals("Bearer abc", request.getHeader("Authorization"))
    }

    @Test
    fun `does not send authorization header when token is blank`() = runBlocking {
        ApiClient.setToken(null)
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"ok\":true}"))

        ApiClient.api.healthz()

        val requestWithoutToken = server.takeRequest()
        assertNull(requestWithoutToken.getHeader("Authorization"))

        ApiClient.setToken("")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"ok\":true}"))

        ApiClient.api.healthz()

        val requestWithBlankToken = server.takeRequest()
        assertNull(requestWithBlankToken.getHeader("Authorization"))
    }

    @Test
    fun `parses healthz response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"ok\":true}"))

        val response = ApiClient.api.healthz()

        assertEquals(true, response.ok)
    }
}
