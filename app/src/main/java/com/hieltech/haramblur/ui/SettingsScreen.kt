package com.hieltech.haramblur.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.settings.*
import com.hieltech.haramblur.data.SettingsCategory

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf<SettingsCategory?>(null) }

    when (currentScreen) {
        null -> {
            // Show navigation screen
            SettingsNavigationScreen(
                onNavigateToCategory = { category ->
                    currentScreen = category
                },
                onNavigateBack = onNavigateBack
            )
        }
        SettingsCategory.ESSENTIAL -> {
            GeneralSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
        SettingsCategory.DETECTION -> {
            DetectionSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
        SettingsCategory.PERFORMANCE -> {
            PerformanceSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
        SettingsCategory.ISLAMIC -> {
            IslamicSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
        SettingsCategory.AI -> {
            AdvancedSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
        SettingsCategory.DEVELOPER -> {
            AdvancedSettingsScreen(
                onNavigateBack = { currentScreen = null }
            )
        }
    }
}