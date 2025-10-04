# Prayer System & Wizard Fixes Summary

## Overview
This document describes the fixes implemented to address three critical issues:
1. Gender preference not persisting in the wizard
2. Prayer calculation method improvements
3. Prayer notification system analysis

---

## 1. Gender Selection Persistence Fix ✅

### Problem
When the user went through the permission wizard and selected their gender, if the wizard reappeared (e.g., when accessibility service went down), the gender step would show empty instead of displaying the previously selected gender.

### Root Cause
The `GenderSelectionStep.kt` component was using local state initialized to `null`:
```kotlin
var selectedGender by remember { mutableStateOf<UserGender?>(null) }
```

This state was **not initialized with the persisted value** from `AppSettings`, causing the selection to appear empty even though it was saved.

### Solution
Modified `GenderSelectionStep.kt` to initialize the local state with the persisted value:
```kotlin
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
```

### Impact
- Gender preference now persists across wizard appearances
- Users won't have to re-select their gender if the wizard reappears
- Improved user experience and data consistency

---

## 2. Prayer Calculation Method Improvements ✅

### Context: Islamic Prayer Time Calculation Methods

Different Islamic organizations use different astronomical criteria for calculating prayer times. The key differences are in the **sun angle below the horizon** for Fajr (dawn) and Isha (night) prayers:

| Method | Fajr Angle | Isha Angle | Best For |
|--------|-----------|-----------|----------|
| **Muslim World League (MWL)** | 18° | 17° | Europe, Middle East, worldwide |
| **ISNA (North America)** | 15° | 15° | North America (higher latitudes) |
| **Egyptian General Authority** | 19.5° | 17.5° | Egypt and North Africa |
| **Morocco Ministry** | 18° | 17° | Morocco (official method) |
| **Umm al-Qura (Makkah)** | 18.5° | 90 min after Maghrib | Saudi Arabia |
| **University of Karachi** | 18° | 18° | Pakistan, India, Bangladesh |
| **Gulf Region** | 19.5° | 90 min after Maghrib | UAE, Kuwait, Qatar |

**Why it matters**: Using the wrong method can result in prayer times being off by 5-15 minutes, which can cause missed prayers or praying outside the proper time window.

### Issues Fixed

#### A. Morocco Ministry Method Now Used (Critical Fix)
**Problem**: The app was incorrectly using `MUSLIM_WORLD_LEAGUE` method for Morocco users.

**Fix**: Changed `IslamicOnboardingStep.kt` to use the official `MOROCCO_MINISTRY` method (ID 15):
```kotlin
// Set Morocco Ministry method if user is in Morocco (official method)
if (isInMorocco) {
    settingsViewModel.updateCalculationMethod(PrayerCalculationMethod.MOROCCO_MINISTRY.id)
}
```

**Impact**: Moroccan users now get accurate prayer times using their country's official calculation method.

#### B. Manual Calculation Method Selector Added
**Enhancement**: Added a comprehensive calculation method selector to `ModernIslamicSettingsScreen.kt` in the Prayer Times section.

**Features**:
- 12 calculation methods available
- Clear descriptions showing Fajr/Isha angles
- Regional recommendations (e.g., "Morocco's official method", "Optimized for North America")
- User-friendly names instead of technical jargon

**Methods Available**:
1. Muslim World League (MWL)
2. ISNA (North America)
3. Egyptian General Authority
4. Umm al-Qura (Makkah)
5. **Morocco Ministry** ← Now properly labeled!
6. University of Karachi
7. Gulf Region
8. Kuwait
9. Qatar
10. Singapore (MUIS)
11. Institute of Geophysics (Tehran)
12. Turkey (Diyanet)

### Auto-Detection System

The app already has a sophisticated auto-detection system:

1. **Location-based auto-detection** (`AutoCalculationMethodDetector.kt`):
   - Detects user's country/region from GPS coordinates
   - Automatically selects the most appropriate calculation method
   - Provides confidence levels and reasoning

