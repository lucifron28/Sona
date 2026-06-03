package com.example.sona.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus

@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues,
    uiState: DownloadsUiState,
    onUrlChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onUpdateDownloader: () -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onSnackbarShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarEvent?.id) {
        uiState.snackbarEvent?.let { event ->
            snackbarIsError = event.isError
            snackbarHostState.showSnackbar(event.message)
            onSnackbarShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (snackbarIsError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        SuccessContainer
                    },
                    contentColor = if (snackbarIsError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        Color.White
                    },
                )
            }
        },
    ) { snackbarPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(snackbarPadding)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(
                onClick = onUpdateDownloader,
                enabled = !uiState.isUpdatingDownloader,
            ) {
                Icon(
                    imageVector = Icons.Filled.Update,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Update")
            }
        }

        OutlinedTextField(
            value = uiState.url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Authorized audio URL") },
            singleLine = true,
        )
        Button(
            onClick = onImportClick,
            enabled = uiState.url.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Import")
        }

        uiState.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.downloads.isEmpty()) {
            Text(
                text = "No URL imports yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = uiState.downloads,
                    key = { download -> download.id },
                ) { download ->
                    DownloadRow(
                        download = download,
                        onDelete = { onDeleteDownload(download.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    }
}

@Composable
private fun DownloadRow(
    download: DownloadItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = download.title ?: download.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = download.statusLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                download.diagnosticMessage?.let { diagnosticMessage ->
                    Text(
                        text = diagnosticMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (download.status.inProgress) {
                    DownloadProgress(download = download)
                }
                download.errorMessage?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete import",
                )
            }
        },
    )
}

@Composable
private fun DownloadProgress(
    download: DownloadItem,
    modifier: Modifier = Modifier,
) {
    val determinate = download.status == DownloadStatus.DOWNLOADING ||
        download.status == DownloadStatus.EXTRACTING ||
        download.status == DownloadStatus.COMPLETED

    if (determinate) {
        LinearProgressIndicator(
            progress = { (download.progress / 100f).coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    }
}

private val DownloadStatus.label: String
    get() = when (this) {
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.FETCHING_METADATA -> "Fetching metadata"
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.EXTRACTING -> "Finalizing"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

private val DownloadItem.statusLine: String
    get() {
        val percent = when (status) {
            DownloadStatus.DOWNLOADING,
            DownloadStatus.EXTRACTING,
            -> " ${progress.coerceIn(0f, 100f).toInt()}%"
            DownloadStatus.COMPLETED -> " 100%"
            else -> ""
        }
        val workSuffix = workId?.take(8)?.let { " - work $it" } ?: ""
        return status.label + percent + workSuffix
    }

private val DownloadStatus.inProgress: Boolean
    get() = when (this) {
        DownloadStatus.QUEUED,
        DownloadStatus.FETCHING_METADATA,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.EXTRACTING,
        -> true
        DownloadStatus.COMPLETED,
        DownloadStatus.FAILED,
        DownloadStatus.CANCELLED,
        -> false
    }

private val SuccessContainer = Color(0xFF2E7D32)
