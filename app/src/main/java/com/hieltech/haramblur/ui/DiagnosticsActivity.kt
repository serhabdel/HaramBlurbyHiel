package com.hieltech.haramblur.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.detection.AppBlockingManager
import com.hieltech.haramblur.detection.EnhancedSiteBlockingManager
import com.hieltech.haramblur.ui.theme.HaramBlurTheme
import com.hieltech.haramblur.utils.DiagnosticsHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DiagnosticsActivity : ComponentActivity() {
    
    private val viewModel: DiagnosticsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            HaramBlurTheme {
                DiagnosticsScreen(viewModel = viewModel)
            }
        }
    }
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val siteBlockingManager: EnhancedSiteBlockingManager,
    private val appBlockingManager: AppBlockingManager
) : ViewModel() {
    
    private val _diagnosticsState = mutableStateOf<DiagnosticsState>(DiagnosticsState.Loading)
    val diagnosticsState: State<DiagnosticsState> = _diagnosticsState
    
    fun runDiagnostics(context: android.content.Context) {
        viewModelScope.launch {
            _diagnosticsState.value = DiagnosticsState.Loading
            
            try {
                val report = DiagnosticsHelper.runComprehensiveDiagnostics(
                    context = context,
                    settingsRepository = settingsRepository,
                    siteBlockingManager = siteBlockingManager,
                    appBlockingManager = appBlockingManager
                )
                
                val summary = DiagnosticsHelper.generateReportSummary(report)
                _diagnosticsState.value = DiagnosticsState.Success(report, summary)
            } catch (e: Exception) {
                _diagnosticsState.value = DiagnosticsState.Error("Failed to run diagnostics: ${e.message}")
            }
        }
    }
}

sealed class DiagnosticsState {
    object Loading : DiagnosticsState()
    data class Success(
        val report: com.hieltech.haramblur.utils.DiagnosticReport,
        val summary: String
    ) : DiagnosticsState()
    data class Error(val message: String) : DiagnosticsState()
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    DiagnosticsScreenContent(viewModel)
}

@Composable
fun DiagnosticsScreenContent(viewModel: DiagnosticsViewModel) {
    val context = LocalContext.current
    val diagnosticsState by viewModel.diagnosticsState
    
    LaunchedEffect(Unit) {
        viewModel.runDiagnostics(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "HaramBlur Diagnostics",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when (val state = diagnosticsState) {
            is DiagnosticsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Running diagnostics...")
                    }
                }
            }
            
            is DiagnosticsState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.runDiagnostics(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Diagnostics")
                }
            }
            
            is DiagnosticsState.Success -> {
                DiagnosticsResults(
                    report = state.report,
                    summary = state.summary,
                    onRetry = { viewModel.runDiagnostics(context) }
                )
            }
        }
    }
}

@Composable
fun DiagnosticsResults(
    report: com.hieltech.haramblur.utils.DiagnosticReport,
    summary: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Overall Health Status
        val totalIssues = report.getTotalIssues()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (totalIssues == 0) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (totalIssues == 0) "✅ System Healthy" else "⚠️ Issues Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (totalIssues == 0) 
                        "All systems are working correctly" 
                    else 
                        "$totalIssues issue(s) detected",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        if (totalIssues > 0) {
            QuickActionsCard(report = report)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Detailed Report
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Detailed Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedButton(
                onClick = {
                    shareReport(context, summary)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Share Report")
            }
        }
    }
}

@Composable
fun QuickActionsCard(report: com.hieltech.haramblur.utils.DiagnosticReport) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Accessibility Service Issues
            if (!report.accessibilityStatus.isEnabled) {
                QuickActionItem(
                    title = "Enable Accessibility Service",
                    description = "HaramBlur needs accessibility service to function",
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
            
            // Overlay Permission Issues
            if (!report.overlayPermissionStatus.isGranted) {
                QuickActionItem(
                    title = "Grant Overlay Permission",
                    description = "Required to display blur overlays",
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                )
            }
            
            // Settings Issues
            if (!report.settingsStatus.persistenceWorks) {
                QuickActionItem(
                    title = "Check App Storage",
                    description = "Settings are not saving properly",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                )
            }
            
            // Performance Issues
            if (report.performanceStatus.memoryUsagePercent > 80) {
                QuickActionItem(
                    title = "Clear App Cache",
                    description = "High memory usage detected",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                        Toast.makeText(context, "Use 'Storage & cache' to clear cache", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareReport(context: android.content.Context, summary: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, summary)
        putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Diagnostics Report")
    }
    
    val chooser = Intent.createChooser(shareIntent, "Share Diagnostics Report")
    context.startActivity(chooser)
}