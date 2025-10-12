# Gender Persistence Bug Fix

## Problem Summary

Users were experiencing a critical bug where gender selection in the setup wizard didn't persist properly. Even after selecting "Male" or "Female" gender during the wizard, the General Settings screen would show a "Gender Required" error message, indicating that `userGender` was still set to `NOT_SPECIFIED`.

This created a frustrating user experience where users believed they had completed the setup correctly, but the app appeared broken and showed error messages for settings they had just configured.

## Root Cause Analysis

The issue was caused by a **race condition** in the gender persistence mechanism:

1. **Asynchronous Write**: The `SettingsRepository.saveSettings()` method used `SharedPreferences.apply()`, which performs asynchronous writes to disk.

2. **Immediate Read**: The UI would read settings immediately after calling the save method, before the background write had completed.

3. **Race Condition**: In many cases, especially on slower devices or under load, the read would occur before the write finished, resulting in the old value (`NOT_SPECIFIED`) being retrieved.

4. **No Verification**: There was no mechanism to verify that the write had actually completed successfully before proceeding to the next step.

### Evidence

- **Language persistence** already used synchronous `commit()` with retry logic (lines 617-652 in `SettingsRepository.kt`)
- **Gender persistence** used async `apply()` (line 551 in original code)
- **SettingsViewModel** had verification logic that detected failures (lines 275-282 in original code)
- The verification logs showed "Gender persistence FAILED" errors in production

## Solution Implemented

We implemented a **synchronous gender persistence method** following the proven pattern already used for language settings. This approach ensures that gender settings are written to disk and verified before the wizard proceeds to the next step.

### Changes Made

#### 1. SettingsRepository.kt - New Synchronous Persistence Method

**File**: `app/src/main/java/com/hieltech/haramblur/data/SettingsRepository.kt`

Added `persistGenderSyncWithResult()` method that:
- Updates the in-memory StateFlow with new gender and blur settings
- Uses synchronous `commit()` instead of async `apply()` to write to SharedPreferences
- Writes all related settings atomically:
  - `user_gender` - the selected gender
  - `blur_male_faces` - whether to blur male faces
  - `blur_female_faces` - whether to blur female faces
  - `enable_face_detection` - enabled automatically
  - `enable_nsfw_detection` - enabled automatically
- Implements retry logic: if first commit fails, waits 100ms and retries once
- Verifies persistence by reading back from SharedPreferences and comparing all values
- Returns `true` if verification succeeds, `false` otherwise
- Logs detailed information at each step for debugging

#### 2. SettingsViewModel.kt - Updated to Use Synchronous Persistence

**File**: `app/src/main/java/com/hieltech/haramblur/ui/SettingsViewModel.kt`

Modified `updateGenderSettings()` method:
- Changed signature from `suspend fun updateGenderSettings(gender: UserGender)` to `suspend fun updateGenderSettings(gender: UserGender): Boolean`
- Replaced async `updateSettings()` call with synchronous `persistGenderSyncWithResult()`
- Removed manual verification logic (now handled by the repository method)
- Returns boolean success indicator to caller
- Enhanced logging to track success/failure

#### 3. GenderSelectionStep.kt - Wait for Confirmation Before Navigation

**File**: `app/src/main/java/com/hieltech/haramblur/ui/components/GenderSelectionStep.kt`

Enhanced the gender selection UI:
- Added `saveError` state variable for error handling
- Updated button onClick to capture the boolean result from `updateGenderSettings()`
- Only proceeds to next step if persistence succeeds (`result == true`)
- Shows user-friendly error message if persistence fails
- Allows user to retry without restarting the wizard
- Added error display UI with dismiss action

#### 4. ModernGeneralSettingsScreen.kt - Improved Error Messaging

**File**: `app/src/main/java/com/hieltech/haramblur/ui/newsettings/ModernGeneralSettingsScreen.kt`

Updated the `NOT_SPECIFIED` gender error message:
- Changed title from "Gender Required" to "Complete Your Profile"
- Made description more inviting and less alarming
- Changed color scheme from `errorContainer` to `tertiaryContainer`
- Changed icon from `Warning` to `Person`
- Added helpful tip: "Go to Settings → Reset Wizard to complete your profile"

#### 5. PermissionWizardViewModel.kt - Added Safety Verification

**File**: `app/src/main/java/com/hieltech/haramblur/ui/PermissionWizardViewModel.kt`

Enhanced `completeGenderSelection()` method:
- Verifies gender is actually set before marking step complete
- Reads current settings from repository
- Only marks step complete if `userGender != NOT_SPECIFIED`
- Logs verification results for debugging
- Prevents wizard from advancing if gender isn't persisted (defensive programming)

## Technical Details

### Synchronous vs Asynchronous Persistence

**Before (Async with `apply()`):**
```kotlin
prefs.edit()
    .putString("user_gender", gender.name)
    .apply() // Returns immediately, writes in background
// UI reads here - might get old value!
```

**After (Sync with `commit()`):**
```kotlin
val success = prefs.edit()
    .putString("user_gender", gender.name)
    .commit() // Blocks until write completes, returns success
// UI reads here - guaranteed to get new value if success == true
```

### Retry Logic

