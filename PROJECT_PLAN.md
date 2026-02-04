# HaramBlur Project Plan

## Executive Summary

This document outlines the strategic plan for HaramBlur - an Islamic content filtering Android application. The plan balances **performance optimization**, **security hardening**, and **open source readiness**.

---

## Phase 1: Security & Cleanup ✅ COMPLETE

### Immediate Actions (DONE)
- [x] Removed sensitive internal documentation
- [x] Secured signing credentials (environment variables)
- [x] Code modularization (11 components)
- [x] Build verification passing

### Security Checklist
- [x] No hardcoded credentials in repo
- [x] ProGuard/R8 enabled
- [x] KeyStore in .gitignore
- [x] local.properties in .gitignore

---

## Phase 2: Performance Optimization 🎯 PRIORITY

### 2.1 ML Model Optimization

#### Quantization (High Impact)
**Goal**: Reduce model size and improve inference speed

```kotlin
// Current: 17MB model
// Target: < 5MB with INT8 quantization

// In MLModelManager.kt:
private fun createInterpreterOptions(): Interpreter.Options {
    return Interpreter.Options().apply {
        // Add quantization
        addDelegate(GpuDelegate())  // GPU acceleration
        numThreads = 4               // Multi-threading
        useXNNPACK = true            // XNNPACK delegate
    }
}
```

**Tools**:
- TensorFlow Lite Model Optimization Toolkit
- Post-training quantization
- INT8 quantization for 4x speedup

**Expected Results**:
- Model size: 17MB → 4-5MB
- Inference time: 200ms → 50-80ms
- Battery usage: -30%

#### Dynamic Model Loading (Medium Impact)
Load models on-demand rather than at startup:
```kotlin
// Lazy loading pattern
val nsfwModel by lazy { loadNsfwModel() }
val genderModel by lazy { loadGenderModel() }
```

---

### 2.2 Adaptive Screen Capture

#### Smart Capture Intervals
Instead of fixed 2-second intervals:

```kotlin
enum class CaptureFrequency {
    IDLE(5000L),        // 5s when no content changes
    NORMAL(2000L),      // 2s default
    ACTIVE(500L),       // 0.5s when inappropriate content detected
    HIGH_ALERT(200L)    // 0.2s during high-confidence detection
}
```

#### Change Detection
Only analyze when screen content changes:
```kotlin
private var lastBitmapHash: String? = null

fun shouldCapture(bitmap: Bitmap): Boolean {
    val currentHash = computeHash(bitmap)
    if (currentHash == lastBitmapHash) return false
    lastBitmapHash = currentHash
    return true
}
```

**Expected Results**:
- Battery: -40% when idle
- CPU: -50% during normal use
- Responsiveness: Better user experience

---

### 2.3 Region-of-Interest Processing

#### Grid-Based Analysis
Instead of processing entire screen, analyze changed regions:

```kotlin
// Divide screen into 4x4 grid
// Only re-analyze changed grid cells
class GridAnalyzer(private val gridSize: Int = 4) {
    fun getChangedRegions(
        previous: Bitmap,
        current: Bitmap
    ): List<Rect> {
        // Return only changed regions
    }
}
```

**Expected Results**:
- Processing time: -60%
- Battery: -35%

---

## Phase 3: Open Source Readiness 🌐

### 3.1 Public Documentation

#### README.md (Keep It Clean)
```markdown
# HaramBlur

Islamic content filtering for Android. Protects users from inappropriate content through AI-powered detection.

## Features
- Real-time content detection
- Privacy-first (on-device processing)
- Prayer times & Islamic features

## Tech Stack
- Kotlin
- TensorFlow Lite
- Jetpack Compose
- MVVM Architecture

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md)

## License
Islamic Open Source License (IOSL)
```

#### CONTRIBUTING.md
- Code style guidelines
- Pull request process
- Testing requirements
- Community guidelines

#### SECURITY.md
```markdown
## Reporting Security Issues

Please email security@haramblur.app instead of opening public issues.

## Security Measures
- On-device processing only
- No data collection
- Encrypted local storage
```

---

### 3.2 Code Cleanup for Public

#### Remove Internal Comments
Delete:
- Developer notes
- TODOs with internal references
- Debugging code
- Internal URLs/references

