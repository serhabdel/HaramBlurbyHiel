package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.AppInfo
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.data.database.BlockedSiteEntity
import com.hieltech.haramblur.data.AppRegistry
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockAppsAndSitesScreen(
    onNavigateBack: () -> Unit = {},
    appBlockingManager: AppBlockingManager? = null,
    siteBlockingManager: EnhancedSiteBlockingManager? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // State management
    var selectedTab by remember { mutableIntStateOf(0) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<com.hieltech.haramblur.data.database.BlockedAppEntity>>(emptyList()) }
    var customBlockedSites by remember { mutableStateOf<List<BlockedSiteEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load data function
    val loadData = remember {
        suspend {
            isLoading = true
            try {
                // Load installed apps
                installedApps = appBlockingManager?.getInstalledApps() ?: emptyList()

                // Load blocked apps
                blockedApps = appBlockingManager?.getBlockedApps() ?: emptyList()

                // Load custom blocked sites
                customBlockedSites = siteBlockingManager?.getCustomBlockedWebsites() ?: emptyList()
            } catch (e: Exception) {
                // Handle error
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
        topBar = {
            TopAppBar(
                title = { Text("Block Apps & Sites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                    text = { Text("Block Apps") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Block Sites") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> BlockAppsTab(
                    installedApps = installedApps,
                    blockedApps = blockedApps,
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
fun BlockAppsTab(
    installedApps: List<AppInfo>,
    blockedApps: List<com.hieltech.haramblur.data.database.BlockedAppEntity>,
    isLoading: Boolean,
    appBlockingManager: AppBlockingManager?,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showPopularApps by remember { mutableStateOf(false) }
    var popularApps by remember { mutableStateOf<List<com.hieltech.haramblur.data.database.BlockedAppEntity>>(emptyList()) }
    var suggestedApps by remember { mutableStateOf<List<String>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load popular apps when showing popular apps
    LaunchedEffect(showPopularApps) {
        if (showPopularApps) {
            popularApps = appBlockingManager?.getPopularApps() ?: emptyList()
            suggestedApps = appBlockingManager?.getSuggestedAppsToBlock() ?: emptyList()
            categories = appBlockingManager?.let {
                val allApps = it.getPopularApps()
                allApps.mapNotNull { app -> app.category }.distinct()
            } ?: emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 80.dp, // Add extra padding for navigation bar
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "📱 App Blocking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Block distracting apps to maintain focus and Islamic values",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Popular Apps Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showPopularApps = !showPopularApps
                        if (!showPopularApps) {
                            coroutineScope.launch {
                                onRefresh()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showPopularApps) "Show All Apps" else "Popular Apps")
                }

                if (!showPopularApps) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val count = appBlockingManager?.initializePopularApps() ?: 0
                                android.util.Log.d("BlockAppsTab", "Initialized $count popular apps")
                                onRefresh()
                            }
                        }
                    ) {
                        Text("🔄 Load Popular")
                    }
                }
            }
        }

        if (showPopularApps) {
            // Popular Apps Content
            if (popularApps.isNotEmpty()) {
                item {
                    Text(
                        text = "⭐ Popular Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Show apps by category
                categories.forEach { category ->
                    val categoryApps = popularApps.filter { it.category == category }
                    if (categoryApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "📂 ${category.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(categoryApps) { app ->
                            PopularAppBlockingItem(
                                app = app,
                                onBlockToggle = { shouldBlock ->
                                    coroutineScope.launch {
                                        if (shouldBlock) {
                                            appBlockingManager?.blockApp(app.packageName)
                                        } else {
                                            appBlockingManager?.unblockApp(app.packageName)
                                        }
                                        popularApps = popularApps.map {
                                            if (it.packageName == app.packageName) {
                                                it.copy(isBlocked = shouldBlock)
                                            } else it
                                        }
                                    }
                                },
                                onTimeBasedBlock = { duration ->
                                    coroutineScope.launch {
                                        appBlockingManager?.blockAppForDuration(app.packageName, duration)
                                        onRefresh()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "No popular apps found. Tap 'Load Popular' to initialize.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Suggested Apps to Block
            if (suggestedApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "💡 Suggested to Block",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Block All Suggested")
                        }

                        OutlinedButton(
                            onClick = {
                                suggestedApps = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }

                items(suggestedApps) { packageName ->
                    val appInfo = AppRegistry.getAppInfo(packageName)
                    if (appInfo != null) {
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
        } else {
            // All Apps Section
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
                items(installedApps) { appInfo ->
                    val isBlocked = blockedApps.any { it.packageName == appInfo.packageName }

                    AppBlockingItem(
                        appInfo = appInfo,
                        isBlocked = isBlocked,
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
            top = 16.dp,
            bottom = 80.dp, // Add extra padding for navigation bar
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "🌐 Website Blocking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Add custom websites to block or manage existing blocked sites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockingItem(
    appInfo: AppInfo,
    isBlocked: Boolean,
    onBlockToggle: (Boolean) -> Unit,
    onTimeBasedBlock: (Int) -> Unit
) {
    var showTimeOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Text(
                text = "📱",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Category: ${appInfo.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Block toggle
            Switch(
                checked = isBlocked,
                onCheckedChange = onBlockToggle
            )
        }

        // Time-based blocking options
        if (isBlocked) {
            AnimatedVisibility(visible = showTimeOptions) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Time-based blocking:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(15) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("15 min")
                        }
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(120) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2 hours")
                        }
                        OutlinedButton(
                            onClick = { showTimeOptions = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Custom")
                        }
                    }
                }
            }

            TextButton(
                onClick = { showTimeOptions = !showTimeOptions },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Time-based options")
                Icon(
                    if (showTimeOptions) Icons.Default.ArrowBack else Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBlockedSiteItem(
    site: BlockedSiteEntity,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚫",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = site.pattern,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Category: ${site.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (site.description != null) {
                    Text(
                        text = site.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularAppBlockingItem(
    app: com.hieltech.haramblur.data.database.BlockedAppEntity,
    onBlockToggle: (Boolean) -> Unit,
    onTimeBasedBlock: (Int) -> Unit
) {
    var showTimeOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Text(
                text = getCategoryEmoji(app.category ?: "other"),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${app.category?.replaceFirstChar { it.uppercase() } ?: "Other"} • ${if (app.isBlocked) "BLOCKED" else "ALLOWED"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (app.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Block toggle
            Switch(
                checked = app.isBlocked,
                onCheckedChange = onBlockToggle
            )
        }

        // Time-based blocking options
        if (app.isBlocked) {
            AnimatedVisibility(visible = showTimeOptions) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Time-based blocking:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(15) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("15 min")
                        }
                        OutlinedButton(
                            onClick = { onTimeBasedBlock(120) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2 hours")
                        }
                        OutlinedButton(
                            onClick = { showTimeOptions = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Custom")
                        }
                    }
                }
            }

            TextButton(
                onClick = { showTimeOptions = !showTimeOptions },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Time options")
                Icon(
                    if (showTimeOptions) Icons.Filled.ArrowBack else Icons.Filled.Add,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun SuggestedAppItem(
    appInfo: com.hieltech.haramblur.data.AppInfo,
    onBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getCategoryEmoji(appInfo.category),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "💡 Suggested for blocking",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBlock) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Block",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "social_media" -> "📱"
        "messaging" -> "💬"
        "dating" -> "💕"
        "entertainment" -> "🎬"
        "shopping" -> "🛒"
        "news" -> "📰"
        else -> "📱"
    }
}
