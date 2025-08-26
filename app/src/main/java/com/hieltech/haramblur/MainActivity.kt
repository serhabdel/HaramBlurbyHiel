package com.hieltech.haramblur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hieltech.haramblur.ui.HomeScreen
import com.hieltech.haramblur.ui.BlockAppsAndSitesScreen
import com.hieltech.haramblur.ui.SettingsScreen
import com.hieltech.haramblur.ui.DebugScreen
import com.hieltech.haramblur.ui.LogsViewerScreen
import com.hieltech.haramblur.ui.SupportScreen
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appBlockingManager: AppBlockingManager

    @Inject
    lateinit var siteBlockingManager: EnhancedSiteBlockingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Test app detection
        lifecycleScope.launch {
            try {
                val installedApps = appBlockingManager.getInstalledApps()
                android.util.Log.d("MainActivity", "Found ${installedApps.size} installed apps")
                if (installedApps.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "Sample apps: ${installedApps.take(5).joinToString { it.appName }}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error getting installed apps", e)
            }
        }

        setContent {
            HaramBlurTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToDebug = { navController.navigate("debug") },
                                onNavigateToBlockApps = { navController.navigate("block_apps_sites") },
                                onNavigateToBlockSites = { navController.navigate("block_apps_sites") },
                                onNavigateToSupport = { navController.navigate("support") },
                                onNavigateToLogs = { navController.navigate("logs") },
                                appBlockingManager = appBlockingManager,
                                siteBlockingManager = siteBlockingManager
                            )
                        }
                        composable("block_apps_sites") {
                            BlockAppsAndSitesScreen(
                                onNavigateBack = { navController.popBackStack() },
                                appBlockingManager = appBlockingManager,
                                siteBlockingManager = siteBlockingManager
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLogs = { navController.navigate("logs") },
                                onNavigateToSupport = { navController.navigate("support") }
                            )
                        }
                        composable("logs") {
                            LogsViewerScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("support") {
                            SupportScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLogs = { navController.navigate("logs") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("debug") {
                            DebugScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}