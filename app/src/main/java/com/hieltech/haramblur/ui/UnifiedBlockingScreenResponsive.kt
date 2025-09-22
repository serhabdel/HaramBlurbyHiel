package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.R
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header section with title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.block_apps_sites),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { coroutineScope.launch { loadDataFunction() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
            
            // Tab selection
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.social_media)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.websites)) }
                )
            }

            // Search bar for social media tab
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_apps)) },
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
                            text = stringResource(R.string.overview),
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
                                Text(stringResource(R.string.total_apps), style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${socialMediaStats.blockedSocialMediaApps}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(stringResource(R.string.blocked), style = MaterialTheme.typography.bodySmall)
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
                        text = stringResource(R.string.quick_actions_title),
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
                            Text(stringResource(R.string.block_all))
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
                            Text(stringResource(R.string.unblock_all))
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
                            text = stringResource(R.string.no_social_media_found),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.great_job),
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
                        text = stringResource(R.string.website_blocking),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.add_custom_websites),
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
                Text(stringResource(R.string.add_website_to_block))
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
                            text = stringResource(R.string.no_websites_blocked),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.add_websites_above),
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
            title = { Text(stringResource(R.string.add_website_to_block)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_website_url))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSiteUrl,
                        onValueChange = { newSiteUrl = it },
                        label = { Text(stringResource(R.string.website_url)) },
                        placeholder = { Text(stringResource(R.string.example_url)) }
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
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text(stringResource(R.string.cancel))
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

@Composable
private fun CustomBlockedSiteItem(
    site: BlockedSiteEntity,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (site.isActive)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.pattern,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = site.category.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (site.isActive) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (site.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (site.isCustom) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (site.description != null) {
                    Text(
                        text = site.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_blocked_site)
                    )
            }
        }
    }
}
