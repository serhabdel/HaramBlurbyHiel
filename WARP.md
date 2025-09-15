# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Context

**HaramBlur** is an Android application that provides real-time Islamic content filtering through an Accessibility Service. It uses on-device machine learning to detect and blur inappropriate content system-wide, while providing spiritual guidance through Quranic verses and Dhikr reminders.

**Key Constraints:**
- Android-only, privacy-first architecture with all processing on-device
- Memory-constrained development environment - **never use Gradle daemon**
- Accessibility Service and overlay permissions require user approval
- ML inference must be battery-optimized and respect thermal limits
- Multi-language support with right-to-left layout awareness

---

## Common Development Commands

**Critical:** Always use `--no-daemon` to prevent memory issues on constrained systems.

### Build and Install
```bash
# Clean build
./gradlew --no-daemon clean

# Debug APK
./gradlew --no-daemon :app:assembleDebug

# Release bundle
./gradlew --no-daemon :app:bundleRelease

# Install debug on connected device
./gradlew --no-daemon :app:installDebug

# Uninstall
adb uninstall com.hieltech.haramblur
```

### Testing
```bash
# All unit tests (debug variant)
./gradlew --no-daemon :app:testDebugUnitTest

# All instrumentation tests (requires device/emulator)
./gradlew --no-daemon :app:connectedDebugAndroidTest

# Single unit test class
./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.hieltech.haramblur.detection.ContentDetectionEngineTest'

# Single unit test method
./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.hieltech.haramblur.detection.ContentDetectionEngineTest.shouldAnalyzeContent'

# Single instrumentation test
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hieltech.haramblur.ui.SettingsScreenTest
```

### Code Quality
```bash
# Android Lint (debug variant)
./gradlew --no-daemon :app:lintDebug

# Full verification (includes lint and tests)
./gradlew --no-daemon :app:check

# Kotlin compilation check
./gradlew --no-daemon :app:compileDebugKotlin
```

### Development Workflow
```bash
# Start/stop ADB server
adb kill-server && adb start-server

# List connected devices
adb devices

# Monitor HaramBlur service logs
adb logcat | grep -E "(HaramBlur|ContentDetection|FaceDetection|AccessibilityService)"

# Monitor specific service
adb logcat --pid=$(adb shell pidof com.hieltech.haramblur) | grep "HaramBlurAccessibilityService"

# Emergency reset overlays (if stuck)
adb shell am broadcast -a com.hieltech.haramblur.EMERGENCY_RESET
```

### Compose Development
```bash
# Enable Compose compiler metrics (optional, local development only)
./gradlew --no-daemon :app:assembleDebug \
  -Pplugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=build/compose_metrics \
  -Pplugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=build/compose_metrics
```

---

## High-Level Architecture Overview

### Core Components

**Main Application Flow:**
```
MainActivity (Compose UI) 
    ↓
HaramBlurAccessibilityService (System-wide monitoring)
    ↓
ContentDetectionEngine (ML pipeline)
    ↓
BlurOverlayManager (WindowManager overlays)
```

### Accessibility Service Architecture
The `HaramBlurAccessibilityService` is the central runtime component:
- Monitors system-wide content through AccessibilityEvents
- Captures screen content via permitted APIs
- Triggers ML-based content analysis pipeline
- Manages blur overlays and spiritual guidance displays
- Runs as foreground service for stability

### Detection Pipeline
Multi-layer on-device processing pipeline:

1. **Frame Acquisition**: Screen capture via Accessibility APIs, throttled for battery
2. **Preprocessing**: Scaling, color normalization, region-of-interest analysis
3. **ML Classification**: 
   - TensorFlow Lite models: `nsfw_mobilenet_v2_140_224.1.tflite`, `model_lite_gender_q.tflite`
   - Google ML Kit Face Detection for human faces
4. **Contextual Analysis**: Heuristics to reduce false positives (app whitelisting, UI chrome detection)
5. **Decision Engine**: Apply blur overlay, show Quranic verses, log events to Room DB
6. **Telemetry**: Local-only logging, no network transmission

### Islamic Features Integration
- **Quranic Verses**: Contextual display on content blocking events
- **Dhikr System**: Timed remembrance notifications and overlay displays
- **Prayer Times**: Location-based calculations with local methods
- **Multi-language**: Arabic, English, French, Indonesian with RTL layout support

