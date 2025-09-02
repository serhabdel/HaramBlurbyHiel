# HaramBlur - Build & Development Guide

## Build Commands (No Daemon)
- **Debug Build**: `./gradlew --no-daemon assembleDebug`
- **Release Build**: `./gradlew --no-daemon assembleRelease`
- **Clean Build**: `./gradlew --no-daemon clean assembleDebug`
- **Install Debug**: `./gradlew --no-daemon installDebug`

## Test Commands
- **Unit Tests**: `./gradlew --no-daemon testDebugUnitTest`
- **Single Test Class**: `./gradlew --no-daemon testDebugUnitTest --tests "*.ClassName"`
- **Single Test Method**: `./gradlew --no-daemon testDebugUnitTest --tests "*.ClassName.methodName"`
- **Instrumentation Tests**: `./gradlew --no-daemon connectedDebugAndroidTest`

## Code Quality
- **Lint**: `./gradlew --no-daemon lintDebug`
- **Type Check**: `./gradlew --no-daemon compileDebugKotlin`

## Tech Stack
- **Language**: Kotlin (JVM 17)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Hilt (DI)
- **Database**: Room
- **ML**: TensorFlow Lite
- **Coroutines**: For async operations

## Code Style
- **Package**: `com.hieltech.haramblur.feature.subfeature`
- **Classes**: PascalCase (`ContentDetectionEngine`)
- **Functions**: camelCase (`analyzeContent()`)
- **Constants**: UPPER_SNAKE_CASE (`TAG = "ClassName"`)
- **DI**: Use Hilt with `@AndroidEntryPoint`/`@HiltViewModel`
- **Logging**: `Log.d(TAG, "message")` with companion object TAG
- **Error Handling**: Try-catch with specific exception types