package com.hieltech.haramblur

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.HomeScreenResponsive
import com.hieltech.haramblur.ui.UnifiedBlockingScreenResponsive
import com.hieltech.haramblur.ui.SettingsScreen
import com.hieltech.haramblur.ui.DebugScreen
import com.hieltech.haramblur.ui.LogsViewerScreen
import com.hieltech.haramblur.ui.SupportScreen
import com.hieltech.haramblur.ui.DiagnosticsScreen
import com.hieltech.haramblur.ui.insights.InsightsScreen
import com.hieltech.haramblur.ui.StreamlinedOnboardingScreen
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.ui.MainScreen
import com.hieltech.haramblur.ui.PrayerScreen
import com.hieltech.haramblur.ui.PermissionHelper
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.ui.components.ModernNavigationBar
import com.hieltech.haramblur.ui.components.ModernNavigationDrawerContent
import com.hieltech.haramblur.ui.NavRoutes
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.utils.LocaleUtils
import com.hieltech.haramblur.ui.SettingsViewModel
import com.hieltech.haramblur.detection.Language
import com.hieltech.haramblur.presentation.InitializationHelper
import dagger.hilt.android.AndroidEntryPoint
import com.hieltech.haramblur.widget.WidgetUpdateManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var appBlockingManager: AppBlockingManager

    @Inject
    lateinit var siteBlockingManager: EnhancedSiteBlockingManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var permissionHelper: PermissionHelper
    
    @Inject
    lateinit var initializationHelper: InitializationHelper

    override fun attachBaseContext(base: Context) {
        // Wrap base context with saved language before any UI is created
        val prefs = base.getSharedPreferences("haramblur_settings", Context.MODE_PRIVATE)
        val langName = prefs.getString("preferred_language", Language.ENGLISH.name) ?: Language.ENGLISH.name
        val lang = try { Language.valueOf(langName) } catch (_: Exception) { Language.ENGLISH }
        val wrapped = LocaleUtils.wrap(base, lang)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // Observe language change events and recreate activity to refresh resources
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.languageChangeEvents.collect {
                    android.util.Log.d("MainActivity", "Language change event received. Recreating activity.")
                    recreate()
                }
            }
        }

        // Initialize test URLs, fix database issues, and apply first-time defaults
        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "Initializing app and applying first-time defaults...")
                
                // Apply first-time defaults (High Quality mode) - skips performance assessment
                settingsRepository.applyFirstTimeDefaults()
                
                // Fix any database issues first
                initializationHelper.fixDatabaseIssues()
                
                // Initialize test NSFW URLs including nsfw.ma
                initializationHelper.initializeTestUrls()
                
                // Refresh widgets when app starts - ensures widgets have latest data
                WidgetUpdateManager.refreshAllWidgets(this@MainActivity)
                
                android.util.Log.d("MainActivity", "✅ App initialization completed with High Quality defaults")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error during app initialization", e)
            }
        }
        
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
            val settings by settingsRepository.settings.collectAsState()
            HaramBlurTheme(
                appTheme = settings.appTheme,
                preferredLanguage = settings.preferredLanguage
            ) {
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

                // Check onboarding and permissions status with enhanced logic
                LaunchedEffect(Unit) {
                    try {
                        val settings = settingsRepository.getCurrentSettings()

                        // Force refresh permissions before checking
                        permissionHelper.updatePermissionStatuses()
                        kotlinx.coroutines.delay(300) // Allow time for permission updates

                        val permissionStatus = permissionHelper.getEnhancedBlockingPermissionStatus()

                        // Enhanced wizard logic: show wizard only when needed
                        // Skip wizard if all critical permissions are granted (handles reinstall case)
                        // Only show wizard if permissions are incomplete
                        val shouldShowWizard = !permissionStatus.isComplete
                        
                        // If permissions are complete but onboarding wasn't marked, mark it now
                        if (permissionStatus.isComplete && !settings.onboardingCompleted) {
                            settingsRepository.markOnboardingCompleted()
                        }

                        android.util.Log.d("MainActivity", "Setup check - Onboarding: ${settings.onboardingCompleted}, " +
                            "Permissions complete: ${permissionStatus.isComplete}, " +
                            "Accessibility: ${permissionStatus.accessibilityServiceGranted}, " +
                            "Show wizard: $shouldShowWizard")

                        startDestination = if (shouldShowWizard) NavRoutes.PERMISSION_WIZARD else NavRoutes.HOME
                        isInitializing = false
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error checking setup status", e)
                        startDestination = NavRoutes.PERMISSION_WIZARD // Safer fallback - show wizard on error
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
                                text = "Checking permissions and setup...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "This may take a moment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                                                    ModernNavigationDrawerContent(
            currentRoute = currentRoute,
            onNavigateToInsights = {
                if (currentRoute != NavRoutes.INSIGHTS) {
                    navController.navigate(NavRoutes.INSIGHTS)
                }
            },
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
            onNavigateToDiagnostics = {
                if (currentRoute != NavRoutes.DIAGNOSTICS) {
                    navController.navigate(NavRoutes.DIAGNOSTICS)
                }
            },
            onCloseDrawer = { scope.launch { drawerState.close() } }
        )
                        }
                    ) {
                        Scaffold(
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
                                    onNavigateToPrayer = {
                                        if (currentRoute != NavRoutes.PRAYER) {
                                            navController.navigate(NavRoutes.PRAYER) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    },
                                    onNavigateToDhikr = {
                                        if (currentRoute != NavRoutes.DHIKR) {
                                            navController.navigate(NavRoutes.DHIKR) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    },
                                    onNavigateToHadith = {
                                        if (currentRoute != NavRoutes.HADITH) {
                                            navController.navigate(NavRoutes.HADITH) {
                                                launchSingleTop = true
                                                restoreState = true
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                            }
                                        }
                                    },
                                    onNavigateToQuran = {
                                        if (currentRoute != NavRoutes.QURAN) {
                                            navController.navigate(NavRoutes.QURAN) {
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
                                    StreamlinedOnboardingScreen(
                                        onComplete = {
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.PERMISSION_WIZARD) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                                                 composable(NavRoutes.HOME) {
                                                                     MainScreen(
                                                                         onNavigateToSettings = {
                                                                             if (currentRoute != NavRoutes.SETTINGS) {
                                                                                 navController.navigate(NavRoutes.SETTINGS)
                                                                             }
                                                                         },
                                                                         onNavigateToDebug = {
                                                                             if (currentRoute != NavRoutes.DEBUG) {
                                                                                 navController.navigate(NavRoutes.DEBUG)
                                                                             }
                                                                         },
                                                                         onNavigateToPrayer = {
                                                                             if (currentRoute != NavRoutes.PRAYER) {
                                                                                 navController.navigate(NavRoutes.PRAYER)
                                                                             }
                                                                         },
                                                                         onNavigateToDhikr = {
                                                                             if (currentRoute != NavRoutes.DHIKR) {
                                                                                 navController.navigate(NavRoutes.DHIKR)
                                                                             }
                                                                         },
                                                                         onOpenDrawer = { scope.launch { drawerState.open() } }
                                                                     )
                                                                 }
                                composable(NavRoutes.BLOCK_APPS_SITES) {
                                UnifiedBlockingScreenResponsive(
                                    onNavigateBack = { navController.popBackStack() },
                                    appBlockingManager = appBlockingManager,
                                    siteBlockingManager = siteBlockingManager
                                )
                            }
                                composable(NavRoutes.PRAYER) {
                                    PrayerScreen(
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToSettings = {
                                            if (currentRoute != NavRoutes.SETTINGS) {
                                                navController.navigate(NavRoutes.SETTINGS)
                                            }
                                        }
                                    )
                                }
                                composable(NavRoutes.INSIGHTS) {
                                    InsightsScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable(NavRoutes.DHIKR) {
                                    com.hieltech.haramblur.ui.dhikr.DhikrScreen()
                                }
                                composable(NavRoutes.HADITH) {
                                    com.hieltech.haramblur.ui.hadith.HadithScreen(
                                        onHadithClick = { index ->
                                            navController.navigate("hadith_detail/$index")
                                        }
                                    )
                                }
                                composable(
                                    NavRoutes.HADITH_DETAIL,
                                    arguments = listOf(navArgument("hadithIndex") { type = NavType.IntType })
                                ) { backStackEntry ->
                                    val hadithIndex = backStackEntry.arguments?.getInt("hadithIndex") ?: 0
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry(NavRoutes.HADITH)
                                    }
                                    val hadithViewModel: com.hieltech.haramblur.ui.hadith.HadithViewModel = hiltViewModel(parentEntry)
                                    val uiState by hadithViewModel.uiState.collectAsState()
                                    com.hieltech.haramblur.ui.hadith.HadithDetailScreen(
                                        hadiths = uiState.hadiths,
                                        initialIndex = hadithIndex,
                                        appLanguage = uiState.appLanguage,
                                        onBack = { navController.popBackStack() },
                                        onShare = { hadith ->
                                            val shareText = getString(
                                                R.string.hadith_share_text,
                                                hadith.englishText,
                                                hadith.narrator,
                                                hadith.bookName,
                                                hadith.hadithNumber
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            }
                                            startActivity(android.content.Intent.createChooser(intent, getString(R.string.hadith_share)))
                                        }
                                    )
                                }
                                composable(NavRoutes.QURAN) {
                                    com.hieltech.haramblur.ui.quran.QuranScreen(
                                        onSurahClick = { surahNumber ->
                                            navController.navigate("quran_reader/$surahNumber")
                                        }
                                    )
                                }
                                composable(
                                    NavRoutes.QURAN_READER,
                                    arguments = listOf(androidx.navigation.navArgument("surahNumber") { type = androidx.navigation.NavType.IntType })
                                ) { backStackEntry ->
                                    val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry(NavRoutes.QURAN)
                                    }
                                    val quranViewModel: com.hieltech.haramblur.ui.quran.QuranViewModel = hiltViewModel(parentEntry)
                                    com.hieltech.haramblur.ui.quran.QuranReaderScreen(
                                        surahNumber = surahNumber,
                                        viewModel = quranViewModel,
                                        onBack = { navController.popBackStack() },
                                        onShare = { verse ->
                                            val shareText = "${verse.textUthmani}\n\n${verse.translationText}\n\n— ${verse.verseKey}"
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            }
                                            startActivity(android.content.Intent.createChooser(intent, getString(R.string.quran_share)))
                                        }
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
                                composable(NavRoutes.DIAGNOSTICS) {
                                    DiagnosticsScreen(
                                        viewModel = hiltViewModel()
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
        // Refresh permission status when returning from settings with retry logic
        lifecycleScope.launch {
            try {
                // Add a small delay to allow system to update permission status
                kotlinx.coroutines.delay(500)
                permissionHelper.updatePermissionStatuses()

                // Extra retry for accessibility service which can be tricky to detect
                val accessibilityResult = permissionHelper.retryPermissionCheck(
                    "ACCESSIBILITY_SERVICE",
                    maxRetries = 2,
                    delayMs = 1000
                )

                android.util.Log.d("MainActivity", "Accessibility service check result: $accessibilityResult")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error refreshing permissions on resume", e)
            }
        }
    }
}
