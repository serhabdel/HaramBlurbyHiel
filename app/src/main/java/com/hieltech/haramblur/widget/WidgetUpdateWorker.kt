package com.hieltech.haramblur.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically updates widgets with fresh data
 * This ensures prayer countdown and other time-sensitive data stays current
 */
class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "WidgetUpdateWorker running...")
                
                val manager = GlanceAppWidgetManager(context)
                
                // Update Islamic Dashboard widgets with timestamp to force refresh
                val dashboardWidget = IslamicDashboardGlanceWidget()
                val dashboardIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
                Log.d(TAG, "Updating ${dashboardIds.size} Dashboard widgets")
                dashboardIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[longPreferencesKey("update_time")] = System.currentTimeMillis()
                        }
                    }
                    dashboardWidget.update(context, glanceId)
                }
                
                // Update Prayer Times widgets
                val prayerWidget = PrayerTimesOnlyGlanceWidget()
                val prayerIds = manager.getGlanceIds(PrayerTimesOnlyGlanceWidget::class.java)
                Log.d(TAG, "Updating ${prayerIds.size} Prayer widgets")
                prayerIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[longPreferencesKey("update_time")] = System.currentTimeMillis()
                        }
                    }
                    prayerWidget.update(context, glanceId)
                }
                
                Log.d(TAG, "Widget update complete")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Widget update failed", e)
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WORK_NAME = "widget_periodic_update"
        
        /**
         * Schedule periodic widget updates every 1 minute
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES  // Minimum is 15 minutes for PeriodicWork
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Widget update worker scheduled")
        }
        
        /**
         * Cancel periodic updates
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Widget update worker cancelled")
        }
        
        /**
         * Trigger an immediate one-time update
         */
        fun triggerImmediateUpdate(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Immediate widget update triggered")
        }
    }
}
