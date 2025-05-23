package com.remote.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.remote.dashboard.model.CommandRequest

@Composable
fun CommandLog(
    commandLog: List<CommandRequest>
) {
    Column(Modifier.padding(8.dp)) {
        Text("Command History", style = MaterialTheme.typography.titleMedium)
        if (commandLog.isEmpty()) {
            Text("No commands sent yet.")
        } else {
            commandLog.reversed().take(20).forEach {
                Text("• ${it.type} ${it.options ?: ""} (to ${it.deviceId})")
            }
        }
    }
}