package com.hieltech.haramblur.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hieltech.haramblur.R
import com.hieltech.haramblur.accessibility.HaramBlurAccessibilityService
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToDebug) {
                        Icon(Icons.Default.Build, contentDescription = "Debug")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Enhanced Status Card with Real-time Indicators
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceRunning) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Service Status",
                        modifier = Modifier.size(48.dp),
                        tint = if (serviceRunning) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (serviceRunning) "🛡️ HaramBlur Active" else "❌ HaramBlur Inactive",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (serviceRunning) 
                            "Female content detection and filtering active" 
                        else "Enable accessibility service to start filtering",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    if (serviceRunning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Quick Status Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatusIndicator(
                                icon = "👤",
                                label = "Face Detection",
                                isActive = true
                            )
                            StatusIndicator(
                                icon = "🔞",
                                label = "Content Filter",
                                isActive = true
                            )
                            StatusIndicator(
                                icon = "⚡",
                                label = "GPU Accel",
                                isActive = true
                            )
                        }
                    }
                }
            }
            
            // Setup Instructions
            if (!serviceRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Setup Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "To use HaramBlur, you need to enable the accessibility service:\n\n" +
                                    "1. Tap 'Open Settings' below\n" +
                                    "2. Find 'HaramBlur' in the list\n" +
                                    "3. Toggle it ON\n" +
                                    "4. Confirm by tapping 'Allow'",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }
            }
            
            // App Information
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About HaramBlur",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "HaramBlur automatically detects and blurs inappropriate content " +
                                "across all apps on your device, helping you maintain Islamic values " +
                                "while using technology. All processing happens locally on your device " +
                                "for complete privacy.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Quick Actions Card
            if (serviceRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚡ Quick Actions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: Implement optimal settings */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🎯 Optimal")
                            }
                            
                            Button(
                                onClick = { /* TODO: Implement reset settings */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔄 Reset")
                            }
                        }
                    }
                }
            }
            
            // Privacy Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔒 Privacy & Focus",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• Focused on female content detection only\n" +
                                "• All processing happens locally on device\n" +
                                "• GPU acceleration for better performance\n" +
                                "• No data leaves your device",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    icon: String,
    label: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}