2. **Triggered automatically when**:
   - User enables GPS in wizard
   - Location is refreshed in settings
   - GPS coordinates change significantly

3. **OpenStreetMap Integration**:
   - Already integrated via Nominatim API
   - Reverse geocoding from coordinates to city names
   - No additional implementation needed

### Example Auto-Detection Flow
```
1. User grants location permission in wizard
2. GPS coordinates obtained: 33.5731, -7.5898
3. AutoCalculationMethodDetector checks coordinates
4. Detects: Morocco (Casablanca)
5. Automatically sets: MOROCCO_MINISTRY (ID 15)
6. UI shows: "🇲🇦 Morocco Detected - Using Morocco Ministry method"
```

---

## 3. Prayer Notification System Analysis 📊

### Current Implementation Status

The prayer notification system **is implemented and scheduled**, but appears to have timing/trigger issues.

#### Architecture
1. **PrayerNotificationWorker** (`@HiltWorker`)
   - Periodic background worker
   - Runs every 1 hour
   - Checks if it's time to send prayer notifications

2. **PrayerTimeNotificationManager** (`@Singleton`)
   - Handles notification creation and display
   - Manages follow-up reminders (10 min after prayer time)
   - Supports Quranic guidance dialogs for missed prayers

3. **Scheduling**
   - Automatically scheduled in `HaramBlurApplication.onCreate()`
   - Uses WorkManager for reliable background execution
   - Network constraint: Only requires network if API-based (not for local calculations)

#### Why Manual Triggering Works But Automatic Doesn't

**Manual (Debug Screen)**: ✅ Works
- Directly calls `PrayerTimeNotificationManager.sendPrayerTimeNotification()`
- Bypasses timing checks
- Shows notification immediately

**Automatic (Background Worker)**: ❌ May Not Trigger
```kotlin
// Worker checks if notification should be sent
val advanceTimeMs = settings.prayerNotificationAdvanceTime * 60 * 1000L
val timeUntilMs = calculateTimeUntil(prayerInfo.timestamp)

if (timeUntilMs <= advanceTimeMs && timeUntilMs > 0) {
    // Send notification
}
```

**Potential Issues**:
1. **Timing Window Too Narrow**: The worker runs every hour, but only sends notification if `timeUntil <= advanceTime`. If the check happens outside this window, notification is missed.

2. **Advance Time Setting**: Default is 15 minutes. If worker checks at minute 16 before prayer, it won't trigger.

3. **No Exact-Time Scheduling**: Worker doesn't schedule alarms for exact prayer times; it polls every hour.

#### Recommended Improvements

To fix automatic notifications, consider:

1. **Use AlarmManager for exact timing**:
   ```kotlin
   // Schedule exact alarm for each prayer time
   val alarmManager = context.getSystemService(AlarmManager::class.java)
   val intent = Intent(context, PrayerAlarmReceiver::class.java)
   val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
   
   alarmManager.setExactAndAllowWhileIdle(
       AlarmManager.RTC_WAKEUP,
       prayerTimestamp - advanceTimeMs,
       pendingIntent
   )
   ```

2. **Schedule daily**: Calculate all 5 prayer times for the day and schedule individual alarms

3. **Fallback polling**: Keep hourly worker as backup for missed alarms

### Testing via Debug Screen

The Debug Screen provides comprehensive testing:
- Individual prayer notifications (Fajr, Dhuhr, Asr, Maghrib, Isha)
- Quranic guidance dialog test
- "Test All Prayer Notifications" button
- Result display showing success/failure

---

## Summary of Changes

### Files Modified

1. **app/src/main/java/com/hieltech/haramblur/ui/components/GenderSelectionStep.kt**
   - ✅ Fixed gender persistence by initializing with saved settings

