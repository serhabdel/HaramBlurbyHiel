# Prayer Notification Testing Implementation Plan

## Overview
This document outlines the implementation plan for adding test triggers for Salah notifications and Quranic guidance dialogs to the DebugScreen in the HaramBlur application.

## Current System Analysis

### Prayer Notification System Components
1. **PrayerTimeNotificationManager**: Manages prayer time notifications with intelligent follow-up reminders and Quranic guidance
2. **PrayerGuidanceActivity**: Activity that displays Quranic guidance about the importance of timely prayer
3. **PrayerNotificationReceiver**: Handles prayer notification button actions
4. **PrayerNotificationWorker**: Background worker for scheduling and sending prayer time notifications

### Current DebugScreen Structure
- **CompactDebugScreen**: For small screens (< 600dp width)
- **MediumDebugScreen**: For medium screens (600-840dp width)
- **ExpandedDebugScreen**: For large screens (> 840dp width)
- **DebugViewModel**: Handles debug actions and state management

## Implementation Plan

### Phase 1: Update DebugViewModel

#### 1.1 Add PrayerTimeNotificationManager Dependency
```kotlin
@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDetectionEngine: ContentDetectionEngine,
    private val faceDetectionManager: FaceDetectionManager,
    private val mlModelManager: MLModelManager,
    private val settingsRepository: SettingsRepository,
    private val prayerTimeNotificationManager: PrayerTimeNotificationManager // NEW
) : ViewModel() {
```

#### 1.2 Add Prayer Notification Testing Methods
```kotlin
/**
 * Test prayer time notification for a specific prayer
 */
fun testPrayerNotification(prayerName: String) {
    addDebugLog(TAG, "Testing prayer notification for: $prayerName")
    viewModelScope.launch {
        try {
            val prayerEnum = PrayerName.valueOf(prayerName.uppercase())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            
            prayerTimeNotificationManager.sendPrayerTimeNotification(prayerEnum, currentTime)
            
            addDebugLog(TAG, "Prayer notification sent for $prayerName")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Prayer notification sent for $prayerName"
            )
        } catch (e: Exception) {
            addDebugLog(TAG, "Failed to send prayer notification: ${e.message}")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Failed: ${e.message}"
            )
        }
    }
}

/**
 * Test Quranic guidance dialog
 */
fun testQuranicGuidance(prayerName: String) {
    addDebugLog(TAG, "Testing Quranic guidance for: $prayerName")
    viewModelScope.launch {
        try {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            
            // Simulate user indicating they haven't prayed
            prayerTimeNotificationManager.handlePrayerNotCompleted(prayerName, currentTime)
            
            addDebugLog(TAG, "Quranic guidance triggered for $prayerName")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Quranic guidance shown for $prayerName"
            )
        } catch (e: Exception) {
            addDebugLog(TAG, "Failed to show Quranic guidance: ${e.message}")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Failed: ${e.message}"
            )
        }
    }
}

/**
 * Test prayer reminder notification
 */
fun testPrayerReminder(prayerName: String) {
    addDebugLog(TAG, "Testing prayer reminder for: $prayerName")
    viewModelScope.launch {
        try {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            
            // Simulate reminder notification
            prayerTimeNotificationManager.handlePrayerNotCompleted(prayerName, currentTime)
            
            addDebugLog(TAG, "Prayer reminder sent for $prayerName")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Prayer reminder sent for $prayerName"
            )
        } catch (e: Exception) {
            addDebugLog(TAG, "Failed to send prayer reminder: ${e.message}")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Failed: ${e.message}"
            )
        }
    }
}

/**
 * Test all prayer notifications in sequence
 */
fun testAllPrayerNotifications() {
    addDebugLog(TAG, "Testing all prayer notifications")
    viewModelScope.launch {
        try {
            val prayers = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            
            prayers.forEach { prayerName ->
                val prayerEnum = PrayerName.valueOf(prayerName)
                prayerTimeNotificationManager.sendPrayerTimeNotification(prayerEnum, currentTime)
                kotlinx.coroutines.delay(2000) // 2 second delay between notifications
            }
            
            addDebugLog(TAG, "All prayer notifications sent successfully")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "All 5 prayer notifications sent"
            )
        } catch (e: Exception) {
            addDebugLog(TAG, "Failed to send all prayer notifications: ${e.message}")
            _debugState.value = _debugState.value.copy(
                lastActionResult = "Failed: ${e.message}"
            )
        }
    }
}
```

