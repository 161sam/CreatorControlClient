package ccc.client

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
import ccc.client.api.CapabilitiesResponse
import ccc.client.api.CommandMeta
import ccc.client.api.CommandsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.ZonedDateTime

class BrowserActivity : AppCompatActivity() {
    private var loadJob: Job? = null
    private var commandDetailJob: Job? = null
    private var currentSection: Section = Section.CAPABILITIES
    private var capabilities: CapabilitiesResponse? = null
    private var commands: CommandsResponse? = null
    private var selectedCommand: CommandMeta? = null
    private var selectedCommandName: String? = null

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    private val capabilitiesAdapter by lazy { moshi.adapter(CapabilitiesResponse::class.java) }
    private val commandsAdapter by lazy { moshi.adapter(CommandsResponse::class.java) }
    private val commandAdapter by lazy { moshi.adapter(CommandMeta::class.java) }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppConfig.token.isNotBlank()) {
            ApiClient.setToken(AppConfig.token)
        } else {
            ApiClient.setToken(null)
        }

        val statusView = TextView(this).apply {
            text = "Loading…"
            setTextColor(Color.GRAY)
            textSize = 22f
            setPadding(0, 0, 0, 12)
        }
        val detailsView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 16)
            text = buildConnectionDetails()
        }
        val messageView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 16)
        }
        val reloadButton = Button(this).apply {
            text = "Reload"
        }
        val copyButton = Button(this).apply {
            text = "Copy browser diagnostics"
        }
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(reloadButton)
            addView(copyButton)
        }
        val capabilitiesButton = Button(this).apply {
            text = "Capabilities"
        }
        val commandsButton = Button(this).apply {
            text = "Commands"
        }
        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(capabilitiesButton)
            addView(commandsButton)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        val capabilitiesView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
        }
        val commandsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val commandDetailTitle = TextView(this).apply {
            text = "Command Detail"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        val commandDetailView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
        }

        contentLayout.addView(capabilitiesView)
        contentLayout.addView(commandsContainer)
        contentLayout.addView(commandDetailTitle)
        contentLayout.addView(commandDetailView)

        val scrollView = ScrollView(this).apply {
            addView(
                contentLayout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.START
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(statusView)
            addView(detailsView)
            addView(messageView)
            addView(buttonRow)
            addView(sectionRow)
            addView(scrollView)
        }
        setContentView(layout)

        reloadButton.setOnClickListener {
            loadData(statusView, messageView, capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
        }
        copyButton.setOnClickListener {
            copyDiagnostics(statusView, detailsView, messageView)
        }
        capabilitiesButton.setOnClickListener {
            currentSection = Section.CAPABILITIES
            updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
        }
        commandsButton.setOnClickListener {
            currentSection = Section.COMMANDS
            updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
        }

        updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
        loadData(statusView, messageView, capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
    }

    private fun loadData(
        statusView: TextView,
        messageView: TextView,
        capabilitiesView: TextView,
        commandsContainer: LinearLayout,
        commandDetailTitle: TextView,
        commandDetailView: TextView
    ) {
        if (loadJob?.isActive == true) {
            return
        }
        statusView.text = "Loading…"
        statusView.setTextColor(Color.GRAY)
        messageView.text = ""
        capabilitiesView.text = "Loading capabilities…"
        commandsContainer.removeAllViews()
        selectedCommand = null
        selectedCommandName = null
        commandDetailView.text = ""
        updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)

        loadJob = lifecycleScope.launch {
            try {
                val capabilitiesResult = withContext(Dispatchers.IO) { ApiClient.api.capabilities() }
                val commandsResult = withContext(Dispatchers.IO) { ApiClient.api.commands() }
                capabilities = capabilitiesResult
                commands = commandsResult
                statusView.text = "OK"
                statusView.setTextColor(Color.rgb(46, 125, 50))
                capabilitiesView.text = formatCapabilities(capabilitiesResult)
                renderCommands(commandsResult, commandsContainer, commandDetailTitle, commandDetailView)
            } catch (e: Exception) {
                statusView.text = "ERROR"
                statusView.setTextColor(Color.rgb(198, 40, 40))
                messageView.text = formatErrorMessage(e)
                capabilitiesView.text = "Failed to load capabilities."
                commandsContainer.removeAllViews()
                commandsContainer.addView(TextView(this@BrowserActivity).apply {
                    text = "Failed to load commands."
                })
            } finally {
                loadJob = null
                updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailTitle, commandDetailView)
            }
        }
    }

    private fun renderCommands(
        commandsResult: CommandsResponse,
        container: LinearLayout,
        commandDetailTitle: TextView,
        commandDetailView: TextView
    ) {
        container.removeAllViews()
        val list = commandsResult.commands
        if (list.isNullOrEmpty()) {
            container.addView(TextView(this).apply { text = "No commands available." })
            return
        }
        list.forEach { command ->
            val title = buildString {
                append(command.name ?: "<unnamed>")
                command.description?.takeIf { it.isNotBlank() }?.let {
                    append(" — ").append(it)
                }
                val tags = command.tags?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                if (!tags.isNullOrBlank()) {
                    append(" [").append(tags).append("]")
                }
            }
            val button = Button(this).apply {
                text = title
                setOnClickListener {
                    val name = command.name
                    if (name.isNullOrBlank()) {
                        selectedCommand = command
                        selectedCommandName = null
                        commandDetailView.text = formatCommandDetail(command)
                        updateSectionVisibility(null, null, commandDetailTitle, commandDetailView)
                    } else {
                        loadCommandDetail(name, commandDetailTitle, commandDetailView)
                    }
                }
            }
            container.addView(button)
        }
    }

    private fun loadCommandDetail(
        name: String,
        commandDetailTitle: TextView,
        commandDetailView: TextView
    ) {
        if (commandDetailJob?.isActive == true) {
            return
        }
        selectedCommandName = name
        commandDetailView.text = "Loading command detail…"
        updateSectionVisibility(null, null, commandDetailTitle, commandDetailView)
        commandDetailJob = lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { ApiClient.api.command(name) }
                selectedCommand = detail
                commandDetailView.text = formatCommandDetail(detail)
            } catch (e: Exception) {
                selectedCommand = null
                commandDetailView.text = "ERROR: ${formatErrorMessage(e)}"
            } finally {
                commandDetailJob = null
                updateSectionVisibility(null, null, commandDetailTitle, commandDetailView)
            }
        }
    }

    private fun updateSectionVisibility(
        capabilitiesView: TextView?,
        commandsContainer: LinearLayout?,
        commandDetailTitle: TextView?,
        commandDetailView: TextView?
    ) {
        val showCapabilities = currentSection == Section.CAPABILITIES
        capabilitiesView?.visibility = if (showCapabilities) View.VISIBLE else View.GONE
        commandsContainer?.visibility = if (showCapabilities) View.GONE else View.VISIBLE
        val showDetail = !showCapabilities && !commandDetailView?.text.isNullOrBlank()
        commandDetailTitle?.visibility = if (showDetail) View.VISIBLE else View.GONE
        commandDetailView?.visibility = if (showDetail) View.VISIBLE else View.GONE
    }

    private fun formatCapabilities(response: CapabilitiesResponse): String {
        val capabilities = response.capabilities
        return buildString {
            appendLine("Service: ${response.service ?: "<unknown>"}")
            appendLine("API version: ${response.apiVersion ?: "<unknown>"}")
            appendLine("Time (UTC): ${response.timeUtc ?: "<unknown>"}")
            appendLine("Auth: ${response.auth?.scheme ?: "unknown"} required=${response.auth?.required ?: "unknown"}")
            appendLine()
            appendLine("Formats")
            appendLine("  import: ${capabilities?.importFormats?.joinToString(", ") ?: "<none>"}")
            appendLine("  export: ${capabilities?.exportFormats?.joinToString(", ") ?: "<none>"}")
            appendLine()
            appendLine("Commands")
            appendLine("  available: ${capabilities?.commands?.size ?: 0}")
            capabilities?.commands?.takeIf { it.isNotEmpty() }?.forEach { command ->
                appendLine("  - $command")
            }
            appendLine()
            appendLine("Limits")
            appendLine(formatMap("  ", capabilities?.limits))
            appendLine()
            appendLine("Features")
            appendLine(formatMap("  ", capabilities?.features))
            appendLine()
            appendLine("FreeCAD")
            appendLine(formatMap("  ", response.freecad))
            appendLine()
            appendLine("Session")
            appendLine(formatMap("  ", response.session))
            appendLine()
            appendLine("Raw JSON (truncated)")
            appendLine(truncateJson(capabilitiesAdapter.toJson(response), 1200))
        }
    }

    private fun formatCommandDetail(command: CommandMeta): String {
        val argsSchema = command.argsSchema?.let { mapAdapter.toJson(it) }
        val safetyJson = command.safety?.let { mapAdapter.toJson(it) }
        val constraintsJson = command.constraints?.let { mapAdapter.toJson(it) }
        val limitsJson = command.limits?.let { mapAdapter.toJson(it) }
        return buildString {
            appendLine("Name: ${command.name ?: "<unknown>"}")
            appendLine("Description: ${command.description ?: "<none>"}")
            appendLine("Returns: ${command.returns ?: "<none>"}")
            appendLine("Tags: ${command.tags?.joinToString(", ") ?: "<none>"}")
            appendLine("Args schema: ${argsSchema ?: "<none>"}")
            appendLine("Safety: ${safetyJson ?: "<none>"}")
            appendLine("Constraints: ${constraintsJson ?: "<none>"}")
            appendLine("Limits: ${limitsJson ?: "<none>"}")
            appendLine()
            appendLine("Raw JSON (truncated)")
            appendLine(truncateJson(commandAdapter.toJson(command), 1200))
        }
    }

    private fun formatMap(prefix: String, map: Map<String, Any?>?): String {
        if (map.isNullOrEmpty()) {
            return "${prefix}<none>"
        }
        return map.entries.joinToString("\n") { entry ->
            "$prefix${entry.key}: ${entry.value}"
        }
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

    private fun truncateJson(input: String, maxLength: Int): String {
        return truncate(input, maxLength)
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

    private fun copyDiagnostics(
        statusView: TextView,
        detailsView: TextView,
        messageView: TextView
    ) {
        val diagnosticsText = buildDiagnosticsText(statusView, detailsView, messageView)
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("CCC Browser Diagnostics", diagnosticsText))
        Toast.makeText(this, "Browser diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun buildDiagnosticsText(
        statusView: TextView,
        detailsView: TextView,
        messageView: TextView
    ): String {
        val timestamp = ZonedDateTime.now().toString()
        val capabilitiesJson = capabilities?.let { truncateJson(capabilitiesAdapter.toJson(it), 1000) } ?: "<none>"
        val commandsJson = commands?.let { truncateJson(commandsAdapter.toJson(it), 1000) } ?: "<none>"
        val commandJson = selectedCommand?.let { truncateJson(commandAdapter.toJson(it), 1000) } ?: "<none>"
        val selectedName = selectedCommandName ?: selectedCommand?.name ?: "<none>"
        val messageText = messageView.text?.toString()?.trim().orEmpty()
        return buildString {
            appendLine("CCC browser diagnostics")
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
            appendLine("capabilities_json:")
            appendLine("  ${capabilitiesJson.replace("\n", "\n  ")}")
            appendLine()
            appendLine("commands_json:")
            appendLine("  ${commandsJson.replace("\n", "\n  ")}")
            appendLine()
            appendLine("selected_command: $selectedName")
            appendLine("command_detail_json:")
            appendLine("  ${commandJson.replace("\n", "\n  ")}")
        }
    }

    private enum class Section {
        CAPABILITIES,
        COMMANDS
    }
}
