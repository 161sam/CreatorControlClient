package ccc.client

import android.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
import ccc.client.api.InfoResponse
import ccc.client.api.VersionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    private val tag = "CCC"
    private var healthJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val statusView = TextView(this).apply {
            text = "Connecting…"
            setTextColor(Color.GRAY)
            textSize = 24f
            setPadding(0, 0, 0, 16)
        }
        val detailsView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 16)
        }
        val messageView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 24)
        }
        val retryButton = Button(this).apply {
            text = "Retry"
            isEnabled = false
        }
        val copyButton = Button(this).apply {
            text = "Copy diagnostics"
            isEnabled = false
        }
        val openBrowserButton = Button(this).apply {
            text = "Open Browser"
            isEnabled = true
        }
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(retryButton)
            addView(copyButton)
            addView(openBrowserButton)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.START
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(statusView)
            addView(detailsView)
            addView(messageView)
            addView(buttonRow)
        }
        setContentView(layout)

        if (AppConfig.token.isNotBlank()) {
            ApiClient.setToken(AppConfig.token)
            Log.i(tag, "auth enabled")
        } else {
            Log.i(tag, "auth disabled")
        }
        Log.i(tag, "config mode=${AppConfig.mode} baseUrl=${AppConfig.baseUrl}")

        detailsView.text = buildConnectionDetails()
        retryButton.setOnClickListener { runHealthCheck(statusView, messageView, retryButton, copyButton) }
        copyButton.setOnClickListener {
            if (healthJob?.isActive == true) {
                return@setOnClickListener
            }
            copyDiagnostics(statusView, detailsView, messageView)
        }
        openBrowserButton.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }
        runHealthCheck(statusView, messageView, retryButton, copyButton)
    }

    private fun runHealthCheck(
        statusView: TextView,
        messageView: TextView,
        retryButton: Button,
        copyButton: Button
    ) {
        if (healthJob?.isActive == true) {
            return
        }

        statusView.text = "Connecting…"
        statusView.setTextColor(Color.GRAY)
        messageView.text = ""
        retryButton.isEnabled = false
        copyButton.isEnabled = false

        healthJob = lifecycleScope.launch {
            try {
                Log.i(tag, "healthz starting")
                val res = withContext(Dispatchers.IO) { ApiClient.api.healthz() }
                Log.i(tag, "healthz OK: $res")
                statusView.text = "OK"
                statusView.setTextColor(Color.rgb(46, 125, 50))
                val messages = mutableListOf<String>()
                messages.add("healthz: ok=${res.ok}")
                val infoResult = fetchInfo()
                val infoLines = infoResult.infoLines
                val versionLine = infoResult.info?.version?.let { formatVersionLine(it) } ?: fetchVersionLine()
                messages.add(versionLine)
                messages.addAll(infoLines)
                messageView.text = messages.joinToString("\n")
            } catch (e: Exception) {
                Log.e(tag, "healthz failed", e)
                statusView.text = "ERROR"
                statusView.setTextColor(Color.rgb(198, 40, 40))
                messageView.text = formatErrorMessage(e)
            } finally {
                retryButton.isEnabled = true
                copyButton.isEnabled = true
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

    private suspend fun fetchVersionLine(): String {
        return try {
            val version = withContext(Dispatchers.IO) { ApiClient.api.version() }
            formatVersionLine(version)
        } catch (e: Exception) {
            "version: ERROR ${formatErrorMessage(e)}"
        }
    }

    private suspend fun fetchInfo(): InfoResult {
        return try {
            val info = withContext(Dispatchers.IO) { ApiClient.api.info() }
            InfoResult(info = info, infoLines = formatInfoLines(info))
        } catch (e: Exception) {
            InfoResult(
                info = null,
                infoLines = listOf("info: ERROR ${formatErrorMessage(e)}")
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

    private fun copyDiagnostics(
        statusView: TextView,
        detailsView: TextView,
        messageView: TextView
    ) {
        val diagnosticsText = buildDiagnosticsText(statusView, detailsView, messageView)
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("CCC Diagnostics", diagnosticsText))
        Toast.makeText(this, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun buildDiagnosticsText(
        statusView: TextView,
        detailsView: TextView,
        messageView: TextView
    ): String {
        val timestamp = java.time.ZonedDateTime.now().toString()
        val endpoints = listOf(
            "/api/v1/healthz",
            "/api/v1/info",
            "/api/v1/version"
        )
        val messageText = messageView.text?.toString()?.trim().orEmpty()
        return buildString {
            appendLine("CCC diagnostics")
            appendLine("timestamp: $timestamp")
            appendLine("status: ${statusView.text}")
            appendLine()
            appendLine("app_config:")
            detailsView.text?.toString()?.lines()?.forEach { line ->
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
        val infoLines: List<String>
    )
}
