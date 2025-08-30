package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.AppInfo
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.data.database.BlockedSiteEntity
import com.hieltech.haramblur.data.AppRegistry
import com.hieltech.haramblur.utils.SocialMediaDetector
import com.hieltech.haramblur.ui.components.SocialMediaAppCard
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Simplified and stable responsive blocking screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedBlockingScreenResponsive(
    onNavigateBack: () -> Unit = {},
    appBlockingManager: AppBlockingManager? = null,
    siteBlockingManager: EnhancedSiteBlockingManager? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    // Simple responsive logic - use single layout for all screen sizes
    UnifiedBlockingScreenSimple(
        onNavigateBack = onNavigateBack,
        appBlockingManager = appBlockingManager,
        siteBlockingManager = siteBlockingManager
    )
}

/**
 * Simplified and stable blocking screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedBlockingScreenSimple(
    onNavigateBack: () -> Unit,
    appBlockingManager: AppBlockingManager?,
    siteBlockingManager: EnhancedSiteBlockingManager?
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Simplified state management - only essential state
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var dataLoaded by remember { mutableStateOf(false) }

    // Data state - load only when needed
    var socialMediaApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<com.hieltech.haramblur.data.database.BlockedAppEntity>>(emptyList()) }
    var customBlockedSites by remember { mutableStateOf<List<BlockedSiteEntity>>(emptyList()) }
    var socialMediaStats by remember { mutableStateOf<AppBlockingManager.SocialMediaStats?>(null) }

    // Load data function - simplified and safer
    val loadData = remember {
        suspend {
            if (isLoading) {
                // Prevent multiple simultaneous loads
            } else {
                isLoading = true
                try {
                    // Load data based on current tab to reduce memory usage
                    when (selectedTab) {
                        0 -> {
                            // Only load social media data when on social media tab
                            val installedApps = appBlockingManager?.getInstalledApps() ?: emptyList()
                            socialMediaApps = SocialMediaDetector.getInstalledSocialMediaApps(installedApps)
                            blockedApps = appBlockingManager?.getBlockedApps() ?: emptyList()
                            socialMediaStats = appBlockingManager?.getSocialMediaBlockingStats()
                        }
                        1 -> {
                            // Only load website data when on websites tab
                            customBlockedSites = siteBlockingManager?.getCustomBlockedWebsites() ?: emptyList()
                        }
                    }
                    dataLoaded = true
                } catch (e: Exception) {
                    // Simplified error handling
                    snackbarHostState.showSnackbar(
                        message = "Failed to load data",
                        duration = SnackbarDuration.Short
                    )
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Function to load data (accessible from composables)
    suspend fun loadDataFunction() {
        if (isLoading) return // Prevent multiple simultaneous loads

        isLoading = true
        try {
            // Load data based on current tab to reduce memory usage
            when (selectedTab) {
                0 -> {
                    // Only load social media data when on social media tab
                    val installedApps = appBlockingManager?.getInstalledApps() ?: emptyList()
                    socialMediaApps = SocialMediaDetector.getInstalledSocialMediaApps(installedApps)
                    blockedApps = appBlockingManager?.getBlockedApps() ?: emptyList()
                    socialMediaStats = appBlockingManager?.getSocialMediaBlockingStats()
                }
                1 -> {
                    // Only load website data when on websites tab
                    customBlockedSites = siteBlockingManager?.getCustomBlockedWebsites() ?: emptyList()
                }
            }
            dataLoaded = true
        } catch (e: Exception) {
            // Simplified error handling
            snackbarHostState.showSnackbar(
                message = "Failed to load data",
                duration = SnackbarDuration.Short
            )
        } finally {
            isLoading = false
        }
    }

    // Load data when tab changes
    LaunchedEffect(selectedTab) {
        loadDataFunction()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Block Apps & Sites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selection
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📱 Social Media") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🌐 Websites") }
                )
            }

            // Search bar for social media tab
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> SocialMediaTabContent(
                    socialMediaApps = socialMediaApps.filter { app ->
                        if (searchQuery.isBlank()) true
                        else app.appName.contains(searchQuery, ignoreCase = true)
                    },
                    blockedApps = blockedApps,
                    socialMediaStats = socialMediaStats,
                    isLoading = isLoading,
                    appBlockingManager = appBlockingManager,
                    onRefresh = { coroutineScope.launch { loadDataFunction() } }
                )
                1 -> WebsitesTabContent(
                    customBlockedSites = customBlockedSites,
                    isLoading = isLoading,
                    siteBlockingManager = siteBlockingManager,
                    onRefresh = { coroutineScope.launch { loadDataFunction() } }
                )
            }
        }
    }
}

/**
 * Social Media Tab Content - Simplified and stable
 */
