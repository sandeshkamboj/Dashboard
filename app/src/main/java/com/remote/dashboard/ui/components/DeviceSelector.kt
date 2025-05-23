package com.remote.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.remote.dashboard.data.SupabaseRepository
import com.remote.dashboard.model.Device
import kotlinx.coroutines.launch

@Composable
fun DeviceSelector(
    selectedDeviceId: String,
    onDeviceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        devices = SupabaseRepository.fetchDevices()
        loading = false
    }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        Text(
            if (selectedDeviceId.isBlank())
                if (loading) "Loading devices..." else "Select Device"
            else
                devices.find { it.id == selectedDeviceId }?.let { it.name ?: it.id } ?: selectedDeviceId
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            devices.forEach {
                DropdownMenuItem(
                    onClick = {
                        onDeviceSelected(it.id)
                        expanded = false
                    },
                    text = {
                        Column {
                            Text(it.name ?: it.id)
                            if (it.lastSeen != null)
                                Text("Last seen: ${it.lastSeen}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                )
            }
            if (!loading && devices.isEmpty()) {
                DropdownMenuItem(
                    onClick = { expanded = false },
                    text = { Text("No devices found") }
                )
            }
        }
    }
}