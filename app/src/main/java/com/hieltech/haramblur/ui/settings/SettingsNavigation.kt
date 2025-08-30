package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Import the SettingsCategory from the data package
import com.hieltech.haramblur.data.SettingsCategory

// Define navigation categories that map to the data enum
enum class NavigationCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val settingsCategory: SettingsCategory
) {
    GENERAL(
        "General",
        "Service control and basic settings",
        Icons.Default.Settings,
        SettingsCategory.ESSENTIAL
    ),
    DETECTION(
        "Detection",
        "Face and content detection settings",
        Icons.Default.Search,
        SettingsCategory.DETECTION
    ),
    PERFORMANCE(
        "Performance",
        "Speed and resource optimization",
        Icons.Default.Build,
        SettingsCategory.PERFORMANCE
    ),
    ISLAMIC(
        "Islamic",
        "Quranic guidance and Dhikr",
        Icons.Default.Star,
        SettingsCategory.ISLAMIC
    ),
    ADVANCED(
        "Advanced",
        "AI and developer options",
        Icons.Default.Build,
        SettingsCategory.AI
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavigationScreen(
    onNavigateToCategory: (SettingsCategory) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(NavigationCategory.values().size) { index ->
                val navCategory = NavigationCategory.values()[index]
                SettingsCategoryCard(
                    category = navCategory,
                    onClick = { onNavigateToCategory(navCategory.settingsCategory) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryCard(
    category: NavigationCategory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}