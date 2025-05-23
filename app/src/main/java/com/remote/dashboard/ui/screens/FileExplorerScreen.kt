package com.remote.dashboard.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Upload
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
fun FileExplorerScreen(deviceId: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileTree by remember { mutableStateOf<Map<String, List<FileEntry>>>(emptyMap()) }
    var currentPath by remember { mutableStateOf("/") }
    var loading by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }
    var filesInDir by remember { mutableStateOf<List<FileEntry>>(emptyList()) }

    // File picker launcher for upload
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(uri)
                val name = getFileNameFromUri(context, uri)
                if (inputStream != null && name != null) {
                    val bytes = inputStream.readBytes()
                    val supaPath = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
                    val ok = SupabaseRepository.uploadFile("files", supaPath, bytes)
                    if (ok) {
                        Toast.makeText(context, "Upload complete", Toast.LENGTH_SHORT).show()
                        // Optionally refresh the file list
                        fileTree = fetchFileTree(deviceId)
                        filesInDir = fileTree[currentPath] ?: emptyList()
                    } else {
                        Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Fetch file system index from Supabase (as a JSON object)
    LaunchedEffect(deviceId) {
        loading = true
        fileTree = fetchFileTree(deviceId)
        filesInDir = fileTree[currentPath] ?: emptyList()
        loading = false
    }

    // Update files list on path change
    LaunchedEffect(currentPath, fileTree) {
        filesInDir = fileTree[currentPath] ?: emptyList()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPath != "/") {
                IconButton(onClick = {
                    // Go up one directory
                    val upPath = currentPath.removeSuffix("/").substringBeforeLast("/", "") + "/"
                    currentPath = if (upPath.isBlank()) "/" else upPath
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Up")
                }
            }
            Text(
                currentPath,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { uploadLauncher.launch("*/*") }) {
                Icon(Icons.Default.Upload, contentDescription = "Upload File")
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filesInDir.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files or folders here.")
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(filesInDir) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (entry.type == "dir") {
                                    currentPath = if (currentPath.endsWith("/")) currentPath + entry.name + "/" else "$currentPath/${entry.name}/"
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (entry.type == "dir") {
                            Icon(Icons.Default.Folder, contentDescription = null, Modifier.padding(end = 8.dp))
                            Text(
                                entry.name,
                                Modifier.weight(1f)
                            )
                        } else {
                            Text(entry.name, Modifier.weight(1f))
                            IconButton(onClick = {
                                scope.launch { downloadAndOpenFile(context, entry) }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                            }
                            IconButton(onClick = { confirmDelete = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        confirmDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val ok = SupabaseRepository.deleteFile("files", entry.path)
                            if (ok) {
                                fileTree = fetchFileTree(deviceId)
                                filesInDir = fileTree[currentPath] ?: emptyList()
                            } else {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                            }
                            confirmDelete = null
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                },
                title = { Text("Delete File") },
                text = { Text("Are you sure you want to delete '${entry.name}'?") }
            )
        }
    }
}

// Helper to get file name from URI
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(nameIndex)
    }
}

// Helper to download and open file
suspend fun downloadAndOpenFile(context: Context, entry: FileEntry) {
    val bytes = SupabaseRepository.downloadFile("files", entry.path)
    if (bytes.isEmpty()) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        return
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val outFile = File(dir, entry.name)
    FileOutputStream(outFile).use { it.write(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, "*/*")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Open file"))
}

// Fetch the file system tree (from Supabase, as a JSON file named "$deviceId-filetree.json" in "files" bucket)
suspend fun fetchFileTree(deviceId: String): Map<String, List<FileEntry>> {
    // You must ensure your agent uploads this file regularly!
    return try {
        val bytes = SupabaseRepository.downloadFile("files", "$deviceId-filetree.json")
        if (bytes.isEmpty()) return emptyMap()
        // The JSON should be { "/": [FileEntry, ...], "/folder/": [FileEntry, ...], ... }
        kotlinx.serialization.json.Json.decodeFromString<Map<String, List<FileEntry>>>(bytes.decodeToString())
    } catch (_: Exception) { emptyMap() }
}