### Phase 2: Update DebugScreen UI

#### 2.1 Add Prayer Notification Testing Section to CompactDebugScreen
```kotlin
// After Behavioral Actions Testing Card (around line 346)

// Prayer Notification Testing
Card {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.prayer_notification_testing),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.test_prayer_notification_system),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Prayer Test Buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.testPrayerNotification("Fajr") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_fajr_notification))
            }

            Button(
                onClick = { viewModel.testPrayerNotification("Dhuhr") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_dhuhr_notification))
            }

            Button(
                onClick = { viewModel.testPrayerNotification("Asr") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_asr_notification))
            }

            Button(
                onClick = { viewModel.testPrayerNotification("Maghrib") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_maghrib_notification))
            }

            Button(
                onClick = { viewModel.testPrayerNotification("Isha") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.test_isha_notification))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quranic Guidance Test
            Button(
                onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.test_quranic_guidance))
            }

            // Test All Notifications
            Button(
                onClick = { viewModel.testAllPrayerNotifications() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(stringResource(R.string.test_all_prayer_notifications))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.last_prayer_test_result),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = debugState.lastActionResult ?: "No prayer tests yet",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
```

#### 2.2 Add Prayer Notification Testing Section to MediumDebugScreen
```kotlin
// After Behavioral Actions Testing Card (around line 675)

// Prayer Notification Testing
Card {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            "🕌 Prayer Notification Testing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Test the prayer notification system:",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Prayer Test Buttons in grid layout
        val prayers = listOf(
            "Fajr" to "🌅 Fajr",
            "Dhuhr" to "☀️ Dhuhr",
            "Asr" to "🌤️ Asr",
            "Maghrib" to "🌅 Maghrib",
            "Isha" to "🌙 Isha"
        )

        prayers.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (prayer, label) ->
                    Button(
                        onClick = { viewModel.testPrayerNotification(prayer) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Special Test Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📖 Quranic Guidance")
            }

            Button(
                onClick = { viewModel.testAllPrayerNotifications() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("🔄 Test All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Last Prayer Test Result:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = debugState.lastActionResult ?: "No prayer tests yet",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

#### 2.3 Add Prayer Notification Testing Section to ExpandedDebugScreen
```kotlin
// Add to the right column after Behavioral Actions Testing (around line 1032)

