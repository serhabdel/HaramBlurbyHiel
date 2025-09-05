package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.settings.*
import com.hieltech.haramblur.data.SettingsCategory
import com.hieltech.haramblur.ui.components.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf<SettingsCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var settingsMode by remember { mutableStateOf(SettingsMode.SIMPLE) }

    when (currentScreen) {
        null -> {
            // Show navigation screen with modern enhancements
            AnimatedFadeIn(visible = true) {
                SettingsNavigationScreen(
                    onNavigateToCategory = { category ->
                        currentScreen = category
                    },
                    onNavigateBack = onNavigateBack,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    settingsMode = settingsMode,
                    onSettingsModeChange = { settingsMode = it }
                )
            }
        }
        SettingsCategory.ESSENTIAL -> {
            AnimatedSlideIn(visible = true) {
                GeneralSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
        SettingsCategory.DETECTION -> {
            AnimatedSlideIn(visible = true) {
                DetectionSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
        SettingsCategory.PERFORMANCE -> {
            AnimatedSlideIn(visible = true) {
                PerformanceSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
        SettingsCategory.ISLAMIC -> {
            AnimatedSlideIn(visible = true) {
                IslamicSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
        SettingsCategory.AI -> {
            AnimatedSlideIn(visible = true) {
                AdvancedSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
        SettingsCategory.DEVELOPER -> {
            AnimatedSlideIn(visible = true) {
                AdvancedSettingsScreen(
                    onNavigateBack = { currentScreen = null }
                )
            }
        }
    }
}