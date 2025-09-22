package com.hieltech.haramblur.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.newsettings.ModernAdvancedSettingsScreen
import com.hieltech.haramblur.ui.newsettings.ModernDetectionSettingsScreen
import com.hieltech.haramblur.ui.newsettings.ModernGeneralSettingsScreen
import com.hieltech.haramblur.ui.newsettings.ModernIslamicSettingsScreen
import com.hieltech.haramblur.ui.newsettings.ModernPerformanceSettingsScreen
import com.hieltech.haramblur.ui.newsettings.ModernSettingsHomeScreen
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
            AnimatedFadeIn(visible = true) {
                ModernSettingsHomeScreen(
                    onNavigateBack = onNavigateBack,
                    onNavigateToCategory = { category -> currentScreen = category },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    settingsMode = settingsMode,
                    onSettingsModeChange = { settingsMode = it }
                )
            }
        }
        SettingsCategory.ESSENTIAL -> {
            AnimatedSlideIn(visible = true) {
                ModernGeneralSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
        SettingsCategory.DETECTION -> {
            AnimatedSlideIn(visible = true) {
                ModernDetectionSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
        SettingsCategory.PERFORMANCE -> {
            AnimatedSlideIn(visible = true) {
                ModernPerformanceSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
        SettingsCategory.ISLAMIC -> {
            AnimatedSlideIn(visible = true) {
                ModernIslamicSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
        SettingsCategory.AI -> {
            AnimatedSlideIn(visible = true) {
                ModernAdvancedSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
        SettingsCategory.DEVELOPER -> {
            AnimatedSlideIn(visible = true) {
                ModernAdvancedSettingsScreen(onNavigateBack = { currentScreen = null })
            }
        }
    }
}
