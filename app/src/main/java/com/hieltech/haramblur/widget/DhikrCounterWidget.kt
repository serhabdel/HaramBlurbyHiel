package com.hieltech.haramblur.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.hieltech.haramblur.MainActivity
import com.hieltech.haramblur.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Simple Dhikr Counter Widget (2x2)
 * Large tap area for easy counting
 */
class DhikrCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DhikrCounterGlanceWidget()
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("DhikrWidget", "onReceive: ${intent.action}")
        
        when (intent.action) {
            ACTION_INCREMENT -> {
                Log.d("DhikrWidget", "INCREMENT action received")
                val (count, index) = WidgetDataProvider.incrementTasbih(context)
                Log.d("DhikrWidget", "New count=$count, index=$index")
                updateAllWidgets(context)
            }
            ACTION_RESET -> {
                Log.d("DhikrWidget", "RESET action received")
                WidgetDataProvider.resetTasbih(context)
                updateAllWidgets(context)
            }
        }
    }
    
    private fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val dhikrWidget = DhikrCounterGlanceWidget()
                
                // Get all Dhikr widget IDs and update their state
                val dhikrIds = manager.getGlanceIds(DhikrCounterGlanceWidget::class.java)
                Log.d("DhikrWidget", "Updating ${dhikrIds.size} Dhikr widgets with state...")
                
                val count = WidgetDataProvider.getTasbihCount(context)
                val index = WidgetDataProvider.getTasbihIndex(context)
                
                dhikrIds.forEach { glanceId ->
                    // Update state with timestamp to force re-render
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[intPreferencesKey("tasbih_count")] = count
                            this[intPreferencesKey("tasbih_index")] = index
                            // Add timestamp to force state change detection
                            this[androidx.datastore.preferences.core.longPreferencesKey("update_time")] = System.currentTimeMillis()
                        }
                    }
                    Log.d("DhikrWidget", "State updated for $glanceId, calling update...")
                    // Then update the widget
                    dhikrWidget.update(context, glanceId)
                }
                
                // Update Dashboard widgets too - with state
                val dashboardWidget = IslamicDashboardGlanceWidget()
                val dashboardIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
                Log.d("DhikrWidget", "Updating ${dashboardIds.size} Dashboard widgets with state...")
                dashboardIds.forEach { glanceId ->
                    // Update state for dashboard widget too
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[intPreferencesKey("tasbih_count")] = count
                            this[intPreferencesKey("tasbih_index")] = index
                            this[androidx.datastore.preferences.core.longPreferencesKey("update_time")] = System.currentTimeMillis()
                        }
                    }
                    dashboardWidget.update(context, glanceId)
                }
                
                Log.d("DhikrWidget", "All widgets updated with state, count=$count")
                
                // Also trigger a one-time worker update for prayer countdown refresh
                WidgetUpdateWorker.triggerImmediateUpdate(context)
            } catch (e: Exception) {
                Log.e("DhikrWidget", "Error updating widgets", e)
            }
        }
    }
    
    companion object {
        const val ACTION_INCREMENT = "com.hieltech.haramblur.widget.DHIKR_INCREMENT"
        const val ACTION_RESET = "com.hieltech.haramblur.widget.DHIKR_RESET"
    }
}

class DhikrCounterGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    
    // Use stateDefinition to force widget updates
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("DhikrWidget", "provideGlance called for id=$id")
        
        provideContent {
            // Read from Glance state - this is what triggers re-renders
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val count = prefs[intPreferencesKey("tasbih_count")] ?: WidgetDataProvider.getTasbihCount(context)
            val index = prefs[intPreferencesKey("tasbih_index")] ?: WidgetDataProvider.getTasbihIndex(context)
            val tasbih = WidgetDataProvider.tasbihTexts.getOrNull(index) ?: WidgetDataProvider.tasbihTexts[0]
            
            Log.d("DhikrWidget", "Rendering widget with count=$count from state")
            WidgetContentWithData(context, count, index, tasbih)
        }
    }
    
    @Composable
    private fun WidgetContentWithData(
        context: Context,
        tasbihCount: Int,
        tasbihIndex: Int,
        currentTasbih: WidgetDataProvider.TasbihData
    ) {
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(
                        ColorProvider(Color(0xFF1B5E20))
                    )
                    .cornerRadius(20.dp)
                    .clickable(actionSendBroadcast(
                        Intent(context, DhikrCounterWidgetReceiver::class.java).apply {
                            action = DhikrCounterWidgetReceiver.ACTION_INCREMENT
                        }
                    )),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arabic text
                    Text(
                        text = currentTasbih.arabic,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    // Transliteration
                    Text(
                        text = currentTasbih.transliteration,
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // Counter display
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color.White.copy(alpha = 0.2f)))
                            .cornerRadius(30.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$tasbihCount",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFFD700)),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    // Progress indicator
                    Row(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = GlanceModifier
                                    .size(8.dp)
                                    .background(
                                        ColorProvider(
                                            if (index == tasbihIndex) Color(0xFFFFD700)
                                            else Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                    .cornerRadius(4.dp),
                                contentAlignment = Alignment.Center
                            ) {}
                            if (index < 2) {
                                Spacer(modifier = GlanceModifier.width(6.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    Text(
                        text = context.getString(R.string.widget_tap_anywhere_count),
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                            fontSize = 9.sp
                        )
                    )
                }
                
                // Reset button in corner
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .background(ColorProvider(Color.White.copy(alpha = 0.2f)))
                            .cornerRadius(12.dp)
                            .clickable(actionSendBroadcast(
                                Intent(context, DhikrCounterWidgetReceiver::class.java).apply {
                                    action = DhikrCounterWidgetReceiver.ACTION_RESET
                                }
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.widget_reset_icon),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action callback for incrementing dhikr counter - updates widget immediately
 */
class DhikrIncrementAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("DhikrAction", "Dhikr increment clicked, glanceId=$glanceId")
        val (count, index) = WidgetDataProvider.incrementTasbih(context)
        Log.d("DhikrAction", "New count=$count, index=$index")
        
        // Update THIS widget immediately
        DhikrCounterGlanceWidget().update(context, glanceId)
        
        // Also update Islamic Dashboard widgets
        try {
            val manager = GlanceAppWidgetManager(context)
            val dashboardWidget = IslamicDashboardGlanceWidget()
            val dashboardIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
            dashboardIds.forEach { id ->
                dashboardWidget.update(context, id)
            }
        } catch (e: Exception) {
            Log.e("DhikrAction", "Error updating Dashboard widgets", e)
        }
    }
}

/**
 * Action callback for resetting dhikr counter - updates widget immediately
 */
class DhikrResetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("DhikrAction", "Dhikr reset clicked, glanceId=$glanceId")
        WidgetDataProvider.resetTasbih(context)
        
        // Update THIS widget immediately
        DhikrCounterGlanceWidget().update(context, glanceId)
        
        // Also update Islamic Dashboard widgets
        try {
            val manager = GlanceAppWidgetManager(context)
            val dashboardWidget = IslamicDashboardGlanceWidget()
            val dashboardIds = manager.getGlanceIds(IslamicDashboardGlanceWidget::class.java)
            dashboardIds.forEach { id ->
                dashboardWidget.update(context, id)
            }
        } catch (e: Exception) {
            Log.e("DhikrAction", "Error updating Dashboard widgets", e)
        }
    }
}
