package com.hieltech.haramblur.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    var selectedLanguage by remember { mutableStateOf(
        languages.find { it.appLanguage == settings.preferredLanguage } ?: languages.first()
    ) }
    
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
                        .clickable { selectedLanguage = language },
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
                settingsViewModel.updateLanguage(selectedLanguage.appLanguage)
                // Mark language step as complete
                viewModel.completeLanguageSelection()
                onLanguageSelected()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = true
        ) {
            Text(
                text = "Continue in ${selectedLanguage.name}",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
