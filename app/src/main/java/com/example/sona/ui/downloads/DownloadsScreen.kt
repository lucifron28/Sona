package com.example.sona.ui.downloads
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sona.domain.model.DownloadItem
import com.example.sona.domain.model.DownloadStatus

@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues,
    uiState: DownloadsUiState,
    onUrlChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onCancelDownload: (Long) -> Unit,
    onRetryDownload: (Long) -> Unit,
    onPlayDownload: (Long) -> Unit,
    onRemoveHistory: (Long) -> Unit,
    onSnackbarShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )
    val importClick = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onImportClick()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }
    var detailsDownload by remember { mutableStateOf<DownloadItem?>(null) }

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = uiState.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Authorized audio URL") },
                singleLine = true,
            )
            Button(
                onClick = importClick,
                enabled = uiState.url.isNotBlank(),
            ) {
                Icon(imageVector = Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Import")
            }

            uiState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.downloads.isEmpty()) {
                Text(
                    text = "No imports yet. Add an authorized audio URL to get started.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = uiState.downloads,
                        key = { download -> download.id },
                    ) { download ->
                        DownloadRow(
                            download = download,
                            onCancel = { onCancelDownload(download.id) },
                            onRetry = { onRetryDownload(download.id) },
                            onPlay = { onPlayDownload(download.id) },
                            onRemoveHistory = { onRemoveHistory(download.id) },
                            onShowDetails = { detailsDownload = download },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    detailsDownload?.let { download ->
        DownloadDetailsDialog(
            download = download,
            onDismiss = { detailsDownload = null },
        )
    }
}

@Composable
private fun DownloadRow(
    download: DownloadItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit,
    onRemoveHistory: () -> Unit,
    onShowDetails: () -> Unit,
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
                if (download.status.inProgress) {
                    DownloadProgress(download = download)
                }
                if (download.status == DownloadStatus.FAILED) {
                    Text(
                        text = "Import failed. Open details for the error.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (download.status) {
                    DownloadStatus.QUEUED,
                    DownloadStatus.FETCHING_METADATA,
                    DownloadStatus.DOWNLOADING,
                    DownloadStatus.EXTRACTING,
                    -> IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Cancel download",
                        )
                    }
                    DownloadStatus.FAILED,
                    DownloadStatus.CANCELLED,
                    -> IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry download",
                        )
                    }
                    DownloadStatus.COMPLETED -> IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play downloaded track",
                        )
                    }
                }
                if (!download.status.inProgress) {
                    IconButton(onClick = onRemoveHistory) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove download history",
                        )
                    }
                }
                IconButton(onClick = onShowDetails) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Download details",
                    )
                }
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
        download.status == DownloadStatus.EXTRACTING

    if (determinate) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinearProgressIndicator(
                progress = { (download.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = download.progressPercentLabel,
                modifier = Modifier.widthIn(min = 44.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    }
}

@Composable
private fun DownloadDetailsDialog(
    download: DownloadItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = download.title ?: "Download details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Status: ${download.status.label}")
                Text(text = "URL: ${download.url}")
                download.workId?.let { Text(text = "Work ID: $it") }
                download.diagnosticMessage?.let { Text(text = "Latest output: $it") }
                download.errorMessage?.let { Text(text = "Error: $it") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
    )
}

private val DownloadStatus.label: String
    get() = when (this) {
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.FETCHING_METADATA -> "Preparing"
        DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadStatus.EXTRACTING -> "Adding to library"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

private val DownloadItem.statusLine: String
    get() = when (status) {
        DownloadStatus.QUEUED -> "Waiting for a connection or a free download slot"
        DownloadStatus.FETCHING_METADATA -> "Preparing audio import"
        DownloadStatus.DOWNLOADING -> "Downloading · ${progressPercentLabel}"
        DownloadStatus.EXTRACTING -> "Adding to library · ${progressPercentLabel}"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELLED -> "Cancelled"
    }

private val DownloadItem.progressPercentLabel: String
    get() = "${progress.coerceIn(0f, 100f).toInt()}%"

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
