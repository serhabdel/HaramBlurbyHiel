package com.hieltech.haramblur.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.LogRepository
import com.hieltech.haramblur.data.database.LogEntity
import com.hieltech.haramblur.data.database.LogStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LogsViewerViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedLevels = MutableStateFlow(setOf("DEBUG", "INFO", "WARN", "ERROR"))
    val selectedLevels: StateFlow<Set<String>> = _selectedLevels

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _logs = MutableStateFlow<List<LogEntity>>(emptyList())
    val logs: StateFlow<List<LogEntity>> = _logs

    val logStatistics: StateFlow<LogStatistics?> = logRepository.getLogStatistics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    init {
        viewModelScope.launch {
            try {
                refreshLogs()
            } catch (e: Exception) {
                logRepository.logError("LogsViewerViewModel", "Failed to initialize logs", e)
                _logs.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun toggleLevel(level: String) {
        _selectedLevels.value = if (_selectedLevels.value.contains(level)) {
            _selectedLevels.value - level
        } else {
            _selectedLevels.value + level
        }
        refreshLogs()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        refreshLogs()
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val query = _searchQuery.value
                val levels = _selectedLevels.value.toList()

                if (query.isNotEmpty()) {
                    // Search mode
                    logRepository.searchLogs(query).take(1).collect { searchResults ->
                        _logs.value = searchResults.filter { log ->
                            levels.contains(log.level)
                        }
                    }
                } else if (levels.isNotEmpty()) {
                    // Filter by levels
                    logRepository.getLogsWithLevels(levels, limit = 500).take(1).collect { filteredLogs ->
                        _logs.value = filteredLogs
                    }
                } else {
                    // Get recent logs
                    logRepository.getRecentLogs(500).take(1).collect { recentLogs ->
                        _logs.value = recentLogs
                    }
                }
            } catch (e: Exception) {
                logRepository.logError("LogsViewerViewModel", "Failed to refresh logs", e)
                _logs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun exportLogs(context: Context) {
        logRepository.logInfo("LogsViewerViewModel", "Starting log export")

        try {
            val levels = _selectedLevels.value.toList()
            val exportFile = logRepository.exportLogsToFile(levels = levels)

            if (exportFile != null) {
                shareFile(context, exportFile, "text/plain")
                logRepository.logInfo("LogsViewerViewModel", "Log export completed successfully")
            } else {
                logRepository.logError("LogsViewerViewModel", "Log export failed - file is null")
            }
        } catch (e: Exception) {
            logRepository.logError("LogsViewerViewModel", "Log export failed", e)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "HaramBlur Application Logs")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the HaramBlur application logs for troubleshooting.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(intent, "Share Logs")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)

            viewModelScope.launch {
                logRepository.logInfo("LogsViewerViewModel", "Share intent launched for log file")
            }

        } catch (e: Exception) {
            viewModelScope.launch {
                logRepository.logError("LogsViewerViewModel", "Failed to share log file", e)
            }
        }
    }

    suspend fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.logInfo("LogsViewerViewModel", "Clearing all logs")
        }
        logRepository.clearAllLogs()
        refreshLogs()
    }

    fun getLogsAsText(): Flow<String> {
        return flow {
            val levels = _selectedLevels.value.toList()
            val text = logRepository.exportLogsAsText(levels = levels)
            emit(text)
        }
    }
}
