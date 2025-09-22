package com.hieltech.haramblur.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Import the SettingsCategory from the data package
import com.hieltech.haramblur.data.SettingsCategory
import com.hieltech.haramblur.R
import com.hieltech.haramblur.ui.components.*

// Define navigation categories that map to the data enum
enum class NavigationCategory(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val settingsCategory: SettingsCategory
) {
    GENERAL(
        R.string.nav_general_title,
        R.string.nav_general_description,
        Icons.Default.Settings,
        SettingsCategory.ESSENTIAL
    ),
    DETECTION(
        R.string.nav_detection_title,
        R.string.nav_detection_description,
        Icons.Default.Search,
        SettingsCategory.DETECTION
    ),
    PERFORMANCE(
        R.string.nav_performance_title,
        R.string.nav_performance_description,
        Icons.Default.Build,
        SettingsCategory.PERFORMANCE
    ),
    ISLAMIC(
        R.string.nav_islamic_title,
        R.string.nav_islamic_description,
        Icons.Default.Star,
        SettingsCategory.ISLAMIC
    ),
    ADVANCED(
        R.string.nav_advanced_title,
        R.string.nav_advanced_description,
        Icons.Default.Build,
        SettingsCategory.AI
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavigationScreen(
    onNavigateToCategory: (SettingsCategory) -> Unit,
    onNavigateBack: () -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    settingsMode: SettingsMode = SettingsMode.SIMPLE,
    onSettingsModeChange: (SettingsMode) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(responsiveLayoutMargins())
        ) {
            // Modern header section with app branding
            ModernCard(
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                elevation = responsiveCardElevation(),
                cornerRadius = responsiveCornerRadius(),
                contentPadding = responsiveCardPadding()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
                ) {
                    Text(
                        text = "🛡️ HaramBlur Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Configure your Islamic content filtering experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Settings search functionality
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Settings mode toggle
            SettingsModeToggle(
                currentMode = settingsMode,
                onModeChange = onSettingsModeChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Quick toggles section for most commonly used settings
            ModernCard(
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                elevation = responsiveCardElevation(),
                cornerRadius = responsiveCornerRadius(),
                contentPadding = responsiveCardPadding()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
                ) {
                    Text(
                        text = "Quick Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Note: These would need to be connected to actual settings state
                    QuickToggleRow(
                        toggles = listOf(
                            "Protection" to (true to {}),
                            "Prayer Times" to (false to {}),
                            "Dhikr" to (true to {})
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Enhanced navigation grid with modern cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(responsiveGridColumns()),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(responsiveGridSpacing()),
                verticalArrangement = Arrangement.spacedBy(responsiveGridSpacing()),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NavigationCategory.values().size) { index ->
                    val navCategory = NavigationCategory.values()[index]
                    AnimatedListItem(
                        visible = true,
                        index = index
                    ) {
                        ModernSettingsCategoryCard(
                            category = navCategory,
                            onClick = { onNavigateToCategory(navCategory.settingsCategory) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSettingsCategoryCard(
    category: NavigationCategory,
    onClick: () -> Unit
) {
    // Category-specific gradient colors
    val gradientColors = when (category.settingsCategory) {
        SettingsCategory.ISLAMIC -> listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
        SettingsCategory.DETECTION -> listOf(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
        SettingsCategory.PERFORMANCE -> listOf(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
        SettingsCategory.AI -> listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
        else -> listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    }

    ModernCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        gradientColors = gradientColors,
        elevation = responsiveCardElevation(),
        cornerRadius = responsiveCornerRadius(),
        contentPadding = responsiveCardPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = stringResource(category.titleResId),
                modifier = Modifier.size(responsiveIconSize() * 1.5f),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            Text(
                text = stringResource(category.titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(category.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2
            )
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
                contentDescription = stringResource(category.titleResId),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(category.titleResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(category.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
