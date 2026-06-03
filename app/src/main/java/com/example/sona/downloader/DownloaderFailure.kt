package com.example.sona.downloader

internal fun String?.cleanedDownloaderLine(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("\\s+"), " ")
        ?.take(MAX_DIAGNOSTIC_LENGTH)

internal fun Throwable.userVisibleDownloadMessage(lastDownloaderLine: String?): String =
    listOfNotNull(
        message?.takeIf { it.isNotBlank() },
        cause?.message?.takeIf { it.isNotBlank() },
        lastDownloaderLine?.let { "Downloader stopped after: $it" },
    ).firstOrNull() ?: "Downloader failed without a detailed message. Try Update, then retry."

internal fun Throwable.shouldRepairYoutube403(
    url: String,
    lastDownloaderLine: String?,
): Boolean {
    if (!url.isYoutubeUrl()) return false

    val diagnosticText = listOfNotNull(
        message,
        cause?.message,
        lastDownloaderLine,
    ).joinToString(separator = "\n")
        .lowercase()

    return diagnosticText.contains("403") && diagnosticText.contains("forbidden")
}

private fun String.isYoutubeUrl(): Boolean =
    contains("youtube.com", ignoreCase = true) ||
        contains("youtu.be", ignoreCase = true)

private const val MAX_DIAGNOSTIC_LENGTH = 180
