package ccc.client.api

import retrofit2.http.GET

interface CccApi {
    @GET("/api/v1/healthz")
    suspend fun healthz(): HealthzResponse
}
