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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.maps.android.compose.*
import com.remote.dashboard.data.SupabaseRepository
import com.remote.dashboard.model.FileEntry
import com.remote.dashboard.model.LocationEvent
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(deviceId: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locations by remember { mutableStateOf<List<Pair<LocationEvent, FileEntry>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selectedLoc by remember { mutableStateOf<LocationEvent?>(null) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }

    // Fetch all location files and parse them
    LaunchedEffect(Unit) {
        loading = true
        val entries = SupabaseRepository.fetchFiles("location")
        val locs: MutableList<Pair<LocationEvent, FileEntry>> = mutableListOf()
        for (file in entries) {
            val bytes = SupabaseRepository.downloadFile("location", file.path)
            if (bytes.isEmpty()) continue
            try {
                val json = bytes.decodeToString()
                val obj = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject
                val event = LocationEvent(
                    lat = obj["lat"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    lng = obj["lng"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    timestamp = obj["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                )
                locs.add(event to file)
            } catch (_: Exception) { }
        }
        locations = locs.sortedByDescending { it.first.timestamp }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (locations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No locations found.")
        }
        return
    }

    var cameraPositionState = rememberCameraPositionState {
        position = CameraPositionState().position.copy(
            target = LatLng(
                locations.first().first.lat,
                locations.first().first.lng
            ),
            zoom = 8f
        )
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(2f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                locations.forEach { (loc, file) ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.lat, loc.lng)),
                        title = Date(loc.timestamp).toString(),
                        snippet = file.name,
                        onClick = {
                            selectedLoc = loc
                            // Animate to marker
                            scope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.lat, loc.lng), 15f
                                    )
                                )
                            }
                            false
                        }
                    )
                }
            }
        }

        Divider()

        // Location List
        LazyColumn(
            Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            items(locations) { (loc, file) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLoc = loc
                            scope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.lat, loc.lng), 15f
                                    )
                                )
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "(${loc.lat.format(6)}, ${loc.lng.format(6)})",
                        Modifier.weight(1f)
                    )
                    Text(
                        text = Date(loc.timestamp).formatForDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        scope.launch { downloadAndOpenLocation(context, file) }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                    IconButton(onClick = { confirmDelete = file }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }

        // Delete confirmation dialog
        confirmDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val ok = SupabaseRepository.deleteFile("location", file.path)
                            if (ok) locations = locations.filter { it.second != file }
                            else Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                            confirmDelete = null
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                },
                title = { Text("Delete Location") },
                text = { Text("Are you sure you want to delete '${file.name}'?") }
            )
        }
    }
}

// Helper extension to format latitude/longitude
fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Date.formatForDisplay(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(this)

// Helper to download and open location file as JSON
suspend fun downloadAndOpenLocation(context: Context, file: FileEntry) {
    val bytes = SupabaseRepository.downloadFile("location", file.path)
    if (bytes.isEmpty()) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
        return
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val outFile = File(dir, file.name)
    FileOutputStream(outFile).use { it.write(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, "application/json")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Open location JSON"))
}