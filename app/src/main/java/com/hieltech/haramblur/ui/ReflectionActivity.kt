package com.hieltech.haramblur.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.QuranicVerse
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.ui.components.QuranicVerseDialog
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.AppBlockingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lightweight activity shown from notification to present a reflection dialog.
 */
@AndroidEntryPoint
class ReflectionActivity : ComponentActivity() {

    @Inject lateinit var quranicRepository: QuranicRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appBlockingManager: AppBlockingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra("package_name")
        val appName = intent.getStringExtra("app_name") ?: packageName ?: "App"
        val verseId = intent.getStringExtra("verse_id")
        val forceClose = intent.getBooleanExtra("force_close", false)

        lifecycleScope.launch {
            setContent {
                val settings by settingsRepository.settings.collectAsState(initial = settingsRepository.getCurrentSettings())

                // Fetch verse by ID if provided
                var verse by remember { mutableStateOf<QuranicVerse?>(null) }
                val guidance = remember { null }

                // Load verse when verseId is available
                LaunchedEffect(verseId) {
                    if (!verseId.isNullOrEmpty()) {
                        try {
                            verse = quranicRepository.getVerseById(verseId)
                        } catch (e: Exception) {
                            Log.e("ReflectionActivity", "Error loading verse: $verseId", e)
                            // Fallback to a default verse for the category
                            verse = quranicRepository.getVerseForCategory(BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE)
                        }
                    } else {
                        // No verse ID provided, get a default verse for social media
                        verse = quranicRepository.getVerseForCategory(BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE)
                    }
                }

                HaramBlurTheme(appTheme = settings.appTheme, preferredLanguage = settings.preferredLanguage) {
                    if (forceClose && !packageName.isNullOrEmpty()) {
                        // Close immediately without UI when launched for quick close
                        LaunchedEffect(Unit) {
                            appBlockingManager.enforceBlock(packageName)
                            finish()
                        }
                    }

                    QuranicVerseDialog(
                        verse = verse, // could fetch by verseId if wired
                        guidance = null,
                        category = BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE,
                        reflectionTimeSeconds = settings.mandatoryReflectionTime,
                        onAction = { action ->
                            when (action) {
                                is WarningDialogAction.Close -> {
                                    if (!packageName.isNullOrEmpty()) {
                                        lifecycleScope.launch { appBlockingManager.enforceBlock(packageName) }
                                    }
                                    finish()
                                }
                                is WarningDialogAction.Continue -> {
                                    finish()
                                }
                                else -> {}
                            }
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}
