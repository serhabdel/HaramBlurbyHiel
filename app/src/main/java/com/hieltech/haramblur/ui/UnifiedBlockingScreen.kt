package com.hieltech.haramblur.ui

import androidx.compose.animation.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedBlockingScreen(
    onNavigateBack: () -> Unit = {},
    appBlockingManager: AppBlockingManager? = null,
    siteBlockingManager: EnhancedSiteBlockingManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State management
    var selectedTab by remember { mutableIntStateOf(0) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var socialMediaApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<com.hieltech.haramblur.data.database.BlockedAppEntity>>(emptyList()) }
    var customBlockedSites by remember { mutableStateOf<List<BlockedSiteEntity>>(emptyList()) }
    var socialMediaStats by remember { mutableStateOf<AppBlockingManager.SocialMediaStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Load data function
    val loadData = remember {
        suspend {
            isLoading = true
            try {
                // Load all installed apps
                installedApps = appBlockingManager?.getInstalledApps() ?: emptyList()

                // Filter social media apps
                socialMediaApps = SocialMediaDetector.getInstalledSocialMediaApps(installedApps)

                // Load blocked apps
                blockedApps = appBlockingManager?.getBlockedApps() ?: emptyList()

                // Load custom blocked sites
                customBlockedSites = siteBlockingManager?.getCustomBlockedWebsites() ?: emptyList()

                // Load social media stats
                socialMediaStats = appBlockingManager?.getSocialMediaBlockingStats()

            } catch (e: Exception) {
                // Show error snackbar with retry action
                val result = snackbarHostState.showSnackbar(
                    message = "Failed to load data: ${e.message}",
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                // Handle retry action
                if (result == SnackbarResult.ActionPerformed) {
                    // Retry by reloading
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Load data when screen opens
    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
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
                    text = { Text("📱 Social Media") },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🌐 Websites") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }

            // Search bar for social media tab
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search social media apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> SocialMediaAppsTab(
                    socialMediaApps = socialMediaApps.filter { app ->
                        if (searchQuery.isBlank()) true
                        else app.appName.contains(searchQuery, ignoreCase = true) ||
                             app.packageName.contains(searchQuery, ignoreCase = true)
                    },
                    blockedApps = blockedApps,
                    socialMediaStats = socialMediaStats,
                    isLoading = isLoading,
                    appBlockingManager = appBlockingManager,
                    onRefresh = { coroutineScope.launch { loadData() } }
                )
                1 -> BlockSitesTab(
                    customBlockedSites = customBlockedSites,
                    isLoading = isLoading,
                    siteBlockingManager = siteBlockingManager,
                    onRefresh = { coroutineScope.launch { loadData() } }
                )
            }
        }
    }
}

@Composable
fun SocialMediaAppsTab(
    socialMediaApps: List<AppInfo>,
    blockedApps: List<com.hieltech.haramblur.data.database.BlockedAppEntity>,
    socialMediaStats: AppBlockingManager.SocialMediaStats?,
    isLoading: Boolean,
    appBlockingManager: AppBlockingManager?,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<SocialMediaDetector.SocialMediaSubcategory?>(null) }
    var suggestedApps by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load suggested apps
    LaunchedEffect(Unit) {
        suggestedApps = appBlockingManager?.suggestSocialMediaAppsToBlock() ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 80.dp,
            start = 12.dp,
            end = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "📱 Social Media Blocking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Protect your focus by managing social media access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Stats display
                    socialMediaStats?.let { stats ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatItem("Total Apps", stats.totalSocialMediaApps.toString())
                            StatItem("Blocked", stats.blockedSocialMediaApps.toString())
                            StatItem("Most Used", stats.mostUsedCategory.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }

        // Bulk actions
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚡ Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val count = appBlockingManager?.blockAllSocialMediaApps() ?: 0
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
                                    val count = appBlockingManager?.unblockAllSocialMediaApps() ?: 0
                                    onRefresh()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unblock All")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    suggestedApps.forEach { packageName ->
                                        appBlockingManager?.blockApp(packageName)
                                    }
                                    suggestedApps = emptyList()
                                    onRefresh()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = suggestedApps.isNotEmpty()
                        ) {
                            Text("Block Suggested (${suggestedApps.size})")
                        }
                    }
                }
            }
        }

        // Category filter
        item {
            Text(
                text = "📂 Filter by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
                SocialMediaDetector.getAllSubcategories().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) }
                    )
                }
            }
        }

        // Apps list
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            val filteredApps = if (selectedCategory != null) {
                socialMediaApps.filter { app ->
                    SocialMediaDetector.getSocialMediaCategory(app.packageName) == selectedCategory
                }
            } else {
                socialMediaApps
            }

            if (filteredApps.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
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
                items(filteredApps) { appInfo ->
                    val isBlocked = blockedApps.any { it.packageName == appInfo.packageName }
                    val subcategory = SocialMediaDetector.getSocialMediaCategory(appInfo.packageName)

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

        // Suggested apps section
        if (suggestedApps.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Text(
                    text = "💡 Suggested to Block",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Create a map of installed apps for efficient lookup
            val installedAppsMap = socialMediaApps.associateBy { it.packageName }

            items(suggestedApps) { packageName ->
                val appInfo = installedAppsMap[packageName] ?: run {
                    // Fallback for apps not in installed list
                    AppInfo(
                        packageName = packageName,
                        appName = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                        category = "Unknown",
                        isSystemApp = false,
                        icon = null
                    )
                }

                SuggestedAppItem(
                    appInfo = appInfo,
                    onBlock = {
                        coroutineScope.launch {
                            appBlockingManager?.blockApp(packageName)
                            suggestedApps = suggestedApps.filter { it != packageName }
                            onRefresh()
                        }
                    },
                    onDismiss = {
                        suggestedApps = suggestedApps.filter { it != packageName }
                    }
                )
            }
        }
    }
}

@Composable
fun BlockSitesTab(
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
            start = 12.dp,
            end = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "🌐 Website Blocking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Add custom websites to block or manage existing blocked sites",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

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

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(customBlockedSites) { site ->
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
                        placeholder = { Text("e.g., example.com or https://example.com") }
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

@Composable
fun SuggestedAppItem(
    appInfo: AppInfo,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = appInfo.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBlock,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Block",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block")
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss suggestion"
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBlockedSiteItem(
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
                    contentDescription = "Remove blocked site"
                )
            }
        }
    }
}

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