#### Add Public-Facing Comments
```kotlin
/**
 * Analyzes screen content for inappropriate material.
 * All processing happens locally on the device.
 * 
 * @param bitmap Screenshot to analyze
 * @return Analysis result with blur recommendations
 */
fun analyzeContent(bitmap: Bitmap): AnalysisResult
```

---

### 3.3 Repository Hygiene

#### Files to Remove/Check
```
❌ Remove:
- Any TODO files
- Internal meeting notes
- Draft designs
- Personal config files

✅ Keep:
- Source code
- Public docs
- Tests
- License
- Clean README
```

---

## Phase 4: Feature Enhancements ✨

### 4.1 User Experience

#### Smart Blur Animations
```kotlin
// Smooth fade-in/fade-out
val blurAnimation = animateFloatAsState(
    targetValue = if (shouldBlur) 1f else 0f,
    animationSpec = tween(300, easing = FastOutSlowInEasing)
)
```

#### Customizable Blur Styles
- Gaussian blur
- Pixelation
- Solid color overlay
- Custom patterns

### 4.2 Enhanced Detection

#### Context-Aware Detection
```kotlin
// Different sensitivity per app type
when (appCategory) {
    BROWSER -> highSensitivity
    SOCIAL_MEDIA -> mediumSensitivity
    MESSAGING -> lowSensitivity
}
```

#### Continuous Learning
```kotlin
// User feedback integration
fun reportFalsePositive(region: Rect) {
    // Adjust thresholds based on feedback
    adaptiveThreshold.adjust(-0.05f)
}
```

---

## Phase 5: Testing & Quality 🧪

### 5.1 Test Strategy

#### Unit Tests (Priority)
```kotlin
// Test new components
- BrowserDetectorTest
- UrlExtractorTest
- MemoryManagerTest
- DetectionProcessorTest
```

#### Integration Tests
- End-to-end detection flow
- Screen capture → Analysis → Blur
- Performance benchmarks

#### Device Testing
- Low-end devices (2GB RAM)
- High-end devices (8GB+ RAM)
- Different Android versions (8-14)

### 5.2 Performance Benchmarks

#### Metrics to Track
```kotlin
data class PerformanceMetrics(
    val inferenceTimeMs: Long,
    val memoryUsageMb: Int,
    val batteryImpactPercent: Float,
    val frameDropRate: Float
)
```

#### Target Benchmarks
| Metric | Current | Target |
|--------|---------|--------|
| Inference time | 200ms | <100ms |
| Memory usage | 150MB | <100MB |
| Battery/hour | 15% | <10% |
| Frame drops | 5% | <2% |

---

## Implementation Timeline

### Month 1: Performance (Critical)
- Week 1-2: ML model quantization
- Week 3: Adaptive capture intervals
- Week 4: Region-of-interest processing

### Month 2: Polish & Open Source
- Week 1-2: Code cleanup
- Week 3: Documentation
- Week 4: Community setup

### Month 3: Testing & Release
- Week 1-2: Testing
- Week 3: Beta release
- Week 4: Public release

---

## Success Metrics

### Performance
- [ ] App launches in <3 seconds
- [ ] Detection latency <100ms
- [ ] Battery usage <10%/hour
- [ ] No ANRs or crashes

### User Experience
- [ ] Blur appears smoothly
- [ ] No false positives >5%
- [ ] App feels responsive
- [ ] User retention >60%

### Open Source
- [ ] Clean, documented codebase
- [ ] Active contributors
- [ ] Regular releases
- [ ] Positive community

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Model quantization reduces accuracy | High | A/B testing, fallback to full model |
| Adaptive capture misses content | Medium | Conservative thresholds, user feedback |
| Performance gains not noticeable | Low | Benchmarks, user surveys |
| Open source exposes vulnerabilities | Medium | Security audit, responsible disclosure |

---

## Resources Needed

### Development
- Android Developer (1 FTE)
- ML Engineer (0.5 FTE, for model optimization)

### Tools
- Firebase Test Lab (device testing)
- GitHub Actions (CI/CD)
- TensorFlow Lite Model Maker

### Community
- Moderators for issues/PRs
- Beta testers (50-100 users)

---

## Conclusion

This plan prioritizes **performance** (user experience) while preparing for **open source** success. The modular architecture we've built makes all these improvements achievable.

**Key Success Factor**: Start with ML model optimization - it has the highest impact on user experience.

---

*Last Updated: 2026-02-03*
*Version: 1.0*
