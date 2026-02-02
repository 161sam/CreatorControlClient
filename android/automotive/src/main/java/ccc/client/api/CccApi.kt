package ccc.client.api

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CccApi {
    @GET("/api/v1/healthz")
    suspend fun healthz(): HealthzResponse

    @GET("/api/v1/version")
    suspend fun version(): VersionResponse

    @GET("/api/v1/info")
    suspend fun info(): InfoResponse

    @GET("/api/v1/capabilities")
    suspend fun capabilities(): CapabilitiesResponse

    @GET("/api/v1/commands")
    suspend fun commands(): CommandsResponse

    @GET("/api/v1/commands/{name}")
    suspend fun command(@Path("name") name: String): CommandMeta

    @POST("/api/v1/commands/exec")
    suspend fun execCommand(@Body request: ExecCommandRequest): ExecCommandResponse

    @POST("/api/v1/commands/exec")
    suspend fun execCommandRaw(@Body body: RequestBody): ExecCommandResponse
}
