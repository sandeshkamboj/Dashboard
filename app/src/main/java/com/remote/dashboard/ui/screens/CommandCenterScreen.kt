package com.remote.dashboard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.remote.dashboard.data.SupabaseRepository
import com.remote.dashboard.model.CommandRequest
import com.remote.dashboard.ui.components.CommandLog
import com.remote.dashboard.ui.components.DeviceSelector
import kotlinx.coroutines.launch

@Composable
fun CommandCenterScreen(
    selectedDeviceId: String?,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentDeviceId by remember { mutableStateOf(selectedDeviceId ?: "") }
    val commandLog = remember { mutableStateListOf<CommandRequest>() }

    DeviceSelector(
        selectedDeviceId = currentDeviceId,
        onDeviceSelected = { currentDeviceId = it }
    )

    Spacer(Modifier.height(16.dp))

    if (currentDeviceId.isBlank()) {
        Text("Please select a device.", Modifier.padding(16.dp))
        return
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        // Repeat command cards for all commands as previously implemented, see previous answers.
        // For brevity, only one sample card shown. (Copy for each command type.)
        item {
            CommandCard(
                title = "Capture Photo",
                content = {
                    var camera by remember { mutableStateOf("rear") }
                    var flash by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Camera: ")
                        DropdownMenuBox(
                            options = listOf("rear", "front"),
                            selected = camera,
                            onSelected = { camera = it }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(flash, onCheckedChange = { flash = it })
                        Text("Flash")
                    }
                    SendButton {
                        val cmd = CommandRequest(
                            deviceId = currentDeviceId,
                            type = "capturePhoto",
                            options = mapOf("camera" to camera, "flash" to flash)
                        )
                        coroutineScope.launch {
                            val ok = SupabaseRepository.sendCommand(cmd)
                            if (ok) commandLog.add(cmd)
                            else Toast.makeText(context, "Failed to send command", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        // ... repeat for all other commands ...
        item { CommandLog(commandLog) }
    }
}

@Composable
private fun CommandCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.padding(8.dp)) {
        Column(Modifier.padding(16.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        })
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        Modifier.padding(top = 8.dp)
    ) { Text("Send") }
}

@Composable
private fun <T> DropdownMenuBox(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selected.toString())
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    onClick = {
                        onSelected(it)
                        expanded = false
                    },
                    text = { Text(it.toString()) }
                )
            }
        }
    }
}