@Composable
private fun SocialMediaTabContent(
    socialMediaApps: List<AppInfo>,
    blockedApps: List<com.hieltech.haramblur.data.database.BlockedAppEntity>,
    socialMediaStats: AppBlockingManager.SocialMediaStats?,
    isLoading: Boolean,
    appBlockingManager: AppBlockingManager?,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 80.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stats header
        if (socialMediaStats != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${socialMediaStats.totalSocialMediaApps}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Total Apps", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${socialMediaStats.blockedSocialMediaApps}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text("Blocked", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Quick actions
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    appBlockingManager?.blockAllSocialMediaApps()
                                    onRefresh()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Block All")
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    appBlockingManager?.unblockAllSocialMediaApps()
                                    onRefresh()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unblock All")
                        }
                    }
                }
            }
        }

        // Apps list
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (socialMediaApps.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 No social media apps found!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Great job maintaining digital wellness",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(socialMediaApps) { appInfo ->
                val isBlocked = blockedApps.any { it.packageName == appInfo.packageName }
                val subcategory = SocialMediaDetector.getSocialMediaCategory(appInfo.packageName)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBlocked)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    SocialMediaAppCard(
                        appInfo = appInfo,
                        isBlocked = isBlocked,
                        subcategory = subcategory,
                        onBlockToggle = { shouldBlock ->
                            coroutineScope.launch {
                                if (shouldBlock) {
                                    appBlockingManager?.blockApp(appInfo.packageName)
                                } else {
                                    appBlockingManager?.unblockApp(appInfo.packageName)
                                }
                                onRefresh()
                            }
                        },
                        onTimeBasedBlock = { duration ->
                            coroutineScope.launch {
                                appBlockingManager?.blockAppForDuration(appInfo.packageName, duration)
                                onRefresh()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Websites Tab Content - Simplified and stable
 */
@Composable
private fun WebsitesTabContent(
    customBlockedSites: List<BlockedSiteEntity>,
    isLoading: Boolean,
    siteBlockingManager: EnhancedSiteBlockingManager?,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var newSiteUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 80.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🌐 Website Blocking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add custom websites to block",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Add website button
        item {
            OutlinedButton(
                onClick = { showAddSiteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Website to Block")
            }
        }

        // Sites list
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (customBlockedSites.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚫 No websites blocked yet",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Add websites above to start blocking them",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(customBlockedSites) { site ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (site.isActive)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    CustomBlockedSiteItem(
                        site = site,
                        onRemove = {
                            coroutineScope.launch {
                                siteBlockingManager?.removeCustomBlockedWebsite(site.pattern)
                                onRefresh()
                            }
                        }
                    )
                }
            }
        }
    }

    // Add website dialog
    if (showAddSiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddSiteDialog = false },
            title = { Text("Add Website to Block") },
            text = {
                Column {
                    Text("Enter the website URL you want to block:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSiteUrl,
                        onValueChange = { newSiteUrl = it },
                        label = { Text("Website URL") },
                        placeholder = { Text("e.g., example.com") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            siteBlockingManager?.addCustomBlockedWebsite(newSiteUrl)
                            newSiteUrl = ""
                            showAddSiteDialog = false
                            onRefresh()
                        }
                    },
                    enabled = newSiteUrl.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper components
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}