package com.hieltech.haramblur.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.detection.Language as AppLanguage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hieltech.haramblur.ui.SettingsViewModel
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val appLanguage: AppLanguage
)

@Composable
fun LanguageSelectionStep(
    viewModel: PermissionWizardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onLanguageSelected: () -> Unit = {}
) {
    val languages = listOf(
        LanguageOption("en", "English", "English", "🇬🇧", AppLanguage.ENGLISH),
        LanguageOption("ar", "Arabic", "العربية", "🇸🇦", AppLanguage.ARABIC),
        LanguageOption("fr", "French", "Français", "🇫🇷", AppLanguage.FRENCH),
        LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷", AppLanguage.TURKISH),
        LanguageOption("ur", "Urdu", "اردو", "🇵🇰", AppLanguage.URDU),
        LanguageOption("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", AppLanguage.INDONESIAN),
        LanguageOption("ms", "Malay", "Bahasa Melayu", "🇲🇾", AppLanguage.MALAY),
        LanguageOption("bn", "Bengali", "বাংলা", "🇧🇩", AppLanguage.BENGALI),
        LanguageOption("fa", "Persian", "فارسی", "🇮🇷", AppLanguage.PERSIAN),
        LanguageOption("es", "Spanish", "Español", "🇪🇸", AppLanguage.SPANISH)
    )
    
    // Get current language from settings
    val settings by settingsViewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // State for selected language
    var selectedLanguage by remember { mutableStateOf(
        languages.find { it.code == settings.preferredLanguage.code } ?: languages[0]
    ) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌍 Choose Your Language",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Select your preferred language",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Error message display
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages) { language ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) { selectedLanguage = language },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedLanguage.code == language.code) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (selectedLanguage.code == language.code) {
                        CardDefaults.outlinedCardBorder().copy(
                            brush = CardDefaults.outlinedCardBorder().brush
                        )
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = language.flag,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Column {
                                Text(
                                    text = language.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = language.nativeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (selectedLanguage.code == language.code) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                // Save selected language to settings
                if (!isSaving) {
                    isSaving = true
                    errorMessage = null
                    
                    // Use coroutine to handle language update with proper error handling
                    coroutineScope.launch {
                        try {
                            Log.d("LanguageSelector", "🔄 Starting language persistence for ${selectedLanguage.appLanguage.name}")
                            
                            // Use the enhanced method with result and suppress recreation for wizard flow
                            val success = settingsViewModel.updatePreferredLanguageWithResult(
                                selectedLanguage.appLanguage,
                                suppressRecreation = true
                            )
                            
                            if (success) {
                                Log.d("LanguageSelector", "✅ Language persistence successful: ${selectedLanguage.appLanguage.name}")
                                // Mark language step as complete
                                viewModel.completeLanguageSelection()
                                onLanguageSelected()
                            } else {
                                // Handle failure case with retry
                                Log.w("LanguageSelector", "❌ First attempt failed for ${selectedLanguage.appLanguage.name}, retrying...")
                                kotlinx.coroutines.delay(500) // Brief delay before retry
                                
                                val retrySuccess = settingsViewModel.updatePreferredLanguageWithResult(
                                    selectedLanguage.appLanguage,
                                    suppressRecreation = true
                                )
                                
                                if (retrySuccess) {
                                    Log.d("LanguageSelector", "✅ Language persistence successful on retry: ${selectedLanguage.appLanguage.name}")
                                    viewModel.completeLanguageSelection()
                                    onLanguageSelected()
                                } else {
                                    Log.e("LanguageSelector", "❌ Language persistence failed after retry for ${selectedLanguage.appLanguage.name}")
                                    errorMessage = "Failed to save language after retry. Please try again."
                                    isSaving = false
                                }
                            }
                        } catch (e: Exception) {
                            // Handle exception case with detailed logging
                            Log.e("LanguageSelector", "❌ Exception during language save for ${selectedLanguage.appLanguage.name}", e)
                            errorMessage = "Error saving language: ${e.message}"
                            isSaving = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Text(
                    text = "Continue in ${selectedLanguage.name}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
