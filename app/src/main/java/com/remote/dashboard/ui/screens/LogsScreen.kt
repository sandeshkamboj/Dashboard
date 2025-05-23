package com.remote.dashboard.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.remote.dashboard.data.SupabaseRepository
import com.remote.dashboard.model.FileEntry
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun LogsScreen(deviceId: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<FileEntry?>(null) }
    var logContent by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }

    // Fetch all log files
    LaunchedEffect(Unit) {
        loading = true
        logs = SupabaseRepository.fetchFiles("logs")
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Device Logs",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No error logs found.")
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(logs) { log ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                scope.launch {
                                    val bytes = SupabaseRepository.downloadFile("logs", log.path)
                                    logContent = if (bytes.isNotEmpty()) bytes.decodeToString() else "Failed to load log."
                                    selectedLog = log
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            log.name,
                            Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            scope.launch { downloadAndOpenLog(context, log) }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                        IconButton(onClick = { confirmDelete = log }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        // Log content dialog
        if (selectedLog != null && logContent != null) {
            AlertDialog(
                onDismissRequest = {
                    selectedLog = null
                    logContent = null
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedLog = null
                        logContent = null
                    }) { Text("Close") }
                },
                title = { Text(selectedLog?.name ?: "Log") },
                text = {
                    Text(
                        logContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(min = 100.dp, max = 300.dp)
                    )
                }
            )
        }

        // Delete confirmation dialog
        confirmDelete?.let { log ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val ok = SupabaseRepository.deleteFile("logs", log.path)
                            if (ok) logs = logs.filter { it != log }
                            else Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                            confirmDelete = null
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                },
                title = { Text("Delete Log") },
                text = { Text("Are you sure you want to delete '${log.name}'?") }
            )
        }
    }
}

// Helper to download and open log file as text
suspend fun downloadAndOpenLog(context: Context, log: FileEntry) {
    val bytes = SupabaseRepository.downloadFile("logs", log.path)
    if (bytes.isEmpty()) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        return
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val outFile = File(dir, log.name)
    FileOutputStream(outFile).use { it.write(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, "text/plain")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Open log file"))
}