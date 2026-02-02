package ccc.client.ui

enum class StatusLevel {
    Neutral,
    Ok,
    Error
}

data class HomeUiState(
    val statusText: String = "Connecting…",
    val statusLevel: StatusLevel = StatusLevel.Neutral,
    val detailsText: String = "",
    val messageLines: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val lastAction: String? = null,
    val lastActionSummary: String? = null,
    val infoSummary: String = "<none>",
    val versionSummary: String = "<none>"
)
