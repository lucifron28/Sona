package com.example.sona.downloader

import java.net.URI
import java.net.URLDecoder

internal fun String.normalizedDownloadUrl(): String {
    val trimmedUrl = trim()
    val uri = runCatching { URI(trimmedUrl) }.getOrNull() ?: return trimmedUrl
    if (!uri.isYoutubeHost()) return trimmedUrl

    val videoId = uri.youtubeVideoId() ?: return trimmedUrl
    return "https://www.youtube.com/watch?v=$videoId"
}

private fun URI.isYoutubeHost(): Boolean {
    val normalizedHost = host?.lowercase() ?: return false
    return normalizedHost == "youtu.be" ||
        normalizedHost.endsWith(".youtu.be") ||
        normalizedHost == "youtube.com" ||
        normalizedHost.endsWith(".youtube.com")
}

private fun URI.youtubeVideoId(): String? {
    val normalizedHost = host?.lowercase().orEmpty()
    if (normalizedHost == "youtu.be" || normalizedHost.endsWith(".youtu.be")) {
        return pathSegmentAt(0)
    }

    return when (path.orEmpty().trimEnd('/')) {
        "/watch" -> queryParameter("v")
        else -> pathVideoId()
    }
}

private fun URI.pathVideoId(): String? =
    when (pathSegmentAt(0)) {
        "embed",
        "live",
        "shorts",
        -> pathSegmentAt(1)
        else -> null
    }

private fun URI.pathSegmentAt(index: Int): String? =
    path
        ?.split('/')
        ?.filter { it.isNotBlank() }
        ?.getOrNull(index)
        ?.takeIf { it.isNotBlank() }

private fun URI.queryParameter(name: String): String? =
    rawQuery
        ?.split('&')
        ?.asSequence()
        ?.mapNotNull { parameter ->
            val key = parameter.substringBefore('=', missingDelimiterValue = parameter)
            val value = parameter.substringAfter('=', missingDelimiterValue = "")
            if (key == name) decodeUrlComponent(value) else null
        }
        ?.firstOrNull { it.isNotBlank() }

private fun decodeUrlComponent(value: String): String =
    runCatching {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrDefault(value)