If the first `commit()` fails (rare but possible):
1. Log a warning
2. Wait 100ms to allow system to recover
3. Retry the commit once
4. Return the result of the retry

### Verification

After successful commit:
1. Read back all written values from SharedPreferences
2. Compare each value with what was intended to be written
3. Log any mismatches
4. Return `true` only if all values match

## Testing Instructions

### Manual Testing

1. **Fresh Install Test**:
   - Uninstall the app completely
   - Install the new version
   - Complete the setup wizard
   - Select "Male" gender
   - Verify "Male Profile Active" shows in General Settings
   - No "Gender Required" error should appear

2. **Reset Wizard Test**:
   - Go to Settings → Reset Wizard
   - Complete the wizard again
   - Select "Female" gender
   - Verify "Female Profile Active" shows in General Settings

3. **Persistence Test**:
   - Complete wizard with gender selection
   - Close the app completely (swipe away from recents)
   - Reopen the app
   - Go to General Settings
   - Verify gender is still set correctly

4. **Retry Test** (simulate failure):
   - During wizard, select gender
   - If error appears, verify "Retry" option works
   - Verify error message is user-friendly

### Logcat Verification

Monitor logs during gender selection:

```bash
adb logcat | grep -E "SettingsViewModel|SettingsRepository|GenderSelectionStep|PermissionWizardViewModel"
```

Expected log sequence:
1. `SettingsViewModel: 🧔 Applying MALE profile: blur female faces only`
2. `SettingsViewModel: 💾 Saving gender settings synchronously with verification...`
3. `SettingsRepository: persistGenderSyncWithResult first attempt: gender=MALE, blurMale=false, blurFemale=true, success=true`
4. `SettingsRepository: Gender persistence verification: true for MALE`
5. `SettingsViewModel: ✅ Gender persistence verified successfully: MALE`
6. `PermissionWizardViewModel: Current gender in settings: MALE`
7. `PermissionWizardViewModel: ✅ Gender selection step marked as complete`

### SharedPreferences Verification

Check that values are actually written to disk:

```bash
adb shell run-as com.hieltech.haramblur cat shared_prefs/haramblur_settings.xml | grep -E "gender|blur"
```

Expected output (for Male gender):
```xml
<string name="user_gender">MALE</string>
<boolean name="blur_male_faces" value="false" />
<boolean name="blur_female_faces" value="true" />
<boolean name="enable_face_detection" value="true" />
<boolean name="enable_nsfw_detection" value="true" />
```

## Related Files

All files modified in this fix:

1. **app/src/main/java/com/hieltech/haramblur/data/SettingsRepository.kt**
   - Added `persistGenderSyncWithResult()` method

2. **app/src/main/java/com/hieltech/haramblur/ui/SettingsViewModel.kt**
   - Modified `updateGenderSettings()` to use synchronous persistence and return boolean

3. **app/src/main/java/com/hieltech/haramblur/ui/components/GenderSelectionStep.kt**
   - Added error handling and retry logic
   - Wait for persistence confirmation before navigation

4. **app/src/main/java/com/hieltech/haramblur/ui/newsettings/ModernGeneralSettingsScreen.kt**
   - Improved error messaging for NOT_SPECIFIED gender

5. **app/src/main/java/com/hieltech/haramblur/ui/PermissionWizardViewModel.kt**
   - Added verification before marking step complete

6. **GENDER_PERSISTENCE_FIX.md** (this file)
   - Comprehensive documentation of the fix

## Future Improvements

### Consider DataStore Migration

SharedPreferences has known limitations with async operations. Consider migrating to Jetpack DataStore:

```kotlin
// Future improvement: Use DataStore instead of SharedPreferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

suspend fun saveGender(gender: UserGender) {
    dataStore.edit { preferences ->
        preferences[GENDER_KEY] = gender.name
    }
    // DataStore guarantees consistency without manual verification
}
```

### Evaluate Other Critical Settings

Review other settings that might need synchronous persistence:
- Language preference (already uses sync)
- Quality mode
- Detection sensitivity
- Any setting that affects app behavior immediately

### Add Automated Tests

Create unit tests for persistence verification:

```kotlin
@Test
fun `gender persistence should be verified before proceeding`() = runTest {
    val repository = SettingsRepository(mockPrefs)
    val result = repository.persistGenderSyncWithResult(
        gender = UserGender.MALE,
        blurMaleFaces = false,
        blurFemaleFaces = true
    )
    assertTrue(result)
    verify(mockPrefs).commit() // Verify sync commit was used
}
```

### Add Analytics

Track persistence failures to monitor the fix effectiveness:

```kotlin
if (!success) {
    analytics.logEvent("gender_persistence_failed", mapOf(
        "gender" to gender.name,
        "retry_count" to retryCount
    ))
}
```

## Conclusion

This fix addresses the root cause of the gender persistence bug by ensuring synchronous, verified writes to SharedPreferences. The solution follows existing patterns in the codebase (language persistence), includes comprehensive error handling, and provides a better user experience with clear error messages and retry options.

The fix is minimal, targeted, and doesn't introduce regressions by changing the generic `saveSettings()` method. Instead, it creates a dedicated path for critical settings that require immediate persistence verification.