### Data Layer
- **Room Database**: `SiteBlockingDatabase` (version 7) stores blocked content, user preferences, logs
- **Repositories**: `SettingsRepository`, `QuranicRepository`, `LogRepository`, etc.
- **DataStore**: Lightweight settings and locale preferences

### Dependency Injection (Hilt)
Key modules:
- `DataModule`: Database, repositories, core data components
- `EnhancedDetectionModule`: ML models, detection engine, performance components
- `NetworkModule`: API clients for prayer times
- `UIModule`: ViewModels and UI-related dependencies

---

## Project-Specific Context

### Purpose and Values
Real-time Islamic content filtering to help Muslim users avoid haram content while providing spiritual support through technology.

### Core Technologies
- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose (BOM 2024.02.00)
- **Architecture**: MVVM + Hilt DI
- **Database**: Room with SQLite
- **ML**: TensorFlow Lite 2.14.0 + Google ML Kit
- **Build**: AGP 8.12.1, min SDK 25, target SDK 36

### Privacy-First Design
- All ML inference happens on-device
- No screenshots or content uploaded to servers
- Local-only logging with user consent for export
- No telemetry or analytics services

### Performance Characteristics
- Battery-optimized detection with throttling
- GPU acceleration for TensorFlow Lite where available
- Adaptive processing based on device thermal state
- Memory-efficient bitmap processing with recycling

### Permission Requirements
- **BIND_ACCESSIBILITY_SERVICE**: Core functionality (user must enable in Settings)
- **SYSTEM_ALERT_WINDOW**: Blur overlays and guidance UI
- **POST_NOTIFICATIONS**: Prayer times and Dhikr reminders (Android 13+)
- **ACCESS_FINE_LOCATION**: Prayer time calculations (optional)
- **PACKAGE_USAGE_STATS**: Enhanced app filtering (optional)

---

## Development Guidelines

### Memory Management
- **Never use Gradle daemon**: Always pass `--no-daemon` flag
- Keep JVM heap conservative: `-Xmx2048m` as configured in `gradle.properties`
- Prefer module-scoped Gradle tasks to avoid cross-module pressure

### Package Organization
```
com.hieltech.haramblur/
├── accessibility/          # HaramBlurAccessibilityService, overlays
├── data/                   # Repositories, Room database, settings
├── detection/              # ContentDetectionEngine, ML pipeline
├── di/                     # Hilt dependency injection modules
├── ml/                     # TensorFlow Lite, ML Kit wrappers
├── services/               # Background services, notifications
├── ui/                     # Jetpack Compose screens and components
└── utils/                  # Utilities, locale management
```

### Naming Conventions
- Classes: `PascalCase` with descriptive suffixes (`ContentDetectionEngine`, `SettingsRepository`, `HomeViewModel`)
- Functions: `camelCase` (`analyzeContent()`, `getCurrentSettings()`)
- Constants: `UPPER_SNAKE_CASE` (`TAG = "ClassName"`, `MAX_RETRY_COUNT = 3`)
- Hilt modules: `DataModule`, `EnhancedDetectionModule`, etc.

### Testing Approach
- **Unit Tests**: ViewModels with fake repositories, detection engine with synthetic inputs
- **Instrumentation Tests**: Compose UI, service integration, overlay behavior
- **ML Testing**: Use fake classifiers via DI, avoid real TensorFlow Lite in tests
- **Database Testing**: Room in-memory database with migration tests
- Keep tests hermetic - no network dependencies

### Code Quality Standards
- Android Lint warnings treated as errors for CI
- No network logging of user content
- Redact PII in debug logs
- Document public APIs with KDoc
- Use structured logging with LogRepository

### ML Model Management
- Store models in `app/src/main/assets/models/`
- Load via AssetFileDescriptor for memory efficiency
- Version models explicitly and document input requirements
- Keep inference on background dispatchers (Dispatchers.Default)

---

## Permissions and Developer Onboarding

### Accessibility Service Setup
The app's core functionality requires Accessibility Service permission:

