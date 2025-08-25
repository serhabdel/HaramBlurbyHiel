# HaramBlur Android - Technical Stack

## Build System & Configuration

- **Build System**: Gradle with Kotlin DSL
- **Language**: Kotlin (JVM target 11)
- **Min SDK**: 25 (Android 7.1)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36

## Core Technologies

### Architecture & Framework
- **Architecture Pattern**: MVVM + Repository Pattern
- **Dependency Injection**: Dagger Hilt
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Navigation Compose
- **Async Processing**: Kotlin Coroutines

### Machine Learning & Computer Vision
- **ML Framework**: TensorFlow Lite (v2.14.0) with GPU acceleration
- **Face Detection**: Google ML Kit Face Detection
- **Image Processing**: Custom blur effects and overlay rendering
- **Model Management**: Local TensorFlow Lite models with quantization

### Data & Storage
- **Database**: Room + SQLite
- **Preferences**: SharedPreferences via SettingsRepository
- **Architecture Components**: LiveData, ViewModel

### Android Services
- **Core Service**: AccessibilityService for system-wide content access
- **Permissions**: SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, WAKE_LOCK
- **Screen Capture**: Custom ScreenCaptureManager with bitmap processing
- **Overlay System**: BlurOverlayManager with TYPE_ACCESSIBILITY_OVERLAY

## Common Commands

### Build & Development
```bash
# Clean and build
./gradlew clean build

# Install debug build
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate APK
./gradlew assembleDebug
./gradlew assembleRelease
```

### Code Quality
```bash
# Lint check
./gradlew lint

# Check dependencies
./gradlew dependencies
```

## Performance Requirements

- **RAM Usage**: <100MB additional memory
- **CPU Usage**: <5% idle, <15% active detection
- **Battery Drain**: <3% per hour
- **Detection Latency**: <200ms
- **Blur Rendering**: 30+ FPS

## Key Dependencies

- Jetpack Compose BOM 2024.02.00
- TensorFlow Lite 2.14.0 with GPU support
- Dagger Hilt 2.48.1
- Room 2.6.1
- ML Kit Face Detection 16.1.5
- Coroutines 1.7.3