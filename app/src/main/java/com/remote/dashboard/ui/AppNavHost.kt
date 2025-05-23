package com.remote.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.remote.dashboard.ui.screens.*

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "commandCenter") {
        composable("commandCenter") { CommandCenterScreen(null, navController) }
        composable("fileViewer/{deviceId}") { backStack ->
            FileViewerScreen(backStack.arguments?.getString("deviceId") ?: "", navController)
        }
        composable("fileExplorer/{deviceId}") { backStack ->
            FileExplorerScreen(backStack.arguments?.getString("deviceId") ?: "", navController)
        }
        composable("location/{deviceId}") { backStack ->
            LocationScreen(backStack.arguments?.getString("deviceId") ?: "", navController)
        }
        composable("logs/{deviceId}") { backStack ->
            LogsScreen(backStack.arguments?.getString("deviceId") ?: "", navController)
        }
        composable("settings") { SettingsScreen(navController) }
    }
}