1. **User Must Enable**: Cannot be granted programmatically on modern Android
2. **In-App Guidance**: Deep link to Settings: `Settings.ACTION_ACCESSIBILITY_SETTINGS`
3. **Testing Limitation**: ADB commands like `settings put secure enabled_accessibility_services` may be blocked by OEMs

### Overlay Permission
Required for blur overlays and guidance UI:

```bash
# Grant via ADB (not reliable on all devices/versions)
adb shell appops set com.hieltech.haramblur SYSTEM_ALERT_WINDOW allow

# Preferred: Show system permission screen
# Intent: Settings.ACTION_MANAGE_OVERLAY_PERMISSION
```

### Additional Permissions
```bash
# Notifications (Android 13+)
# Requires runtime permission prompt

# Location for prayer times (optional)
# Runtime permission with proper fallbacks

# Usage stats for enhanced filtering (optional) 
# Requires user to enable in Special App Access
```

### Developer Testing Shortcuts
The app includes debug-only deep links to permission settings screens. Consider adding helper scripts:

```bash
# scripts/dev/open_accessibility_settings.sh
adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS

# scripts/dev/open_overlay_settings.sh  
adb shell am start -a android.settings.action.MANAGE_OVERLAY_PERMISSION \
  -d package:com.hieltech.haramblur

# scripts/dev/log_service.sh
adb logcat | grep -E "(HaramBlur|ContentDetection|AccessibilityService)"
```

---

## Testing Strategy

### Unit Testing Focus Areas
- **ViewModels**: State transitions using fake repositories
- **Detection Logic**: ContentDetectionEngine with synthetic bitmap inputs
- **Rule Engine**: Mapping ML confidence scores to blocking decisions
- **Repository Layer**: Database operations and settings management

### Instrumentation Testing
- **Compose UI**: Settings screens, onboarding flow, locale switching
- **Service Integration**: Launch app, verify service binding, overlay toggling
- **Database Migrations**: Room schema migrations from all previous versions

### ML Testing Strategy
- **Fake Dependencies**: Use Hilt to inject fake ML classifiers returning deterministic results
- **Limited Real Models**: Small subset of instrumentation tests with actual TensorFlow Lite inference
- **Input Validation**: Test model input preprocessing and output interpretation

### Performance Testing
- **Memory Profiling**: Monitor bitmap allocation and cleanup
- **Battery Usage**: Measure impact during extended detection sessions
- **Thermal Throttling**: Verify detection adapts to device temperature

---

## Performance and Battery Optimization

### Detection Pipeline Optimization
- **Throttling**: Minimum 1-2 second intervals between analyses
- **Frame Scaling**: Downsample images for TensorFlow Lite input (224x224, 448x448)
- **Tensor Reuse**: Maintain allocated input/output tensors, avoid recreation
- **Caching**: Cache recent analysis results to avoid redundant computation

### Memory Management
- **Bitmap Pooling**: Reuse bitmap objects to reduce GC pressure
- **Model Caching**: Keep TensorFlow Lite interpreters loaded in memory
- **Resource Cleanup**: Proper lifecycle management for overlays and services
- **Leak Prevention**: Use weak references and proper service unbinding

### Adaptive Processing
```kotlin
// Example performance modes in codebase
enum class PerformanceMode {
    ULTRA_FAST,    // Basic detection, 500ms intervals
    FAST,          // Balanced processing, 1s intervals  
    BALANCED,      // Default mode, 2s intervals
    HIGH_QUALITY   // Enhanced analysis, 3s intervals
}
```

### Battery Considerations
- **Thermal Throttling**: Reduce detection frequency when device overheats
- **Battery Saver Mode**: Pause heavy ML inference when enabled
- **App Whitelisting**: Skip processing for known-safe applications
- **Background Optimization**: Work with Android's doze mode and battery optimization

---

## Privacy and Data Handling

### On-Device Processing
- **No Screenshot Persistence**: Process bitmaps in-memory only, immediate cleanup
- **Local ML Inference**: TensorFlow Lite and ML Kit run entirely on-device
- **No Network Transmission**: Content analysis never leaves the device
- **User Control**: Explicit consent required for any log export functionality

