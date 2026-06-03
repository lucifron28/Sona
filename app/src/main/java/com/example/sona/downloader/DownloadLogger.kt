package com.example.sona.downloader

import android.util.Log

object DownloadLogger {
    private const val TAG = "SonaDownloader"

    fun info(downloadId: Long?, message: String) {
        Log.i(TAG, format(downloadId, message))
    }

    fun debug(downloadId: Long?, message: String) {
        Log.d(TAG, format(downloadId, message))
    }

    fun error(downloadId: Long?, message: String, error: Throwable? = null) {
        Log.e(TAG, format(downloadId, message), error)
    }

    private fun format(downloadId: Long?, message: String): String =
        if (downloadId == null) {
            message
        } else {
            "downloadId=$downloadId $message"
        }
}
