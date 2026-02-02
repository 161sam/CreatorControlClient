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

data class ExecCommandRequest(
    val command: String,
    val args: Map<String, Any?> = emptyMap(),
    @Json(name = "request_id")
    val requestId: String? = null
)

data class ExecCommandResponse(
    val ok: Boolean?,
    @Json(name = "request_id")
    val requestId: String?,
    val status: String?,
    val stdout: String?,
    val stderr: String?,
    val result: Any?
)

data class ExportResponse(
    val ok: Boolean?,
    @Json(name = "export_id")
    val exportId: String?,
    val format: String?,
    val filename: String?,
    val path: String?,
    @Json(name = "download_url")
    val downloadUrl: String?,
    val size: Long?,
    @Json(name = "created_utc")
    val createdUtc: String?
)

data class UploadResponse(
    val ok: Boolean?,
    @Json(name = "file_id")
    val fileId: String?,
    val path: String?,
    val filename: String?,
    val size: Long?,
    val mime: String?,
    val sha256: String?
)

data class ArgSpec(
    val type: String?,
    val required: Boolean,
    val defaultValue: Any?,
    val title: String?,
    val description: String?
)

data class ArgsSchema(
    val properties: Map<String, ArgSpec>
)

fun CommandMeta.parseArgsSchema(): ArgsSchema? {
    val schema = argsSchema ?: return null
    val properties = schema["properties"] as? Map<*, *> ?: return null
    val required = (schema["required"] as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
    val parsed = properties.mapNotNull { (key, value) ->
        val name = key as? String ?: return@mapNotNull null
        val spec = value as? Map<*, *> ?: return@mapNotNull null
        val type = spec["type"] as? String
        val defaultValue = spec["default"]
        val title = spec["title"] as? String
        val description = spec["description"] as? String
        name to ArgSpec(
            type = type,
            required = required.contains(name),
            defaultValue = defaultValue,
            title = title,
            description = description
        )
    }.toMap()
    return ArgsSchema(parsed)
}

fun parseExportResponse(result: Any?, adapter: com.squareup.moshi.JsonAdapter<ExportResponse>): ExportResponse? {
    if (result == null) {
        return null
    }
    val payload = if (result is Map<*, *>) {
        val nested = result["export"]
        if (nested is Map<*, *>) nested else result
    } else {
        result
    }
    return runCatching { adapter.fromJsonValue(payload) }.getOrNull()
}