// Prayer Notification Testing
Card {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "🕌 Prayer Notification Testing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Test prayer notification system:",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Prayer Test Buttons in grid
        val prayers = listOf(
            "Fajr" to "🌅 Fajr",
            "Dhuhr" to "☀️ Dhuhr",
            "Asr" to "🌤️ Asr",
            "Maghrib" to "🌅 Maghrib",
            "Isha" to "🌙 Isha"
        )

        prayers.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (prayer, label) ->
                    Button(
                        onClick = { viewModel.testPrayerNotification(prayer) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Special Test Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.testQuranicGuidance("Dhuhr") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📖 Quranic Guidance")
            }

            Button(
                onClick = { viewModel.testPrayerReminder("Asr") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("⏰ Prayer Reminder")
            }

            Button(
                onClick = { viewModel.testAllPrayerNotifications() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("🔄 Test All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Last Prayer Test Result:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = debugState.lastActionResult ?: "No prayer tests yet",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

### Phase 3: Add Required String Resources

#### 3.1 Add to strings.xml
```xml
<!-- Prayer Notification Testing -->
<string name="prayer_notification_testing">Prayer Notification Testing</string>
<string name="test_prayer_notification_system">Test the prayer notification and Quranic guidance system</string>
<string name="test_fajr_notification">Test Fajr Notification</string>
<string name="test_dhuhr_notification">Test Dhuhr Notification</string>
<string name="test_asr_notification">Test Asr Notification</string>
<string name="test_maghrib_notification">Test Maghrib Notification</string>
<string name="test_isha_notification">Test Isha Notification</string>
<string name="test_quranic_guidance">Test Quranic Guidance Dialog</string>
<string name="test_all_prayer_notifications">Test All Prayer Notifications</string>
<string name="last_prayer_test_result">Last Prayer Test Result</string>
```

### Phase 4: Update Dependencies

#### 4.1 Add PrayerTimeNotificationManager to DebugViewModel
```kotlin
// In DebugViewModel.kt, add import
import com.hieltech.haramblur.services.PrayerTimeNotificationManager
import com.hieltech.haramblur.data.prayer.PrayerName
```

#### 4.2 Update Hilt Module
```kotlin
// In di/DataModule.kt, ensure PrayerTimeNotificationManager is provided
@Singleton
@Provides
fun providePrayerTimeNotificationManager(
    @ApplicationContext context: Context,
    settingsRepository: SettingsRepository,
    prayerTimesRepository: PrayerTimesRepository,
    quranicRepository: QuranicRepository
): PrayerTimeNotificationManager {
    return PrayerTimeNotificationManager(context, settingsRepository, prayerTimesRepository, quranicRepository)
}
```

## Testing Strategy

### 1. Unit Testing
- Test each prayer notification method individually
- Test Quranic guidance dialog triggering
- Test error handling for invalid prayer names

### 2. Integration Testing
- Test notification flow from DebugScreen to PrayerTimeNotificationManager
- Test Quranic guidance activity display
- Test prayer reminder functionality

### 3. UI Testing
- Test button visibility and interaction on all screen sizes
- Test result display in the debug log
- Test accessibility of new buttons

## Expected Behavior

### 1. Prayer Notification Test
- User taps "Test Fajr Notification" button
- DebugViewModel calls testPrayerNotification("Fajr")
- PrayerTimeNotificationManager sends notification
- Result appears in debug log
- Actual notification appears in system notification tray

### 2. Quranic Guidance Test
- User taps "Test Quranic Guidance" button
- DebugViewModel calls testQuranicGuidance("Dhuhr")
- PrayerTimeNotificationManager triggers guidance dialog
- PrayerGuidanceActivity displays overlay dialog
- Result appears in debug log

### 3. Test All Notifications
- User taps "Test All Prayer Notifications" button
- DebugViewModel calls testAllPrayerNotifications()
- All 5 prayer notifications are sent with 2-second delays
- Results appear in debug log
- All notifications appear in sequence

## Implementation Notes

1. **Error Handling**: All test methods include try-catch blocks to handle exceptions gracefully
2. **Logging**: All actions are logged to both the debug screen and Android Log
3. **State Management**: Results are stored in the debug state for display
4. **UI Consistency**: Same functionality is available across all screen sizes with appropriate layouts
5. **Accessibility**: Buttons have proper content descriptions and are accessible

## Success Criteria

1. ✅ All prayer notification buttons trigger correct notifications
2. ✅ Quranic guidance button displays the guidance dialog
3. ✅ Test results appear correctly in debug log
4. ✅ UI is consistent across Compact, Medium, and Expanded screen sizes
5. ✅ Error handling works correctly for invalid inputs
6. ✅ All functionality is accessible and user-friendly

## Future Enhancements

1. **Custom Prayer Time**: Allow users to input custom prayer times for testing
2. **Notification Customization**: Test different notification styles and content
3. **Prayer Completion Tracking**: Test prayer completion flow
4. **Multi-language Testing**: Test notifications in different languages
5. **Advanced Scenarios**: Test edge cases like overlapping prayer times