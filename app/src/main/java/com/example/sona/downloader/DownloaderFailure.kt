package com.example.sona.downloader

import java.io.IOException

internal const val MAX_DOWNLOAD_RETRIES = 3

internal fun String?.cleanedDownloaderLine(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(WHITESPACE, " ")
        ?.take(MAX_DIAGNOSTIC_LENGTH)

internal fun Throwable.userVisibleDownloadMessage(lastDownloaderLine: String?): String =
    listOfNotNull(
        message?.takeIf { it.isNotBlank() },
        cause?.message?.takeIf { it.isNotBlank() },
        lastDownloaderLine?.let { "Downloader stopped after: $it" },
    ).firstOrNull() ?: "Downloader failed without a detailed message. Check Settings > Download support, then retry."

internal fun Throwable.shouldRetryDownload(
    lastDownloaderLine: String?,
    runAttemptCount: Int,
): Boolean = runAttemptCount < MAX_DOWNLOAD_RETRIES &&
    isTransientDownloadFailure(lastDownloaderLine)

internal fun Throwable.isTransientDownloadFailure(lastDownloaderLine: String?): Boolean {
    var current: Throwable? = this
    var inspectedCauseCount = 0
    while (current != null && inspectedCauseCount < MAX_CAUSE_DEPTH) {
        if (current is IOException) return true
        current = current.cause?.takeUnless { it === current }
        inspectedCauseCount += 1
    }

    val diagnosticText = listOfNotNull(
        message,
        cause?.message,
        lastDownloaderLine,
    ).joinToString(separator = "\n").lowercase()
    val transientHttpError = Regex("""http(?: error)?\s*(?:408|425|429|5\d{2})""")
        .containsMatchIn(diagnosticText)
    return transientHttpError || listOf(
        "timed out",
        "timeout",
        "connection reset",
        "connection refused",
        "network is unreachable",
        "temporary failure",
        "temporarily unavailable",
        "remote end closed connection",
        "unexpected eof",
    ).any(diagnosticText::contains)
}

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

private val WHITESPACE = Regex("\\s+")
private const val MAX_CAUSE_DEPTH = 16
private const val MAX_DIAGNOSTIC_LENGTH = 180
