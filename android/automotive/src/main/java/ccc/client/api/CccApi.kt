package ccc.client.api

import retrofit2.http.GET

interface CccApi {
    @GET("/api/v1/healthz")
    suspend fun healthz(): HealthzResponse

    @GET("/api/v1/version")
    suspend fun version(): VersionResponse

    @GET("/api/v1/info")
    suspend fun info(): InfoResponse
}
