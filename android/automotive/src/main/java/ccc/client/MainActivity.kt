package ccc.client

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
import ccc.client.api.InfoResponse
import ccc.client.api.VersionResponse
import ccc.client.ui.CccApp
import ccc.client.ui.HomeUiState
import ccc.client.ui.StatusLevel
import ccc.client.ui.theme.CccTheme
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MainActivity : ComponentActivity() {

    private val tag = "CCC"
    private var healthJob: Job? = null
    private var homeState by mutableStateOf(HomeUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppConfig.token.isNotBlank()) {
            ApiClient.setToken(AppConfig.token)
            Log.i(tag, "auth enabled")
        } else {
            ApiClient.setToken(null)
            Log.i(tag, "auth disabled")
        }
        Log.i(tag, "config mode=${AppConfig.mode} baseUrl=${AppConfig.baseUrl}")

        homeState = homeState.copy(detailsText = buildConnectionDetails())

        setContent {
            CccTheme {
                CccApp(
                    homeState = homeState,
                    diagnosticsProvider = { buildDiagnosticsText(homeState) },
                    onRetry = { runHealthCheck() },
                    onOpenBrowserActivity = {
                        startActivity(Intent(this, BrowserActivity::class.java))
                    }
                )
            }
        }

        runHealthCheck()
    }

    private fun runHealthCheck() {
        if (healthJob?.isActive == true) {
            return
        }

        homeState = homeState.copy(
            statusText = "Connecting…",
            statusLevel = StatusLevel.Neutral,
            messageLines = emptyList(),
            isLoading = true,
            lastAction = "healthz",
            lastActionSummary = "starting"
        )

        healthJob = lifecycleScope.launch {
            try {
                Log.i(tag, "healthz starting")
                val res = withContext(Dispatchers.IO) { ApiClient.api.healthz() }
                Log.i(tag, "healthz OK: $res")
                val messages = mutableListOf<String>()
                messages.add("healthz: ok=${res.ok}")
                val infoResult = fetchInfo()
                val infoLines = infoResult.infoLines
                var infoSummary = homeState.infoSummary
                infoResult.infoSummary?.let { infoSummary = it }

                var versionSummary = homeState.versionSummary
                val versionLine = infoResult.info?.version?.let { formatVersionLine(it) } ?: run {
                    val versionResult = fetchVersionLine()
                    versionResult.summary?.let { versionSummary = it }
                    versionResult.line
                }
                messages.add(versionLine)
                messages.addAll(infoLines)

                homeState = homeState.copy(
                    statusText = "OK",
                    statusLevel = StatusLevel.Ok,
                    messageLines = messages,
                    isLoading = false,
                    lastActionSummary = "healthz ok, ${messages.firstOrNull() ?: "no details"}",
                    infoSummary = infoSummary,
                    versionSummary = versionSummary
                )
            } catch (e: Exception) {
                Log.e(tag, "healthz failed", e)
                val errorMessage = formatErrorMessage(e)
                homeState = homeState.copy(
                    statusText = "ERROR",
                    statusLevel = StatusLevel.Error,
                    messageLines = listOf(errorMessage),
                    isLoading = false,
                    lastActionSummary = "healthz error: $errorMessage"
                )
            } finally {
                healthJob = null
            }
        }
    }

    private fun buildConnectionDetails(): String {
        val token = AppConfig.token
        val authText = if (token.isNotBlank()) {
            "auth=enabled token=${maskToken(token)}"
        } else {
            "auth=disabled"
        }
        return "mode=${AppConfig.mode}\nbaseUrl=${AppConfig.baseUrl}\n$authText"
    }

    private fun maskToken(token: String): String {
        val suffix = if (token.length <= 3) token else token.takeLast(3)
        return "****$suffix"
    }

    private suspend fun fetchVersionLine(): VersionResult {
        return try {
            val version = withContext(Dispatchers.IO) { ApiClient.api.version() }
            val line = formatVersionLine(version)
            VersionResult(line = line, summary = line)
        } catch (e: Exception) {
            VersionResult(line = "version: ERROR ${formatErrorMessage(e)}", summary = null)
        }
    }

    private suspend fun fetchInfo(): InfoResult {
        return try {
            val info = withContext(Dispatchers.IO) { ApiClient.api.info() }
            val infoLines = formatInfoLines(info)
            InfoResult(info = info, infoLines = infoLines, infoSummary = infoLines.joinToString("; "))
        } catch (e: Exception) {
            InfoResult(
                info = null,
                infoLines = listOf("info: ERROR ${formatErrorMessage(e)}"),
                infoSummary = null
            )
        }
    }

    private fun formatVersionLine(version: VersionResponse): String {
        val shaText = version.gitSha?.takeIf { it.isNotBlank() }?.let { " (sha=$it)" } ?: ""
        return "version: ${version.name} ${version.version}$shaText"
    }

    private fun formatInfoLines(info: InfoResponse): List<String> {
        val timeText = info.timeUtc ?: "<unknown>"
        val auth = info.auth
        val schemeText = auth?.scheme ?: "unknown"
        val requiredText = auth?.required?.toString() ?: "unknown"
        return listOf(
            "time_utc: $timeText",
            "auth: $schemeText required=$requiredText"
        )
    }

    private fun formatErrorMessage(error: Exception): String {
        return if (error is HttpException) {
            val code = error.code()
            if (code == 401) {
                return "Token missing/invalid — set CCC_TOKEN in build config."
            }
            val rawBody = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
            val bodyText = rawBody?.takeIf { it.isNotBlank() } ?: "<no body>"
            "HTTP $code: ${truncate(bodyText, 200)}"
        } else {
            val message = error.message ?: "unknown error"
            "${error.javaClass.simpleName}: $message"
        }
    }

    private fun truncate(input: String, maxLength: Int): String {
        return if (input.length <= maxLength) input else input.take(maxLength) + "…"
    }

    private fun buildDiagnosticsText(state: HomeUiState): String {
        val timestamp = ZonedDateTime.now().toString()
        val endpoints = listOf(
            "/api/v1/healthz",
            "/api/v1/info",
            "/api/v1/version"
        )
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"
        val messageText = state.messageLines.joinToString("\n").trim()
        return buildString {
            appendLine("CCC diagnostics")
            appendLine("timestamp: $timestamp")
            appendLine("status: ${state.statusText}")
            appendLine("app_version: $appVersion")
            appendLine("device: $deviceInfo")
            appendLine()
            appendLine("last_action: ${state.lastAction ?: "<none>"}")
            appendLine("last_action_summary: ${state.lastActionSummary ?: "<none>"}")
            appendLine()
            appendLine("server_info:")
            appendLine("  info: ${state.infoSummary}")
            appendLine("  version: ${state.versionSummary}")
            appendLine()
            appendLine("app_config:")
            state.detailsText.lines().forEach { line ->
                appendLine("  $line")
            }
            appendLine()
            appendLine("messages:")
            if (messageText.isBlank()) {
                appendLine("  <none>")
            } else {
                messageText.lines().forEach { line ->
                    appendLine("  $line")
                }
            }
            appendLine()
            appendLine("endpoints:")
            endpoints.forEach { endpoint ->
                appendLine("  $endpoint")
            }
        }
    }

    private data class InfoResult(
        val info: InfoResponse?,
        val infoLines: List<String>,
        val infoSummary: String?
    )

    private data class VersionResult(
        val line: String,
        val summary: String?
    )
}
