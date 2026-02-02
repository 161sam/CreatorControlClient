package ccc.client.api

import com.squareup.moshi.Json

data class HealthzResponse(
    val ok: Boolean
)

data class VersionResponse(
    val name: String,
    val version: String,
    @Json(name = "git_sha")
    val gitSha: String?
)

data class InfoAuthResponse(
    val required: Boolean,
    val scheme: String?
)

data class InfoResponse(
    val service: String?,
    @Json(name = "api_version")
    val apiVersion: String?,
    val auth: InfoAuthResponse?,
    @Json(name = "time_utc")
    val timeUtc: String?,
    val version: VersionResponse?
)
