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

data class CapabilitiesDetail(
    @Json(name = "import_formats")
    val importFormats: List<String>?,
    @Json(name = "export_formats")
    val exportFormats: List<String>?,
    val commands: List<String>?,
    val limits: Map<String, Any?>?,
    val features: Map<String, Any?>?
)

data class CapabilitiesResponse(
    val service: String?,
    @Json(name = "api_version")
    val apiVersion: String?,
    @Json(name = "time_utc")
    val timeUtc: String?,
    val auth: InfoAuthResponse?,
    val version: VersionResponse?,
    val freecad: Map<String, Any?>?,
    val session: Map<String, Any?>?,
    val capabilities: CapabilitiesDetail?
)

data class CommandMeta(
    val name: String?,
    val description: String?,
    @Json(name = "args_schema")
    val argsSchema: Map<String, Any?>?,
    val returns: String?,
    val tags: List<String>?,
    val safety: Map<String, Any?>?,
    val constraints: Map<String, Any?>?,
    val limits: Map<String, Any?>?
)

data class CommandsResponse(
    val commands: List<CommandMeta>?
)
