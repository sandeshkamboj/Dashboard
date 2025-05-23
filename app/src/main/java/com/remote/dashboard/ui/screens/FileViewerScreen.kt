package com.remote.dashboard.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.remote.dashboard.data.SupabaseRepository
import com.remote.dashboard.model.FileEntry
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(deviceId: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fileTabs = listOf("Photos", "Videos", "Audio")
    var selectedTab by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<FileEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }
    val buckets = listOf("media", "media", "audio") // Your Supabase buckets

    // Fetch files when tab changes
    LaunchedEffect(selectedTab) {
        loading = true
        files = SupabaseRepository.fetchFiles(buckets[selectedTab])
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            fileTabs.forEachIndexed { idx, tab ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(tab) }
                )
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0, 1 -> { // Photos/Videos
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        Modifier.weight(1f)
                    ) {
                        items(files) { file ->
                            Box(
                                Modifier
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { previewFile = file }
                            ) {
                                val painter = rememberAsyncImagePainter(
                                    model = "https://$SUPABASE_URL/storage/v1/object/public/${buckets[selectedTab]}/${file.path}",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(4.dp)
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                ) {
                                    IconButton(onClick = { confirmDelete = file }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                ) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            downloadAndOpen(context, buckets[selectedTab], file, selectedTab == 1)
                                        }
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Download")
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Audio
                    LazyColumn(Modifier.weight(1f)) {
                        items(files) { file ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    file.name,
                                    Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { previewFile = file }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                }
                                IconButton(onClick = { confirmDelete = file }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        downloadAndOpen(context, buckets[selectedTab], file, false)
                                    }
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preview Dialog for images/videos/audio
        previewFile?.let { file ->
            AlertDialog(
                onDismissRequest = { previewFile = null },
                confirmButton = { TextButton(onClick = { previewFile = null }) { Text("Close") } },
                title = { Text("Preview: ${file.name}") },
                text = {
                    when (selectedTab) {
                        0 -> {
                            // Image
                            val painter = rememberAsyncImagePainter(
                                model = "https://$SUPABASE_URL/storage/v1/object/public/${buckets[selectedTab]}/${file.path}"
                            )
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.size(300.dp)
                            )
                        }
                        1 -> {
                            // Video - Download and open with external player
                            Button(onClick = {
                                scope.launch {
                                    downloadAndOpen(context, buckets[selectedTab], file, true)
                                }
                            }) { Text("Play Video") }
                        }
                        2 -> {
                            // Audio - Download and open with external player
                            Button(onClick = {
                                scope.launch {
                                    downloadAndOpen(context, buckets[selectedTab], file, false)
                                }
                            }) { Text("Play Audio") }
                        }
                    }
                }
            )
        }

        // Delete confirmation dialog
        confirmDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val ok = SupabaseRepository.deleteFile(buckets[selectedTab], file.path)
                            if (ok) files = files.filter { it != file }
                            else Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                            confirmDelete = null
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                },
                title = { Text("Delete File") },
                text = { Text("Are you sure you want to delete '${file.name}'?") }
            )
        }
    }
}

// Helper to download and open files
suspend fun downloadAndOpen(context: android.content.Context, bucket: String, file: FileEntry, isVideo: Boolean) {
    val bytes = SupabaseRepository.downloadFile(bucket, file.path)
    if (bytes.isEmpty()) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        return
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val outFile = File(dir, file.name)
    FileOutputStream(outFile).use { it.write(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, if (isVideo) "video/*" else if (file.name.endsWith(".mp3") || file.name.endsWith(".wav")) "audio/*" else "image/*")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Open with"))
}

// Ensure this matches your Supabase URL (without https://)
private const val SUPABASE_URL = "YOUR_SUPABASE_URL"