package com.hieltech.haramblur.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hieltech.haramblur.data.UserGender
import com.hieltech.haramblur.ui.PermissionWizardViewModel
import com.hieltech.haramblur.ui.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Gender selection step for Islamic-compliant content filtering
 */
@Composable
fun GenderSelectionStep(
    viewModel: PermissionWizardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onGenderSelected: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize with persisted value from settings
    val currentSettings by settingsViewModel.settings.collectAsState()
    val initialGender = remember(currentSettings.userGender) {
        if (currentSettings.userGender != UserGender.NOT_SPECIFIED) {
            currentSettings.userGender
        } else {
            null
        }
    }
    var selectedGender by remember { mutableStateOf<UserGender?>(initialGender) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "👤 Gender Selection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This helps us provide Islamic-compliant content filtering. For example, it may be appropriate for women to see other women's images.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Male option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedGender == UserGender.MALE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == UserGender.MALE,
                        onClick = { selectedGender = UserGender.MALE }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🧔 Male",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Will blur female faces and NSFW content",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Female option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedGender == UserGender.FEMALE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == UserGender.FEMALE,
                        onClick = { selectedGender = UserGender.FEMALE }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "👩 Female",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Will blur male faces and NSFW content",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                selectedGender?.let { gender ->
                    isSaving = true
                    saveError = null // Clear any previous errors
                    coroutineScope.launch {
                        try {
                            Log.d("GenderSelectionStep", "🔄 Starting gender persistence for $gender")

                            // Save gender preference and apply appropriate blur settings
                            // Wait for save to complete before proceeding
                            val success = settingsViewModel.updateGenderSettings(gender)

                            if (success) {
                                Log.d("GenderSelectionStep", "✅ Gender persistence successful: $gender")
                                // Gender successfully persisted, proceed to next step
                                viewModel.completeGenderSelection()
                                onGenderSelected()
                            } else {
                                // Persistence failed, show error and allow retry
                                Log.w("GenderSelectionStep", "❌ First attempt failed for $gender, retrying...")
                                kotlinx.coroutines.delay(500) // Brief delay before retry

                                val retrySuccess = settingsViewModel.updateGenderSettings(gender)

                                if (retrySuccess) {
                                    Log.d("GenderSelectionStep", "✅ Gender persistence successful on retry: $gender")
                                    viewModel.completeGenderSelection()
                                    onGenderSelected()
                                } else {
                                    Log.e("GenderSelectionStep", "❌ Gender persistence failed after retry for $gender")
                                    saveError = "Failed to save gender preference after retry. Please try again."
                                    android.util.Log.e("GenderSelectionStep", "Gender persistence failed for $gender")
                                }
                            }
                        } catch (e: Exception) {
                            // Unexpected error, show error and allow retry
                            Log.e("GenderSelectionStep", "❌ Exception during gender save for $gender", e)
                            saveError = "An error occurred while saving. Please try again."
                            android.util.Log.e("GenderSelectionStep", "Exception during gender save", e)
                        } finally {
                            isSaving = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedGender != null && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Continue")
        }

        // Error message display
        saveError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(
                        onClick = { saveError = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
