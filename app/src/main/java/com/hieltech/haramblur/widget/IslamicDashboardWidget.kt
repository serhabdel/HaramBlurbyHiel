package com.hieltech.haramblur.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Islamic Dashboard Widget - Main comprehensive widget
 * Shows next prayer, hijri date, tasbih counter, and prayer times
 */
class IslamicDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IslamicDashboardGlanceWidget()
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_INCREMENT_TASBIH -> {
                WidgetDataProvider.incrementTasbih(context)
                updateWidgets(context)
            }
            ACTION_RESET_TASBIH -> {
                WidgetDataProvider.resetTasbih(context)
                updateWidgets(context)
            }
            ACTION_UPDATE_WIDGET -> {
                updateWidgets(context)
            }
        }
    }
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
    }
    
    private fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
                Log.d(TAG, "Updating ${glanceIds.size} Islamic Dashboard widgets")
                glanceIds.forEach { glanceId ->
                    glanceAppWidget.update(context, glanceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "IslamicDashboardWidget"
        const val ACTION_INCREMENT_TASBIH = "com.hieltech.haramblur.widget.INCREMENT_TASBIH"
        const val ACTION_RESET_TASBIH = "com.hieltech.haramblur.widget.RESET_TASBIH"
        const val ACTION_UPDATE_WIDGET = "com.hieltech.haramblur.widget.UPDATE_WIDGET"
    }
}
