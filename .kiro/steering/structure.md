# HaramBlur Android - Project Structure

## Package Organization

The project follows a modular package structure organized by feature and layer:

```
com.hieltech.haramblur/
├── accessibility/          # Accessibility service core functionality
│   ├── HaramBlurAccessibilityService.kt    # Main accessibility service
│   ├── ScreenCaptureManager.kt             # Screen capture and monitoring
│   └── BlurOverlayManager.kt               # Overlay window management
├── detection/             # Content detection and analysis
│   └── ContentDetectionEngine.kt          # Main detection coordinator
├── ml/                    # Machine learning components
│   ├── MLModelManager.kt                   # TensorFlow Lite model management
│   └── FaceDetectionManager.kt             # ML Kit face detection
├── data/                  # Data layer and repositories
│   ├── AppSettings.kt                      # Settings data classes
│   └── SettingsRepository.kt               # Settings persistence
├── di/                    # Dependency injection modules
│   └── DataModule.kt                       # Hilt DI configuration
├── ui/                    # User interface components
│   ├── MainScreen.kt                       # Main app screen
│   ├── MainViewModel.kt                    # Main screen view model
│   ├── SettingsScreen.kt                   # Settings screen
│   ├── SettingsViewModel.kt                # Settings view model
│   └── theme/                              # UI theming
│       ├── HaramBlurTheme.kt               # Material 3 theme
│       └── Typography.kt                   # Typography definitions
├── utils/                 # Utility classes (currently empty)
├── HaramBlurApplication.kt                 # Application class with Hilt
└── MainActivity.kt                         # Main activity with Compose navigation
```

## Architecture Layers

### Presentation Layer (`ui/`)
- Jetpack Compose screens and components
- ViewModels following MVVM pattern
- Navigation using Navigation Compose
- Material 3 theming and design system

### Domain/Business Layer (`accessibility/`, `detection/`, `ml/`)
- Core business logic for content detection
- Accessibility service implementation
- ML model management and inference
- Screen capture and overlay management

### Data Layer (`data/`, `di/`)
- Repository pattern for data access
- Settings persistence using SharedPreferences
- Dependency injection configuration with Hilt

## Key Architectural Patterns

### Dependency Injection
- All classes use `@Inject` constructor injection
- Services and repositories are `@Singleton` scoped
- Activities/screens use `@AndroidEntryPoint`

### Coroutines Usage
- All async operations use Kotlin Coroutines
- Service uses `CoroutineScope` with `SupervisorJob`
- UI operations on `Dispatchers.Main`
- Heavy processing on `Dispatchers.Default`

### Repository Pattern
- `SettingsRepository` abstracts settings persistence
- Provides clean API for accessing user preferences
- Handles data transformation and caching

## Resource Organization

### XML Resources (`res/`)
- `xml/accessibility_service_config.xml` - Accessibility service configuration
- `values/strings.xml` - Localized strings
- `values/themes.xml` - Material theme definitions
- `drawable/` - App icons and graphics

### Manifest Configuration
- Accessibility service declaration with proper permissions
- Required permissions: SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, WAKE_LOCK
- Service exported as false for security

## Naming Conventions

- **Classes**: PascalCase (e.g., `ContentDetectionEngine`)
- **Functions**: camelCase (e.g., `analyzeContent`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `NOTIFICATION_ID`)
- **Packages**: lowercase with dots (e.g., `com.hieltech.haramblur.accessibility`)
- **Resources**: snake_case (e.g., `accessibility_service_config.xml`)