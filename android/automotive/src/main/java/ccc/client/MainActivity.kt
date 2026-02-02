package ccc.client

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
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
            addView(retryButton)
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
        retryButton.setOnClickListener { runHealthCheck(statusView, messageView, retryButton) }
        runHealthCheck(statusView, messageView, retryButton)
    }

    private fun runHealthCheck(
        statusView: TextView,
        messageView: TextView,
        retryButton: Button
    ) {
        if (healthJob?.isActive == true) {
            return
        }

        statusView.text = "Connecting…"
        statusView.setTextColor(Color.GRAY)
        messageView.text = ""
        retryButton.isEnabled = false

        healthJob = lifecycleScope.launch {
            try {
                Log.i(tag, "healthz starting")
                val res = withContext(Dispatchers.IO) { ApiClient.api.healthz() }
                Log.i(tag, "healthz OK: $res")
                statusView.text = "OK"
                statusView.setTextColor(Color.rgb(46, 125, 50))
                messageView.text = "Response: ok=${res.ok}"
            } catch (e: Exception) {
                Log.e(tag, "healthz failed", e)
                statusView.text = "ERROR"
                statusView.setTextColor(Color.rgb(198, 40, 40))
                messageView.text = formatErrorMessage(e)
            } finally {
                retryButton.isEnabled = true
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
}
