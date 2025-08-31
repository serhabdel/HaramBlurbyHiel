##instruction
always build without daemon because it crashes my pc with limited memory !
don't run multiple instances for build, ensure ending the prev before starting a new !
## Build Commands

• Debug Build: ./gradlew assembleDebug
• Release Build: ./gradlew assembleRelease
• Clean Build: ./gradlew clean assembleDebug
• Install Debug: ./gradlew installDebug

## Test Commands

• Unit Tests: ./gradlew testDebugUnitTest
• Instrumentation Tests: ./gradlew connectedDebugAndroidTest
• Single Test Class: ./gradlew testDebugUnitTest --tests "*.ClassName"
• Single Test Method: ./gradlew testDebugUnitTest --tests "*.ClassName.methodName"

## Code Quality Commands

• Lint: ./gradlew lintDebug
• Type Check: ./gradlew compileDebugKotlin
• Static Analysis: ./gradlew detekt

## Code Style Guidelines

### Package Structure

com.hieltech.haramblur/
├── accessibility/     # Accessibility services
├── data/             # Data layer (repositories, models, database)
├── detection/        # Content detection engines
├── di/               # Dependency injection modules
├── ml/               # Machine learning components
├── services/         # Background services
├── ui/               # User interface (screens, components, themes)
└── utils/            # Utility classes

### Naming Conventions

• Packages: lowercase, hierarchical (com.hieltech.haramblur.feature.subfeature)
• Classes/Interfaces: PascalCase (ContentDetectionEngine, SettingsRepository)
• Functions/Methods: camelCase (analyzeContent(), getCurrentSettings())
• Variables/Properties: camelCase (detectionSensitivity, blurIntensity)
• Constants: UPPER_SNAKE_CASE (TAG = "ClassName", MAX_RETRY_COUNT = 3)
• Enums: PascalCase for type and values (BlurIntensity.STRONG)

### Import Organization

// Android framework imports
import android.content.Context
import android.util.Log

// Third-party libraries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow

// Local imports (grouped by package)
import com.hieltech.haramblur.data.AppSettings
import com.hieltech.haramblur.detection.ContentDetectionEngine

### Class Structure

@AndroidEntryPoint  // or @HiltViewModel, etc.
class ClassName @Inject constructor(
    private val dependency1: Dependency1,
    private val dependency2: Dependency2
) {
    companion object {
        private const val TAG = "ClassName"
        private const val CONSTANT_VALUE = "value"
    }

    // Public properties
    val publicProperty: Type

    // Private properties
    private var privateProperty: Type? = null

    // Lifecycle methods
    override fun onCreate() { /* ... */ }

    // Public methods
    fun publicMethod(): ReturnType { /* ... */ }

    // Private methods
    private fun privateMethod() { /* ... */ }
}

### Data Classes

/**
 * Comprehensive documentation for data classes
 * @property property1 Description of property1
 * @property property2 Description of property2
 */
data class DataClassName(
    val property1: Type1,
    val property2: Type2 = defaultValue,
    val property3: Type3? = null
) {
    // Companion object for factory methods or constants
    companion object {
        const val DEFAULT_VALUE = "default"
    }
}

### Error Handling

try {
    // Operation that might fail
    val result = riskyOperation()
    Log.d(TAG, "Operation successful: $result")
} catch (e: SpecificException) {
    Log.e(TAG, "Specific error occurred", e)
    handleSpecificError(e)
} catch (e: Exception) {
    Log.e(TAG, "Unexpected error", e)
    handleGenericError(e)
}

### Dependency Injection

// Module definition
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context)
}

// Usage in classes
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: SettingsRepository
}

### Coroutines Usage

class ViewModelClass @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state

    fun performAsyncOperation() {
        viewModelScope.launch {
            try {
                _state.value = State.Loading
                val result = withContext(Dispatchers.IO) {
                    // Heavy computation
                    repository.fetchData()
                }
                _state.value = State.Success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in async operation", e)
                _state.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }
}

### Logging Standards

class ClassName {
    companion object {
        private const val TAG = "ClassName"
    }

    fun someMethod() {
        Log.d(TAG, "Method called with param: $param")
        try {
            // method implementation
            Log.i(TAG, "Operation completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in someMethod", e)
        }
    }
}

### Testing Patterns

class ClassNameTest {

    private lateinit var classUnderTest: ClassName
    private lateinit var mockDependency: MockDependency

    @Before
    fun setup() {
        mockDependency = mockk()
        classUnderTest = ClassName(mockDependency)
    }

    @Test
    fun `should handle success case`() = runTest {
        // Given
        coEvery { mockDependency.method() } returns expectedResult

        // When
        val result = classUnderTest.testMethod()

        // Then
        assertEquals(expectedResult, result)
        coVerify { mockDependency.method() }
    }

    @Test
    fun `should handle error case`() = runTest {
        // Given
        coEvery { mockDependency.method() } throws Exception("Test error")

        // When & Then
        assertThrows(Exception::class.java) {
            classUnderTest.testMethod()
        }
    }
}

### Compose UI Patterns

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                is SettingsState.Loading -> CircularProgressIndicator()
                is SettingsState.Success -> SettingsContent(state as SettingsState.Success)
                is SettingsState.Error -> ErrorContent(state as SettingsState.Error)
            }
        }
    }
}

### File Organization

• One class per file (except for small related classes like enums)
• Related extension functions in same file or dedicated extensions file
• Constants in companion objects or dedicated Constants files
• Test files mirror source structure in src/test or src/androidTest
</xai:function_call">The AGENTS.md file has been created successfully. It contains comprehensive guidelines for:

1. Build/Test Commands: All the essential Gradle commands for building, testing, and code quality checks
2. Code Style Guidelines: Detailed conventions covering:
 • Package structure and naming
 • Import organization
 • Class structure patterns
 • Data class documentation
 • Error handling approaches
 • Dependency injection with Hilt
 • Coroutines usage patterns
 • Logging standards
 • Testing patterns with MockK
 • Jetpack Compose UI patterns
 • File organization principles
