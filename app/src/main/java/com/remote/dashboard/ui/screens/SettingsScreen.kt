package com.remote.dashboard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var darkMode by remember { mutableStateOf(false) }

    // If you want system-wide theming, tie this to a ViewModel or SettingsStore instead of local state!
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Brightness4, contentDescription = "Theme", Modifier.padding(end = 12.dp))
            Text("Dark mode")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = darkMode,
                onCheckedChange = { darkMode = it /* (Tie this to your app-level theme!) */ }
            )
        }

        Divider()

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    // Open your GitHub repo or website
                    val url = "https://github.com/YOUR_REPO"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                .padding(vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Code, contentDescription = null, Modifier.padding(end = 12.dp))
            Text("Source code / GitHub")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    // Open support link or email
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@example.com")
                    }
                    context.startActivity(intent)
                }
                .padding(vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, Modifier.padding(end = 12.dp))
            Text("Contact support")
        }

        Spacer(Modifier.weight(1f))

        Text(
            "App version 1.0.0\n© 2025 YourName",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
        )
    }
}