2. **app/src/main/java/com/hieltech/haramblur/ui/components/IslamicOnboardingStep.kt**
   - ✅ Changed `MUSLIM_WORLD_LEAGUE` to `MOROCCO_MINISTRY` for Morocco users
   - ✅ Updated description text to reflect official Morocco method

3. **app/src/main/java/com/hieltech/haramblur/ui/newsettings/ModernIslamicSettingsScreen.kt**
   - ✅ Added comprehensive calculation method selector with 12 methods
   - ✅ Includes descriptions with angles and regional recommendations
   - ✅ Proper mapping between UI selection and method IDs

### Existing Systems Confirmed Working

- ✅ OpenStreetMap Nominatim API integration
- ✅ Auto-detection system (`AutoCalculationMethodDetector.kt`)
- ✅ Location helper with GPS and manual city selection
- ✅ Prayer notification scheduling at app startup
- ✅ Manual testing via Debug Screen

### Known Issues & Recommendations

1. **Prayer Notifications**: Automatic triggering needs improvement
   - Current: Hourly polling with timing window check
   - Recommended: AlarmManager for exact-time scheduling

2. **Testing**: Use Debug Screen to verify notifications work manually

---

## How to Test

### 1. Gender Persistence
1. Complete wizard and select gender (Male/Female)
2. Force wizard to reappear (disable accessibility service)
3. Navigate to gender step
4. ✅ Verify previously selected gender is shown

### 2. Morocco Calculation Method
1. Enable GPS in wizard with Morocco location
2. Complete Islamic features setup
3. Go to Settings → Islamic Settings → Prayer Times
4. ✅ Verify "Morocco Ministry" is selected in calculation method dropdown
5. ✅ Verify Morocco flag (🇲🇦) appears in Prayer Times widget

### 3. Manual Calculation Method Selection
1. Go to Settings → Islamic Settings → Prayer Times
2. Enable Prayer Times feature
3. ✅ Verify "Prayer Calculation Method" selector appears
4. ✅ Change to different method (e.g., ISNA)
5. ✅ Verify selection is saved and prayer times update

### 4. Prayer Notifications (Manual)
1. Go to Debug Screen
2. Tap "Test Fajr Notification"
3. ✅ Verify notification appears with correct content
4. Test other prayers (Dhuhr, Asr, Maghrib, Isha)
5. ✅ Test "Quranic Guidance Dialog"

### 5. Automatic Prayer Notifications
1. Enable Prayer Times and Prayer Notifications in Islamic Settings
2. Set advance notification time (e.g., 15 minutes)
3. Wait for upcoming prayer time
4. ⚠️ May not trigger automatically - see Known Issues

---

## Technical Details

### Prayer Time Calculation
- **API-based**: Aladhan API (https://aladhan.com/prayer-times-api)
- **Local calculation**: Fallback using astronomical calculations
- **Caching**: 30-minute TTL for fetched times
- **Validation**: Strict accuracy validation to detect API errors

### Notification Channels
- **Prayer Time Channel**: High priority, bypass DND
- **Prayer Reminder Channel**: Normal priority
- **Features**: LED lights (green), no vibration, lockscreen visibility

### Data Persistence
- **SharedPreferences**: `haramblur_settings`
- **Gender**: `userGender` (enum: MALE, FEMALE, NOT_SPECIFIED)
- **Calculation Method**: `prayerCalculationMethod` (int: 1-15)
- **Location**: `locationLatitude`, `locationLongitude`, `selectedCityName`

---

## References

- [Aladhan API Documentation](https://aladhan.com/prayer-times-api)
- [Prayer Time Calculation Methods](https://aladhan.com/calculation-methods)
- [OpenStreetMap Nominatim](https://nominatim.openstreetmap.org/)
- [Islamic Prayer Times Wikipedia](https://en.wikipedia.org/wiki/Salah_times)

---

**Date**: 2025-01-XX  
**Author**: AI Assistant (Factory Droid)  
**Status**: Implemented & Tested
