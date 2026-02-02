package ccc.client

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
import ccc.client.api.ArgSpec
import ccc.client.api.CapabilitiesResponse
import ccc.client.api.CommandMeta
import ccc.client.api.CommandsResponse
import ccc.client.api.ExportResponse
import ccc.client.api.ExecCommandRequest
import ccc.client.api.ExecCommandResponse
import ccc.client.api.UploadResponse
import ccc.client.api.parseExportResponse
import ccc.client.api.parseArgsSchema
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.time.ZonedDateTime

class BrowserActivity : AppCompatActivity() {
    private var loadJob: Job? = null
    private var commandDetailJob: Job? = null
    private var execJob: Job? = null
    private var uploadJob: Job? = null
    private var importJob: Job? = null
    private var exportJob: Job? = null
    private var downloadJob: Job? = null
    private lateinit var pickFileLauncher: ActivityResultLauncher<Array<String>>
    private var currentSection: Section = Section.CAPABILITIES
    private var capabilities: CapabilitiesResponse? = null
    private var commands: CommandsResponse? = null
    private var selectedCommand: CommandMeta? = null
    private var selectedCommandName: String? = null
    private var argInputs: List<ArgInput> = emptyList()
    private var lastExecRequestPayload: String? = null
    private var lastExecResponsePayload: String? = null
    private var lastExecHttpStatus: String? = null
    private var selectedFile: SelectedFileInfo? = null
    private var lastUploadRequestPayload: String? = null
    private var lastUploadResponsePayload: String? = null
    private var lastUploadHttpStatus: String? = null
    private var lastUploadResponse: UploadResponse? = null
    private var lastImportRequestPayload: String? = null
    private var lastImportResponsePayload: String? = null
    private var lastImportHttpStatus: String? = null
    private var lastExportRequestPayload: String? = null
    private var lastExportResponsePayload: String? = null
    private var lastExportHttpStatus: String? = null
    private var lastExportResponse: ExportResponse? = null
    private var lastExportDownloadStatus: String? = null

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    private val capabilitiesAdapter by lazy { moshi.adapter(CapabilitiesResponse::class.java) }
    private val commandsAdapter by lazy { moshi.adapter(CommandsResponse::class.java) }
    private val commandAdapter by lazy { moshi.adapter(CommandMeta::class.java) }
    private val execResponseAdapter by lazy { moshi.adapter(ExecCommandResponse::class.java) }
    private val uploadResponseAdapter by lazy { moshi.adapter(UploadResponse::class.java) }
    private val exportResponseAdapter by lazy { moshi.adapter(ExportResponse::class.java) }
    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any?>>(type)
    }
    private val anyAdapter by lazy { moshi.adapter(Any::class.java) }

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
        val filesTitle = TextView(this).apply {
            text = "Files"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 8, 0, 8)
        }
        val pickFileButton = Button(this).apply {
            text = "Pick file"
        }
        val uploadButton = Button(this).apply {
            text = "Upload"
            isEnabled = false
        }
        val importButton = Button(this).apply {
            text = "Import"
            isEnabled = false
        }
        val fileStatusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 8)
            text = "No file selected."
        }
        val uploadStatusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        val importStatusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        val filesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(filesTitle)
            addView(pickFileButton)
            addView(fileStatusView)
            addView(uploadButton)
            addView(uploadStatusView)
            addView(importButton)
            addView(importStatusView)
        }
        val exportsTitle = TextView(this).apply {
            text = "Exports"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        val exportStlButton = Button(this).apply {
            text = "Export STL"
        }
        val exportStepButton = Button(this).apply {
            text = "Export STEP"
        }
        val exportButtonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(exportStlButton)
            addView(exportStepButton)
        }
        val exportStatusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
            text = "No export requested yet."
        }
        val exportResultView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.DKGRAY)
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 8)
        }
        val exportDownloadButton = Button(this).apply {
            text = "Download"
            isEnabled = false
        }
        val exportCopyLinkButton = Button(this).apply {
            text = "Copy link"
            isEnabled = false
        }
        val exportActionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(exportDownloadButton)
            addView(exportCopyLinkButton)
        }
        val exportsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(exportsTitle)
            addView(exportButtonRow)
            addView(exportStatusView)
            addView(exportResultView)
            addView(exportActionsRow)
        }
        pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                handlePickedFile(uri, fileStatusView, uploadStatusView, importStatusView, uploadButton, importButton)
            }
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
        val argsTitle = TextView(this).apply {
            text = "Arguments"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        val argsHintView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 8)
        }
        val argsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val rawJsonToggle = Switch(this).apply {
            text = "Raw JSON"
            setPadding(0, 8, 0, 8)
        }
        val rawJsonInput = EditText(this).apply {
            hint = "{ \"command\": \"open_new_doc\", \"args\": { } }"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(12, 12, 12, 12)
            minLines = 4
            isSingleLine = false
            visibility = View.GONE
        }
        val runButton = Button(this).apply {
            text = "Run"
        }
        val resultTitle = TextView(this).apply {
            text = "Result"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        val resultStatus = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        val stdoutLabel = TextView(this).apply {
            text = "stdout"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        val stdoutView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 8)
        }
        val stderrLabel = TextView(this).apply {
            text = "stderr"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        val stderrView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 8)
        }
        val resultLabel = TextView(this).apply {
            text = "result"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        val resultView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 8)
        }
        val copyResultButton = Button(this).apply {
            text = "Copy result"
        }
        val commandDetailContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(commandDetailTitle)
            addView(commandDetailView)
            addView(argsTitle)
            addView(argsHintView)
            addView(argsContainer)
            addView(rawJsonToggle)
            addView(rawJsonInput)
            addView(runButton)
            addView(resultTitle)
            addView(resultStatus)
            addView(stdoutLabel)
            addView(stdoutView)
            addView(stderrLabel)
            addView(stderrView)
            addView(resultLabel)
            addView(resultView)
            addView(copyResultButton)
        }

        contentLayout.addView(capabilitiesView)
        contentLayout.addView(commandsContainer)
        contentLayout.addView(commandDetailContainer)

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
            addView(filesContainer)
            addView(exportsContainer)
            addView(buttonRow)
            addView(sectionRow)
            addView(scrollView)
        }
        setContentView(layout)

        val commandDetailViews = CommandDetailViews(
            container = commandDetailContainer,
            detailView = commandDetailView,
            argsTitle = argsTitle,
            argsHint = argsHintView,
            argsContainer = argsContainer,
            rawJsonToggle = rawJsonToggle,
            rawJsonInput = rawJsonInput,
            runButton = runButton,
            resultTitle = resultTitle,
            resultStatus = resultStatus,
            stdoutView = stdoutView,
            stderrView = stderrView,
            resultView = resultView,
            copyResultButton = copyResultButton
        )

        reloadButton.setOnClickListener {
            loadData(statusView, messageView, capabilitiesView, commandsContainer, commandDetailViews)
        }
        copyButton.setOnClickListener {
            copyDiagnostics(statusView, detailsView, messageView)
        }
        pickFileButton.setOnClickListener {
            pickFileLauncher.launch(buildFileMimeTypes())
        }
        uploadButton.setOnClickListener {
            uploadSelectedFile(fileStatusView, uploadStatusView, importStatusView, uploadButton, importButton)
        }
        importButton.setOnClickListener {
            importUploadedFile(importStatusView, importButton)
        }
        exportStlButton.setOnClickListener {
            requestExport("stl", exportStatusView, exportResultView, exportDownloadButton, exportCopyLinkButton)
        }
        exportStepButton.setOnClickListener {
            requestExport("step", exportStatusView, exportResultView, exportDownloadButton, exportCopyLinkButton)
        }
        exportDownloadButton.setOnClickListener {
            downloadExport(exportStatusView)
        }
        exportCopyLinkButton.setOnClickListener {
            copyExportLink()
        }
        capabilitiesButton.setOnClickListener {
            currentSection = Section.CAPABILITIES
            updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailContainer, commandDetailView)
        }
        commandsButton.setOnClickListener {
            currentSection = Section.COMMANDS
            updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailContainer, commandDetailView)
        }
        rawJsonToggle.setOnCheckedChangeListener { _, isChecked ->
            rawJsonInput.visibility = if (isChecked) View.VISIBLE else View.GONE
            setArgsInputsEnabled(argsContainer, !isChecked)
        }
        runButton.setOnClickListener {
            runCommand(commandDetailViews)
        }
        copyResultButton.setOnClickListener {
            copyExecDiagnostics()
        }

        updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailContainer, commandDetailView)
        loadData(statusView, messageView, capabilitiesView, commandsContainer, commandDetailViews)
    }

    private fun loadData(
        statusView: TextView,
        messageView: TextView,
        capabilitiesView: TextView,
        commandsContainer: LinearLayout,
        commandDetailViews: CommandDetailViews
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
        commandDetailViews.detailView.text = ""
        resetExecViews(commandDetailViews)
        updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailViews.container, commandDetailViews.detailView)

        loadJob = lifecycleScope.launch {
            try {
                val capabilitiesResult = withContext(Dispatchers.IO) { ApiClient.api.capabilities() }
                val commandsResult = withContext(Dispatchers.IO) { ApiClient.api.commands() }
                capabilities = capabilitiesResult
                commands = commandsResult
                statusView.text = "OK"
                statusView.setTextColor(Color.rgb(46, 125, 50))
                capabilitiesView.text = formatCapabilities(capabilitiesResult)
                renderCommands(commandsResult, commandsContainer, commandDetailViews)
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
                updateSectionVisibility(capabilitiesView, commandsContainer, commandDetailViews.container, commandDetailViews.detailView)
            }
        }
    }

    private fun renderCommands(
        commandsResult: CommandsResponse,
        container: LinearLayout,
        commandDetailViews: CommandDetailViews
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
                        commandDetailViews.detailView.text = formatCommandDetail(command)
                        renderArgsSchema(commandDetailViews, command)
                        updateSectionVisibility(null, null, commandDetailViews.container, commandDetailViews.detailView)
                    } else {
                        loadCommandDetail(name, commandDetailViews)
                    }
                }
            }
            container.addView(button)
        }
    }

    private fun loadCommandDetail(
        name: String,
        commandDetailViews: CommandDetailViews
    ) {
        if (commandDetailJob?.isActive == true) {
            return
        }
        selectedCommandName = name
        commandDetailViews.detailView.text = "Loading command detail…"
        resetExecViews(commandDetailViews)
        updateSectionVisibility(null, null, commandDetailViews.container, commandDetailViews.detailView)
        commandDetailJob = lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { ApiClient.api.command(name) }
                selectedCommand = detail
                commandDetailViews.detailView.text = formatCommandDetail(detail)
                renderArgsSchema(commandDetailViews, detail)
            } catch (e: Exception) {
                selectedCommand = null
                commandDetailViews.detailView.text = "ERROR: ${formatErrorMessage(e)}"
                renderArgsSchema(commandDetailViews, null)
            } finally {
                commandDetailJob = null
                updateSectionVisibility(null, null, commandDetailViews.container, commandDetailViews.detailView)
            }
        }
    }

    private fun updateSectionVisibility(
        capabilitiesView: TextView?,
        commandsContainer: LinearLayout?,
        commandDetailContainer: LinearLayout?,
        commandDetailView: TextView?
    ) {
        val showCapabilities = currentSection == Section.CAPABILITIES
        capabilitiesView?.visibility = if (showCapabilities) View.VISIBLE else View.GONE
        commandsContainer?.visibility = if (showCapabilities) View.GONE else View.VISIBLE
        val showDetail = !showCapabilities && !commandDetailView?.text.isNullOrBlank()
        commandDetailContainer?.visibility = if (showDetail) View.VISIBLE else View.GONE
    }

    private fun buildFileMimeTypes(): Array<String> {
        return arrayOf(
            "model/*",
            "model/stl",
            "model/step",
            "application/sla",
            "application/step",
            "application/octet-stream",
            "*/*"
        )
    }

    private fun handlePickedFile(
        uri: Uri,
        fileStatusView: TextView,
        uploadStatusView: TextView,
        importStatusView: TextView,
        uploadButton: Button,
        importButton: Button
    ) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
        val meta = querySelectedFile(uri)
        if (meta == null) {
            Toast.makeText(this, "Unable to read file metadata.", Toast.LENGTH_SHORT).show()
            return
        }
        selectedFile = meta
        fileStatusView.text = buildFileStatusText(meta)
        uploadStatusView.text = "Ready to upload."
        importStatusView.text = ""
        uploadButton.isEnabled = true
        importButton.isEnabled = false
        lastUploadRequestPayload = null
        lastUploadResponsePayload = null
        lastUploadHttpStatus = null
        lastUploadResponse = null
        lastImportRequestPayload = null
        lastImportResponsePayload = null
        lastImportHttpStatus = null
        uploadJob?.cancel()
        importJob?.cancel()
    }

    private fun querySelectedFile(uri: Uri): SelectedFileInfo? {
        var name: String? = null
        var size: Long? = null
        contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        val mime = contentResolver.getType(uri)
        if (name.isNullOrBlank()) {
            return null
        }
        return SelectedFileInfo(uri, name!!, size, mime)
    }

    private fun uploadSelectedFile(
        fileStatusView: TextView,
        uploadStatusView: TextView,
        importStatusView: TextView,
        uploadButton: Button,
        importButton: Button
    ) {
        if (uploadJob?.isActive == true) {
            return
        }
        val file = selectedFile
        if (file == null) {
            Toast.makeText(this, "Pick a file first.", Toast.LENGTH_SHORT).show()
            return
        }
        fileStatusView.text = buildFileStatusText(file)
        uploadStatusView.text = "Uploading…"
        importStatusView.text = ""
        uploadButton.isEnabled = false
        importButton.isEnabled = false
        lastUploadRequestPayload = buildUploadRequestSummary(file)
        lastUploadResponsePayload = null
        lastUploadHttpStatus = null
        uploadJob = lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val mime = (file.mime ?: "application/octet-stream").toMediaType()
                    val body = ContentUriRequestBody(this@BrowserActivity, file.uri, mime, file.size)
                    val part = MultipartBody.Part.createFormData("file", file.name, body)
                    ApiClient.api.upload(part)
                }
                lastUploadHttpStatus = "HTTP 200"
                lastUploadResponse = response
                lastUploadResponsePayload = uploadResponseAdapter.toJson(response)
                uploadStatusView.text = buildUploadResultText(response)
                importButton.isEnabled = response.fileId != null || response.path != null
            } catch (e: Exception) {
                val httpException = e as? HttpException
                val rawBody = httpException?.response()?.errorBody()?.string()
                val errorMessage = if (httpException != null) {
                    val bodyText = rawBody?.takeIf { it.isNotBlank() } ?: "<no body>"
                    "HTTP ${httpException.code()}: ${truncate(bodyText, 200)}"
                } else {
                    val message = e.message ?: "unknown error"
                    "${e.javaClass.simpleName}: $message"
                }
                lastUploadHttpStatus = if (httpException != null) "HTTP ${httpException.code()}" else "error"
                lastUploadResponse = null
                lastUploadResponsePayload = rawBody ?: errorMessage
                uploadStatusView.text = "Upload failed: $errorMessage"
                importButton.isEnabled = false
            } finally {
                uploadJob = null
                uploadButton.isEnabled = true
            }
        }
    }

    private fun importUploadedFile(
        importStatusView: TextView,
        importButton: Button
    ) {
        if (importJob?.isActive == true) {
            return
        }
        val file = selectedFile
        val uploadResponse = lastUploadResponse
        val fileId = uploadResponse?.fileId
        val path = uploadResponse?.path
        if (fileId.isNullOrBlank() && path.isNullOrBlank()) {
            Toast.makeText(this, "Upload a file before importing.", Toast.LENGTH_SHORT).show()
            return
        }
        val args = mutableMapOf<String, Any?>()
        if (!fileId.isNullOrBlank()) {
            args["file_id"] = fileId
        } else if (!path.isNullOrBlank()) {
            args["path"] = path
        }
        val format = inferFormat(file?.name ?: path.orEmpty())
        if (!format.isNullOrBlank()) {
            args["format"] = format
        }
        val requestPayload = mapOf("command" to "import_file", "args" to args)
        lastImportRequestPayload = toPrettyJson(requestPayload)
        lastImportResponsePayload = null
        lastImportHttpStatus = null
        importStatusView.text = "Importing…"
        importButton.isEnabled = false
        importJob = lifecycleScope.launch {
            try {
                val execResponse = withContext(Dispatchers.IO) {
                    ApiClient.api.execCommand(ExecCommandRequest("import_file", args))
                }
                lastImportHttpStatus = "HTTP 200"
                lastImportResponsePayload = execResponseAdapter.toJson(execResponse)
                val statusText = when {
                    execResponse.ok == true -> "success"
                    !execResponse.status.isNullOrBlank() -> execResponse.status
                    else -> "error"
                }
                importStatusView.text = buildImportResultText(statusText, execResponse)
            } catch (e: Exception) {
                val httpException = e as? HttpException
                val rawBody = httpException?.response()?.errorBody()?.string()
                val errorMessage = if (httpException != null) {
                    val bodyText = rawBody?.takeIf { it.isNotBlank() } ?: "<no body>"
                    "HTTP ${httpException.code()}: ${truncate(bodyText, 200)}"
                } else {
                    val message = e.message ?: "unknown error"
                    "${e.javaClass.simpleName}: $message"
                }
                lastImportHttpStatus = if (httpException != null) "HTTP ${httpException.code()}" else "error"
                lastImportResponsePayload = rawBody ?: errorMessage
                importStatusView.text = "Import failed: $errorMessage"
            } finally {
                importJob = null
                importButton.isEnabled = true
            }
        }
    }

    private fun requestExport(
        format: String,
        exportStatusView: TextView,
        exportResultView: TextView,
        downloadButton: Button,
        copyLinkButton: Button
    ) {
        if (exportJob?.isActive == true) {
            return
        }
        val args = mapOf("format" to format)
        lastExportRequestPayload = toPrettyJson(mapOf("command" to "export_current_doc", "args" to args))
        lastExportResponsePayload = null
        lastExportHttpStatus = null
        lastExportResponse = null
        lastExportDownloadStatus = null
        exportStatusView.text = "Exporting…"
        exportResultView.text = ""
        downloadButton.isEnabled = false
        copyLinkButton.isEnabled = false
        exportJob = lifecycleScope.launch {
            try {
                val execResponse = withContext(Dispatchers.IO) {
                    ApiClient.api.execCommand(ExecCommandRequest("export_current_doc", args))
                }
                lastExportHttpStatus = "HTTP 200"
                val exportResponse = parseExportResponse(execResponse.result, exportResponseAdapter)
                lastExportResponse = exportResponse
                lastExportResponsePayload = if (exportResponse != null) {
                    exportResponseAdapter.toJson(exportResponse)
                } else {
                    execResponseAdapter.toJson(execResponse)
                }
                exportStatusView.text = buildExportStatusText(execResponse, exportResponse)
                exportResultView.text = exportResponse?.let { formatExportDetails(it) } ?: "No export payload returned."
                val hasDownload = !exportResponse?.downloadUrl.isNullOrBlank()
                downloadButton.isEnabled = hasDownload
                copyLinkButton.isEnabled = hasDownload
            } catch (e: Exception) {
                val httpException = e as? HttpException
                val rawBody = httpException?.response()?.errorBody()?.string()
                val errorMessage = if (httpException != null) {
                    val bodyText = rawBody?.takeIf { it.isNotBlank() } ?: "<no body>"
                    "HTTP ${httpException.code()}: ${truncate(bodyText, 200)}"
                } else {
                    val message = e.message ?: "unknown error"
                    "${e.javaClass.simpleName}: $message"
                }
                lastExportHttpStatus = if (httpException != null) "HTTP ${httpException.code()}" else "error"
                lastExportResponsePayload = rawBody ?: errorMessage
                exportStatusView.text = "Export failed: $errorMessage"
                exportResultView.text = ""
            } finally {
                exportJob = null
            }
        }
    }

    private fun downloadExport(exportStatusView: TextView) {
        if (downloadJob?.isActive == true) {
            return
        }
        val export = lastExportResponse
        val downloadUrl = export?.downloadUrl
        if (export == null || downloadUrl.isNullOrBlank()) {
            Toast.makeText(this, "Export a file before downloading.", Toast.LENGTH_SHORT).show()
            return
        }
        exportStatusView.text = "Downloading…"
        lastExportDownloadStatus = null
        downloadJob = lifecycleScope.launch {
            try {
                val savedFile = withContext(Dispatchers.IO) {
                    ApiClient.downloadExport(downloadUrl).use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IOException("empty response body")
                        val targetDir = getExternalFilesDir(null) ?: cacheDir
                        val safeName = sanitizeFileName(export.filename ?: "export.bin")
                        val output = File(targetDir, safeName)
                        body.byteStream().use { input ->
                            FileOutputStream(output).use { outputStream ->
                                input.copyTo(outputStream)
                            }
                        }
                        output
                    }
                }
                lastExportDownloadStatus = "saved=${savedFile.absolutePath}"
                exportStatusView.text = "Download saved: ${savedFile.absolutePath}"
            } catch (e: Exception) {
                val message = e.message ?: "unknown error"
                lastExportDownloadStatus = "error=${e.javaClass.simpleName}: $message"
                exportStatusView.text = "Download failed: ${e.javaClass.simpleName}: $message"
            } finally {
                downloadJob = null
            }
        }
    }

    private fun copyExportLink() {
        val export = lastExportResponse
        val downloadUrl = export?.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            Toast.makeText(this, "No export link available.", Toast.LENGTH_SHORT).show()
            return
        }
        val fullUrl = buildAbsoluteUrl(downloadUrl)
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("CCC Export Link", fullUrl))
        Toast.makeText(this, "Export link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun buildFileStatusText(file: SelectedFileInfo): String {
        val sizeText = file.size?.let { NumberFormat.getInstance().format(it) + " bytes" } ?: "unknown size"
        val mimeText = file.mime ?: "unknown mime"
        return "Selected: ${file.name} ($sizeText, $mimeText)"
    }

    private fun buildUploadRequestSummary(file: SelectedFileInfo): String {
        val sizeText = file.size?.toString() ?: "<unknown>"
        return "file=${file.name} size=${sizeText} mime=${file.mime ?: "unknown"}"
    }

    private fun buildUploadResultText(response: UploadResponse): String {
        val okText = if (response.ok == true) "ok" else "error"
        val idText = response.fileId ?: "<none>"
        val pathText = response.path ?: "<none>"
        val sizeText = response.size?.toString() ?: "<unknown>"
        return "Upload $okText: id=$idText size=$sizeText path=$pathText"
    }

    private fun buildImportResultText(status: String, response: ExecCommandResponse): String {
        val resultText = response.result?.let { truncateJson(toPrettyJson(it), 800) } ?: "<none>"
        return "Import $status\n$resultText"
    }

    private fun buildExportStatusText(response: ExecCommandResponse, export: ExportResponse?): String {
        val okText = when {
            export?.ok == true -> "ok"
            response.ok == true -> "ok"
            response.status?.isNotBlank() == true -> response.status
            else -> "error"
        }
        val filename = export?.filename ?: "<unknown>"
        val sizeText = export?.size?.let { NumberFormat.getInstance().format(it) } ?: "<unknown>"
        return "Export $okText: $filename (${sizeText} bytes)"
    }

    private fun formatExportDetails(export: ExportResponse): String {
        val sizeText = export.size?.let { NumberFormat.getInstance().format(it) } ?: "<unknown>"
        return buildString {
            appendLine("format: ${export.format ?: "<unknown>"}")
            appendLine("filename: ${export.filename ?: "<unknown>"}")
            appendLine("size: ${sizeText} bytes")
            appendLine("created_utc: ${export.createdUtc ?: "<unknown>"}")
            appendLine("download_url: ${export.downloadUrl ?: "<unknown>"}")
        }
    }

    private fun inferFormat(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "stp" -> "step"
            "step", "stl", "obj", "iges", "fcstd" -> ext
            else -> null
        }
    }

    private fun sanitizeFileName(name: String): String {
        val trimmed = name.trim()
        val cleaned = trimmed.replace("/", "_").replace("\\", "_")
        return if (cleaned.isBlank()) "export.bin" else cleaned
    }

    private fun buildAbsoluteUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            AppConfig.baseUrl.trimEnd('/') + path
        }
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

    private fun renderArgsSchema(views: CommandDetailViews, command: CommandMeta?) {
        views.argsContainer.removeAllViews()
        argInputs = emptyList()
        views.argsHint.text = ""
        views.rawJsonToggle.isChecked = false
        views.rawJsonInput.visibility = View.GONE
        setArgsInputsEnabled(views.argsContainer, true)
        val commandName = command?.name ?: selectedCommandName ?: "open_new_doc"
        views.rawJsonInput.hint = "{ \"command\": \"$commandName\", \"args\": { } }"

        if (command == null) {
            views.argsHint.text = "No command selected."
            return
        }

        val schema = command.parseArgsSchema()
        if (schema == null || schema.properties.isEmpty()) {
            views.argsHint.text = "No args schema; use Raw JSON."
            return
        }

        val inputs = mutableListOf<ArgInput>()
        schema.properties.forEach { (name, spec) ->
            val label = TextView(this).apply {
                text = buildArgLabel(name, spec)
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(0, 8, 0, 4)
            }
            val inputView: View = when (spec.type) {
                "boolean" -> Switch(this).apply {
                    isChecked = (spec.defaultValue as? Boolean) ?: false
                    text = spec.description ?: ""
                }
                "integer" -> EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    setTextColor(Color.BLACK)
                    setHintTextColor(Color.GRAY)
                    spec.description?.let { hint = it }
                    (spec.defaultValue as? Number)?.let { setText(it.toLong().toString()) }
                }
                "number" -> EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    setTextColor(Color.BLACK)
                    setHintTextColor(Color.GRAY)
                    spec.description?.let { hint = it }
                    (spec.defaultValue as? Number)?.let { setText(it.toDouble().toString()) }
                }
                else -> EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    setTextColor(Color.BLACK)
                    setHintTextColor(Color.GRAY)
                    spec.description?.let { hint = it }
                    (spec.defaultValue as? String)?.let { setText(it) }
                }
            }
            views.argsContainer.addView(label)
            views.argsContainer.addView(inputView)
            inputs.add(ArgInput(name, spec, inputView))
        }
        argInputs = inputs
    }

    private fun runCommand(views: CommandDetailViews) {
        if (execJob?.isActive == true) {
            return
        }
        val commandName = selectedCommand?.name ?: selectedCommandName
        if (commandName.isNullOrBlank()) {
            Toast.makeText(this, "Select a command first.", Toast.LENGTH_SHORT).show()
            return
        }
        val rawJsonMode = views.rawJsonToggle.isChecked
        val rawJson = views.rawJsonInput.text?.toString()?.trim().orEmpty()
        if (rawJsonMode) {
            val parsed = runCatching { anyAdapter.fromJson(rawJson) }.getOrNull()
            if (rawJson.isBlank() || parsed == null) {
                Toast.makeText(this, "Raw JSON is invalid.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val argsMap = if (rawJsonMode) {
            null
        } else {
            buildArgsMapOrNull() ?: return
        }
        val requestPayload = if (rawJsonMode) {
            rawJson
        } else {
            val payload = mapOf("command" to commandName, "args" to argsMap)
            toPrettyJson(payload)
        }
        lastExecRequestPayload = requestPayload
        lastExecResponsePayload = null
        lastExecHttpStatus = null
        updateExecResultViews(views, null, null, null, "Running…")
        execJob = lifecycleScope.launch {
            setExecInProgress(views, true)
            try {
                val response = withContext(Dispatchers.IO) {
                    if (rawJsonMode) {
                        val body = requestPayload.toRequestBody("application/json".toMediaType())
                        ApiClient.api.execCommandRaw(body)
                    } else {
                        ApiClient.api.execCommand(ExecCommandRequest(commandName, argsMap ?: emptyMap()))
                    }
                } ?: return@launch
                lastExecHttpStatus = "HTTP 200"
                lastExecResponsePayload = truncateJson(execResponseAdapter.toJson(response), 2000)
                val statusText = when {
                    response.ok == true -> "success"
                    !response.status.isNullOrBlank() -> response.status
                    else -> "error"
                }
                updateExecResultViews(
                    views,
                    response.stdout,
                    response.stderr,
                    response.result,
                    "Status: $statusText ($lastExecHttpStatus)"
                )
            } catch (e: Exception) {
                val httpException = e as? HttpException
                val rawBody = httpException?.response()?.errorBody()?.string()
                val errorMessage = if (httpException != null) {
                    val bodyText = rawBody?.takeIf { it.isNotBlank() } ?: "<no body>"
                    "HTTP ${httpException.code()}: ${truncate(bodyText, 200)}"
                } else {
                    val message = e.message ?: "unknown error"
                    "${e.javaClass.simpleName}: $message"
                }
                val httpStatus = if (httpException != null) "HTTP ${httpException.code()}" else "error"
                lastExecHttpStatus = httpStatus
                lastExecResponsePayload = rawBody?.let { truncateJson(it, 2000) } ?: errorMessage
                updateExecResultViews(
                    views,
                    stdout = null,
                    stderr = errorMessage,
                    result = lastExecResponsePayload,
                    statusLine = "Status: error ($httpStatus)"
                )
            } finally {
                setExecInProgress(views, false)
            }
        }
    }

    private fun buildArgsMapOrNull(): Map<String, Any?>? {
        val args = mutableMapOf<String, Any?>()
        for (input in argInputs) {
            val value = parseArgValue(input) ?: return null
            if (value.shouldInclude) {
                args[input.name] = value.value
            }
        }
        return args
    }

    private fun parseArgValue(input: ArgInput): ParsedArg? {
        val spec = input.spec
        return when (val view = input.view) {
            is Switch -> {
                ParsedArg(shouldInclude = true, value = view.isChecked)
            }
            is EditText -> {
                val text = view.text?.toString()?.trim().orEmpty()
                if (text.isBlank()) {
                    if (spec.required) {
                        Toast.makeText(this, "Required field: ${input.name}", Toast.LENGTH_SHORT).show()
                        return null
                    }
                    return ParsedArg(shouldInclude = false, value = null)
                }
                when (spec.type) {
                    "integer" -> {
                        val parsed = text.toLongOrNull()
                        if (parsed == null) {
                            Toast.makeText(this, "Invalid integer for ${input.name}", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        ParsedArg(true, parsed)
                    }
                    "number" -> {
                        val parsed = text.toDoubleOrNull()
                        if (parsed == null) {
                            Toast.makeText(this, "Invalid number for ${input.name}", Toast.LENGTH_SHORT).show()
                            return null
                        }
                        ParsedArg(true, parsed)
                    }
                    else -> ParsedArg(true, text)
                }
            }
            else -> ParsedArg(false, null)
        }
    }

    private fun updateExecResultViews(
        views: CommandDetailViews,
        stdout: String?,
        stderr: String?,
        result: Any?,
        statusLine: String
    ) {
        views.resultStatus.text = statusLine
        views.stdoutView.text = stdout?.takeIf { it.isNotBlank() } ?: "<none>"
        views.stderrView.text = stderr?.takeIf { it.isNotBlank() } ?: "<none>"
        views.resultView.text = truncateJson(result?.let { toPrettyJson(it) } ?: "<none>", 2000)
    }

    private fun setExecInProgress(views: CommandDetailViews, inProgress: Boolean) {
        views.runButton.isEnabled = !inProgress
        views.rawJsonToggle.isEnabled = !inProgress
        views.rawJsonInput.isEnabled = !inProgress
        setArgsInputsEnabled(views.argsContainer, !inProgress && !views.rawJsonToggle.isChecked)
    }

    private fun resetExecViews(views: CommandDetailViews) {
        views.resultStatus.text = ""
        views.stdoutView.text = ""
        views.stderrView.text = ""
        views.resultView.text = ""
        lastExecRequestPayload = null
        lastExecResponsePayload = null
        lastExecHttpStatus = null
    }

    private fun setArgsInputsEnabled(container: LinearLayout, enabled: Boolean) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.isEnabled = enabled
            if (child is LinearLayout) {
                setArgsInputsEnabled(child, enabled)
            }
        }
        container.alpha = if (enabled) 1f else 0.5f
    }

    private fun buildArgLabel(name: String, spec: ArgSpec): String {
        val typeLabel = spec.type ?: "unknown"
        val requiredLabel = if (spec.required) " *" else ""
        return "$name ($typeLabel)$requiredLabel"
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

    private fun toPrettyJson(value: Any?): String {
        if (value == null) {
            return "<none>"
        }
        val buffer = Buffer()
        val writer = JsonWriter.of(buffer).apply { setIndent("  ") }
        anyAdapter.toJson(writer, value)
        return buffer.readUtf8()
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

    private fun copyExecDiagnostics() {
        val diagnosticsText = buildExecDiagnosticsText()
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("CCC Exec Diagnostics", diagnosticsText))
        Toast.makeText(this, "Command exec diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
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
        val execRequest = lastExecRequestPayload?.let { redactToken(it) } ?: "<none>"
        val execResponse = lastExecResponsePayload ?: "<none>"
        val execStatus = lastExecHttpStatus ?: "<none>"
        val uploadRequest = lastUploadRequestPayload ?: "<none>"
        val uploadResponse = lastUploadResponsePayload ?: "<none>"
        val uploadStatus = lastUploadHttpStatus ?: "<none>"
        val importRequest = lastImportRequestPayload?.let { redactToken(it) } ?: "<none>"
        val importResponse = lastImportResponsePayload ?: "<none>"
        val importStatus = lastImportHttpStatus ?: "<none>"
        val exportRequest = lastExportRequestPayload?.let { redactToken(it) } ?: "<none>"
        val exportResponse = lastExportResponsePayload ?: "<none>"
        val exportStatus = lastExportHttpStatus ?: "<none>"
        val exportDownloadStatus = lastExportDownloadStatus ?: "<none>"
        val exportSummary = lastExportResponse?.let { formatExportDetails(it).trim() } ?: "<none>"
        val fileSummary = selectedFile?.let { buildFileStatusText(it) } ?: "<none>"
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
            appendLine()
            appendLine("last_exec_status: $execStatus")
            appendLine("last_exec_request_payload:")
            appendLine("  ${truncateJson(execRequest, 2000).replace("\n", "\n  ")}")
            appendLine("last_exec_response_payload:")
            appendLine("  ${truncateJson(execResponse, 2000).replace("\n", "\n  ")}")
            appendLine()
            appendLine("file_selection:")
            appendLine("  ${fileSummary.replace("\n", "\n  ")}")
            appendLine()
            appendLine("upload_status: $uploadStatus")
            appendLine("upload_request:")
            appendLine("  ${truncateJson(uploadRequest, 2000).replace("\n", "\n  ")}")
            appendLine("upload_response:")
            appendLine("  ${truncateJson(uploadResponse, 2000).replace("\n", "\n  ")}")
            appendLine()
            appendLine("import_status: $importStatus")
            appendLine("import_request:")
            appendLine("  ${truncateJson(importRequest, 2000).replace("\n", "\n  ")}")
            appendLine("import_response:")
            appendLine("  ${truncateJson(importResponse, 2000).replace("\n", "\n  ")}")
            appendLine()
            appendLine("export_status: $exportStatus")
            appendLine("export_request:")
            appendLine("  ${truncateJson(exportRequest, 2000).replace("\n", "\n  ")}")
            appendLine("export_response:")
            appendLine("  ${truncateJson(exportResponse, 2000).replace("\n", "\n  ")}")
            appendLine("export_download_status: $exportDownloadStatus")
            appendLine("export_summary:")
            appendLine("  ${exportSummary.replace("\n", "\n  ")}")
        }
    }

    private fun buildExecDiagnosticsText(): String {
        val timestamp = ZonedDateTime.now().toString()
        val commandName = selectedCommand?.name ?: selectedCommandName ?: "<none>"
        val request = lastExecRequestPayload?.let { redactToken(it) } ?: "<none>"
        val response = lastExecResponsePayload ?: "<none>"
        val status = lastExecHttpStatus ?: "<none>"
        return buildString {
            appendLine("CCC command exec diagnostics")
            appendLine("timestamp: $timestamp")
            appendLine("command: $commandName")
            appendLine("status: $status")
            appendLine()
            appendLine("request_payload:")
            appendLine("  ${truncateJson(request, 2000).replace("\n", "\n  ")}")
            appendLine()
            appendLine("response_payload:")
            appendLine("  ${truncateJson(response, 2000).replace("\n", "\n  ")}")
        }
    }

    private fun redactToken(payload: String): String {
        val token = AppConfig.token
        if (token.isBlank()) {
            return payload
        }
        return payload.replace(token, maskToken(token))
    }

    private data class SelectedFileInfo(
        val uri: Uri,
        val name: String,
        val size: Long?,
        val mime: String?
    )

    private class ContentUriRequestBody(
        private val context: Context,
        private val uri: Uri,
        private val contentType: okhttp3.MediaType,
        private val contentLength: Long?
    ) : okhttp3.RequestBody() {
        override fun contentType(): okhttp3.MediaType = contentType

        override fun contentLength(): Long = contentLength ?: -1L

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open input stream for $uri")
            inputStream.use { input ->
                sink.writeAll(input.source())
            }
        }
    }

    private enum class Section {
        CAPABILITIES,
        COMMANDS
    }

    private data class CommandDetailViews(
        val container: LinearLayout,
        val detailView: TextView,
        val argsTitle: TextView,
        val argsHint: TextView,
        val argsContainer: LinearLayout,
        val rawJsonToggle: Switch,
        val rawJsonInput: EditText,
        val runButton: Button,
        val resultTitle: TextView,
        val resultStatus: TextView,
        val stdoutView: TextView,
        val stderrView: TextView,
        val resultView: TextView,
        val copyResultButton: Button
    )

    private data class ArgInput(
        val name: String,
        val spec: ArgSpec,
        val view: View
    )

    private data class ParsedArg(
        val shouldInclude: Boolean,
        val value: Any?
    )
}
