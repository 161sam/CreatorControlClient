package ccc.client.api

import retrofit2.http.GET
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
}
