package ccc.client.api

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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

    @Test
    fun `capabilities sends authorization header when token set`() = runBlocking {
        ApiClient.setToken("caps-token")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"service\":\"ccc-freecad-remote\"," +
                        "\"api_version\":\"v1\"," +
                        "\"time_utc\":\"2024-01-01T00:00:00Z\"," +
                        "\"auth\":{\"required\":true,\"scheme\":\"bearer\"}," +
                        "\"version\":{\"name\":\"ccc\",\"version\":\"0.1.0\",\"git_sha\":null}," +
                        "\"freecad\":{\"version\":\"1.0\"}," +
                        "\"session\":{\"pid\":1234}," +
                        "\"capabilities\":{" +
                        "\"import_formats\":[\"fcstd\"]," +
                        "\"export_formats\":[\"fcstd\",\"stl\"]," +
                        "\"commands\":[\"open_new_doc\"]," +
                        "\"limits\":{\"max_upload_mb\":200}," +
                        "\"features\":{\"model_browser\":true}" +
                        "}" +
                        "}"
                )
        )

        ApiClient.api.capabilities()

        val request = server.takeRequest()
        assertEquals("Bearer caps-token", request.getHeader("Authorization"))
    }

    @Test
    fun `download export adds authorization header when token set`() {
        ApiClient.setToken("download-token")
        server.enqueue(MockResponse().setResponseCode(200).setBody("payload"))

        ApiClient.downloadExport("/api/v1/exports/exp123/download").close()

        val request = server.takeRequest()
        assertEquals("Bearer download-token", request.getHeader("Authorization"))
    }

    @Test
    fun `download export omits authorization header when token blank`() {
        ApiClient.setToken("")
        server.enqueue(MockResponse().setResponseCode(200).setBody("payload"))

        ApiClient.downloadExport("/api/v1/exports/exp123/download").close()

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `commands and command detail parse metadata`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"commands\":[" +
                        "{" +
                        "\"name\":\"open_new_doc\"," +
                        "\"description\":\"Create a new document.\"," +
                        "\"args_schema\":{\"type\":\"object\",\"properties\":{}}," +
                        "\"returns\":\"message\"," +
                        "\"tags\":[\"session\"]" +
                        "}" +
                        "]" +
                        "}"
                )
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"name\":\"open_new_doc\"," +
                        "\"description\":\"Create a new document.\"," +
                        "\"args_schema\":{\"type\":\"object\",\"properties\":{}}," +
                        "\"returns\":\"message\"," +
                        "\"tags\":[\"session\"]" +
                        "}"
                )
        )

        val commands = ApiClient.api.commands()
        val detail = ApiClient.api.command("open_new_doc")

        assertEquals(1, commands.commands?.size)
        assertEquals("open_new_doc", commands.commands?.firstOrNull()?.name)
        assertEquals("Create a new document.", commands.commands?.firstOrNull()?.description)
        assertEquals("open_new_doc", detail.name)
        assertEquals("message", detail.returns)
        assertEquals("session", detail.tags?.firstOrNull())
    }

    @Test
    fun `parses export response from exec result`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"ok\":true," +
                        "\"result\":{" +
                        "\"ok\":true," +
                        "\"export_id\":\"exp123\"," +
                        "\"format\":\"stl\"," +
                        "\"filename\":\"export_exp123.stl\"," +
                        "\"path\":\"/tmp/export_exp123.stl\"," +
                        "\"download_url\":\"/api/v1/exports/exp123/download\"," +
                        "\"size\":321," +
                        "\"created_utc\":\"2024-02-02T00:00:00Z\"" +
                        "}" +
                        "}"
                )
        )

        val response = ApiClient.api.execCommand(ExecCommandRequest("export_current_doc", mapOf("format" to "stl")))
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ExportResponse::class.java)
        val export = parseExportResponse(response.result, adapter)

        assertNotNull(export)
        assertEquals("exp123", export?.exportId)
        assertEquals("stl", export?.format)
        assertEquals("/api/v1/exports/exp123/download", export?.downloadUrl)
        assertEquals("2024-02-02T00:00:00Z", export?.createdUtc)
    }

    @Test
    fun `capabilities parse response and omit auth header when token blank`() = runBlocking {
        ApiClient.setToken("")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"service\":\"ccc-freecad-remote\"," +
                        "\"api_version\":\"v1\"," +
                        "\"time_utc\":\"2024-01-01T00:00:00Z\"," +
                        "\"auth\":{\"required\":true,\"scheme\":\"bearer\"}," +
                        "\"version\":{\"name\":\"ccc\",\"version\":\"0.1.0\",\"git_sha\":null}," +
                        "\"freecad\":{\"version\":\"1.0\"}," +
                        "\"session\":{\"pid\":1234}," +
                        "\"capabilities\":{" +
                        "\"import_formats\":[\"fcstd\"]," +
                        "\"export_formats\":[\"stl\"]," +
                        "\"commands\":[\"open_new_doc\",\"recompute\"]," +
                        "\"limits\":{\"max_upload_mb\":200}," +
                        "\"features\":{\"model_browser\":true}" +
                        "}" +
                        "}"
                )
        )

        val response = ApiClient.api.capabilities()

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("ccc-freecad-remote", response.service)
        assertEquals("v1", response.apiVersion)
        assertEquals("fcstd", response.capabilities?.importFormats?.firstOrNull())
        assertEquals(2, response.capabilities?.commands?.size)
        assertEquals(true, response.capabilities?.features?.get("model_browser"))
    }

    @Test
    fun `exec sends authorization header when token set`() = runBlocking {
        ApiClient.setToken("exec-token")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true,\"request_id\":\"req-1\",\"result\":{\"message\":\"ok\"}}")
        )

        ApiClient.api.execCommand(ExecCommandRequest("open_new_doc", mapOf("path" to "/tmp/doc.fcstd")))

        val request = server.takeRequest()
        assertEquals("/api/v1/commands/exec", request.path)
        assertEquals("Bearer exec-token", request.getHeader("Authorization"))
        assertEquals(true, request.body.readUtf8().contains("\"command\":\"open_new_doc\""))
    }

    @Test
    fun `exec does not send authorization header when token blank`() = runBlocking {
        ApiClient.setToken("")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true,\"request_id\":\"req-1\",\"result\":{\"message\":\"ok\"}}")
        )

        ApiClient.api.execCommand(ExecCommandRequest("recompute"))

        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `parses exec response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true,\"request_id\":\"req-99\",\"result\":{\"message\":\"ok\"}}")
        )

        val response = ApiClient.api.execCommand(ExecCommandRequest("fit_all"))

        assertEquals(true, response.ok)
        assertEquals("req-99", response.requestId)
    }

    @Test
    fun `upload sends authorization header when token set`() = runBlocking {
        ApiClient.setToken("upload-token")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"ok\":true," +
                        "\"file_id\":\"file_123\"," +
                        "\"path\":\"/tmp/file_123\"," +
                        "\"filename\":\"model.stl\"," +
                        "\"size\":123," +
                        "\"mime\":\"application/sla\"" +
                        "}"
                )
        )

        val body = "solid".toRequestBody("application/sla".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "model.stl", body)
        ApiClient.api.upload(part)

        val request = server.takeRequest()
        assertEquals("/api/v1/files/upload", request.path)
        assertEquals("Bearer upload-token", request.getHeader("Authorization"))
        val contentType = request.getHeader("Content-Type") ?: ""
        assertEquals(true, contentType.contains("multipart/form-data"))
    }

    @Test
    fun `parses upload response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{" +
                        "\"ok\":true," +
                        "\"file_id\":\"file_abc\"," +
                        "\"path\":\"/uploads/file_abc\"," +
                        "\"filename\":\"model.step\"," +
                        "\"size\":456," +
                        "\"mime\":\"application/step\"," +
                        "\"sha256\":\"deadbeef\"" +
                        "}"
                )
        )

        val body = "data".toRequestBody("application/step".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "model.step", body)
        val response = ApiClient.api.upload(part)

        assertEquals(true, response.ok)
        assertEquals("file_abc", response.fileId)
        assertEquals("/uploads/file_abc", response.path)
        assertEquals("model.step", response.filename)
        assertEquals(456L, response.size)
        assertEquals("application/step", response.mime)
    }
}
