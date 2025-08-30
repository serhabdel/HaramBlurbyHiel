package com.hieltech.haramblur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.hieltech.haramblur.ui.HomeScreenResponsive
import com.hieltech.haramblur.ui.UnifiedBlockingScreenResponsive
import com.hieltech.haramblur.ui.SettingsScreen
import com.hieltech.haramblur.ui.DebugScreen
import com.hieltech.haramblur.ui.LogsViewerScreen
import com.hieltech.haramblur.ui.SupportScreen
import com.hieltech.haramblur.ui.PermissionWizardScreen
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.ui.PermissionHelper
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.ui.components.ModernNavigationBar
import com.hieltech.haramblur.ui.components.ModernTopAppBar
import com.hieltech.haramblur.ui.components.ModernNavigationDrawerContent
import com.hieltech.haramblur.ui.NavRoutes
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

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var permissionHelper: PermissionHelper

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
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Define primary routes that show bottom navigation
                val primaryRoutes = NavRoutes.PRIMARY_ROUTES

                // Determine start destination based on onboarding and permissions
                var startDestination by remember { mutableStateOf<String?>("loading") }
                var isInitializing by remember { mutableStateOf(true) }

                // Check onboarding and permissions status
                LaunchedEffect(Unit) {
                    try {
                        val settings = settingsRepository.getCurrentSettings()
                        val permissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()

                        // Show wizard if onboarding not completed OR required permissions missing
                        val shouldShowWizard = !settings.onboardingCompleted ||
                                             !permissionStatus.isComplete

                        startDestination = if (shouldShowWizard) NavRoutes.PERMISSION_WIZARD else NavRoutes.HOME
                        isInitializing = false
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error checking setup status", e)
                        startDestination = NavRoutes.HOME // Fallback to home on error
                        isInitializing = false
                    }
                }

                // Show loading screen while determining start destination
                if (isInitializing || startDestination == "loading") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Initializing HaramBlur...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                                                    ModernNavigationDrawerContent(
                            currentRoute = currentRoute,
                            onNavigateToLogs = {
                                if (currentRoute != NavRoutes.LOGS) {
                                    navController.navigate(NavRoutes.LOGS)
                                }
                            },
                            onNavigateToDebug = {
                                if (currentRoute != NavRoutes.DEBUG) {
                                    navController.navigate(NavRoutes.DEBUG)
                                }
                            },
                            onNavigateToSupport = {
                                if (currentRoute != NavRoutes.SUPPORT) {
                                    navController.navigate(NavRoutes.SUPPORT)
                                }
                            },
                            onCloseDrawer = { scope.launch { drawerState.close() } }
                        )
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                // Only show top bar for non-wizard routes
                                if (currentRoute != NavRoutes.PERMISSION_WIZARD) {
                                    ModernTopAppBar(
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onNavigateToSettings = {
                                            navController.navigate(NavRoutes.SETTINGS) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            bottomBar = {
                                // Only show bottom bar for primary routes
                                if (currentRoute in primaryRoutes) {
                                                                    ModernNavigationBar(
                                    currentRoute = currentRoute,
                                    onNavigateToHome = {
                                        if (currentRoute != NavRoutes.HOME) {
                                            navController.navigate(NavRoutes.HOME) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    },
                                    onNavigateToBlockAppsSites = {
                                        if (currentRoute != NavRoutes.BLOCK_APPS_SITES) {
                                            navController.navigate(NavRoutes.BLOCK_APPS_SITES) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    },
                                    onNavigateToSettings = {
                                        if (currentRoute != NavRoutes.SETTINGS) {
                                            navController.navigate(NavRoutes.SETTINGS) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    }
                                )
                                }
                            }
                        ) { paddingValues ->
                            NavHost(
                                navController = navController,
                                startDestination = startDestination ?: NavRoutes.HOME,
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                composable(NavRoutes.PERMISSION_WIZARD) {
                                    PermissionWizardScreen(
                                        onComplete = {
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.PERMISSION_WIZARD) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                                                composable(NavRoutes.HOME) {
                                                                    HomeScreenResponsive(
                                                                        onNavigateToSettings = {
                                                                            if (currentRoute != NavRoutes.SETTINGS) {
                                                                                navController.navigate(NavRoutes.SETTINGS)
                                                                            }
                                                                        },
                                                                        onNavigateToBlockApps = {
                                                                            if (currentRoute != NavRoutes.BLOCK_APPS_SITES) {
                                                                                navController.navigate(NavRoutes.BLOCK_APPS_SITES)
                                                                            }
                                                                        },
                                                                        onNavigateToBlockSites = {
                                                                            if (currentRoute != NavRoutes.BLOCK_APPS_SITES) {
                                                                                navController.navigate(NavRoutes.BLOCK_APPS_SITES)
                                                                            }
                                                                        },
                                                                        onNavigateToSupport = {
                                                                            if (currentRoute != NavRoutes.SUPPORT) {
                                                                                navController.navigate(NavRoutes.SUPPORT)
                                                                            }
                                                                        },
                                                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                                                        permissionHelper = permissionHelper,
                                                                        appBlockingManager = appBlockingManager,
                                                                        siteBlockingManager = siteBlockingManager
                                                                    )
                                                                }
                                composable(NavRoutes.BLOCK_APPS_SITES) {
                                UnifiedBlockingScreenResponsive(
                                    onNavigateBack = { navController.popBackStack() },
                                    appBlockingManager = appBlockingManager,
                                    siteBlockingManager = siteBlockingManager
                                )
                            }
                                                            composable(NavRoutes.SETTINGS) {
                                    SettingsScreen(
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToLogs = {
                                            if (currentRoute != NavRoutes.LOGS) {
                                                navController.navigate(NavRoutes.LOGS)
                                            }
                                        },
                                        onNavigateToSupport = {
                                            if (currentRoute != NavRoutes.SUPPORT) {
                                                navController.navigate(NavRoutes.SUPPORT)
                                            }
                                        },
                                        onOpenDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }
                                composable(NavRoutes.LOGS) {
                                LogsViewerScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                                                            composable(NavRoutes.SUPPORT) {
                                    SupportScreen(
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToLogs = {
                                            if (currentRoute != NavRoutes.LOGS) {
                                                navController.navigate(NavRoutes.LOGS)
                                            }
                                        },
                                        onNavigateToSettings = {
                                            if (currentRoute != NavRoutes.SETTINGS) {
                                                navController.navigate(NavRoutes.SETTINGS) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                    popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                }
                                            }
                                        }
                                    )
                                }
                                composable(NavRoutes.DEBUG) {
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
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission status when returning from settings
        permissionHelper.updatePermissionStatuses()
    }
}