### Logging and Debugging
- **Structured Logging**: Use LogRepository for consistent log formatting
- **Content Redaction**: Never log raw OCR text or screenshot metadata
- **Sampling**: Rate-limit debug logs to reduce database size
- **User Export**: Provide logs export with clear privacy warnings

### Data Retention
- **Room Database**: Local SQLite storage only
- **Automatic Cleanup**: Configurable log retention periods
- **User Purge**: Settings option to clear all logs and cached data

---

## Internationalization and Locale Management

### Locale Handling
- **Per-App Locale**: Use `AppCompatDelegate.setApplicationLocales()` on Android 13+
- **Legacy Support**: Custom locale wrapper for Android < 13
- **No Device Impact**: App locale changes don't affect system language

### Right-to-Left Layout Support
- **Arabic Support**: Proper RTL layout in Jetpack Compose
- **Font Selection**: Arabic-compatible fonts for Islamic content
- **Layout Direction**: Automatic layout mirroring for RTL languages

### Content Localization
- **Quranic Verses**: Multiple translations stored in Room database
- **Dhikr Content**: Localized remembrance phrases with transliteration
- **UI Strings**: Standard Android string resources for app interface

### Supported Languages
- Arabic (العربية) - RTL primary language
- English - Default fallback
- French (Français)
- Indonesian (Bahasa Indonesia)

---

## Troubleshooting and Known Issues

### Gradle Build Issues
```bash
# Out of memory errors
./gradlew --no-daemon clean
rm -rf .gradle/caches/

# Force daemon off if accidentally enabled
export GRADLE_OPTS="-Dorg.gradle.daemon=false"
```

### Accessibility Service Issues
- **Service Not Starting**: Verify enabled in Settings > Accessibility > HaramBlur
- **Service Killed**: Check battery optimization whitelist, OEM power management
- **Permissions Reset**: Some OEMs reset accessibility services on updates

### Overlay Problems
- **Overlays Not Showing**: Confirm SYSTEM_ALERT_WINDOW permission granted
- **Stuck Overlays**: Use emergency reset broadcast: `adb shell am broadcast -a com.hieltech.haramblur.EMERGENCY_RESET`
- **OEM Restrictions**: Some manufacturers require additional overlay permissions

### ML and Performance Issues
- **TensorFlow Lite Crashes**: Disable NNAPI/GPU delegates if unstable
- **High CPU Usage**: Check detection throttling is working properly
- **Model Loading Failures**: Verify assets packaging with `./gradlew assembleDebug`

### Testing and Development
- **Instrumentation Test Flakiness**: Ensure single emulator, disable animations
- **Database Migration Failures**: Check Room migration paths and schema versions
- **Compose Preview Issues**: Verify Compose compiler version compatibility

---

## Glossary

- **AS**: Accessibility Service - Android system service for app interaction
- **TFLite**: TensorFlow Lite - On-device machine learning inference engine
- **ML Kit**: Google's on-device machine learning APIs
- **ROI**: Region of Interest - Specific areas in images targeted for analysis
- **UDF**: Unidirectional Data Flow - Architecture pattern used in ViewModels
- **Haram**: Islamic term for forbidden content
- **Dhikr**: Islamic remembrance of Allah through repeated phrases
- **Qibla**: Direction of prayer in Islam, towards Mecca

---

## Key File References

### Core Architecture
- `HaramBlurAccessibilityService.kt` - Main accessibility service
- `ContentDetectionEngine.kt` - ML detection pipeline
- `BlurOverlayManager.kt` - Overlay rendering system
- `SettingsRepository.kt` - App configuration management

### Database Layer
- `SiteBlockingDatabase.kt` - Room database (version 7)
- Database entities in `data/database/` package
- DAOs for data access abstraction

### Dependency Injection
- `DataModule.kt` - Core data dependencies
- `EnhancedDetectionModule.kt` - ML and detection dependencies
- `NetworkModule.kt` - API and network dependencies

### UI Layer
- `MainActivity.kt` - Compose navigation entry point
- UI screens in `ui/` package
- Components in `ui/components/` package

### ML Assets
- `app/src/main/assets/models/nsfw_mobilenet_v2_140_224.1.tflite` - NSFW detection
- `app/src/main/assets/models/model_lite_gender_q.tflite` - Gender classification

---

*Last updated: Generated from repository analysis*