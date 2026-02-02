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

    @Test
    fun `version sends authorization header when token set`() = runBlocking {
        ApiClient.setToken("abc123")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"name\":\"CreatorControlServer\",\"version\":\"0.1.0-dev\",\"git_sha\":null}")
        )

        ApiClient.api.version()

        val request = server.takeRequest()
        assertEquals("Bearer abc123", request.getHeader("Authorization"))
    }

    @Test
    fun `version and info do not send authorization header when token blank`() = runBlocking {
        ApiClient.setToken("")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"name\":\"CreatorControlServer\",\"version\":\"0.1.0-dev\",\"git_sha\":null}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"service\":\"CreatorControlServer\"," +
                        "\"api_version\":\"v1\"," +
                        "\"auth\":{\"required\":true,\"scheme\":\"bearer\"}," +
                        "\"time_utc\":\"2024-01-01T00:00:00Z\"," +
                        "\"version\":{\"name\":\"CreatorControlServer\",\"version\":\"0.1.0-dev\",\"git_sha\":null}" +
                        "}"
                )
        )

        ApiClient.api.version()
        ApiClient.api.info()

        val versionRequest = server.takeRequest()
        assertNull(versionRequest.getHeader("Authorization"))
        val infoRequest = server.takeRequest()
        assertNull(infoRequest.getHeader("Authorization"))
    }

    @Test
    fun `parses version and info responses`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"name\":\"CreatorControlServer\",\"version\":\"0.1.0-dev\",\"git_sha\":null}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"service\":\"CreatorControlServer\"," +
                        "\"api_version\":\"v1\"," +
                        "\"auth\":{\"required\":true,\"scheme\":\"bearer\"}," +
                        "\"time_utc\":\"2024-01-01T00:00:00Z\"," +
                        "\"version\":{\"name\":\"CreatorControlServer\",\"version\":\"0.1.0-dev\",\"git_sha\":null}" +
                        "}"
                )
        )

        val version = ApiClient.api.version()
        val info = ApiClient.api.info()

        assertEquals("CreatorControlServer", version.name)
        assertEquals("0.1.0-dev", version.version)
        assertNull(version.gitSha)

        assertEquals("CreatorControlServer", info.service)
        assertEquals("v1", info.apiVersion)
        assertEquals(true, info.auth?.required)
        assertEquals("bearer", info.auth?.scheme)
        assertEquals("2024-01-01T00:00:00Z", info.timeUtc)
        assertEquals("CreatorControlServer", info.version?.name)
    }
}
