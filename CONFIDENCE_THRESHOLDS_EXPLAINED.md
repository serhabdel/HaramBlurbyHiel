# Confidence Thresholds Explained - HaramBlur

**Settings Location**: Detection Settings → Detection Sensitivity

---

## 🎯 **What Are These Thresholds?**

These are the **minimum confidence levels** required for the ML models to trigger a blur. Think of them as "how sure" the model needs to be before taking action.

---

## 👩 **Gender Confidence Threshold**

**Setting**: `genderConfidenceThreshold`  
**Range**: 30% - 80%  
**Default**: 40%  
**UI Location**: Detection Settings → "Gender Confidence Threshold"

### **What It Does**:

This controls how confident the gender classification model needs to be to identify a face as female (and therefore blur it).

### **Example Scenarios**:

#### **Lower Threshold (30%)**:
```
Face Detection Result:
- Gender: FEMALE
- Confidence: 35%

Action: ✅ BLUR (because 35% > 30%)
Result: More aggressive - blurs even when model is less certain
```

#### **Higher Threshold (80%)**:
```
Face Detection Result:
- Gender: FEMALE  
- Confidence: 65%

Action: ❌ NO BLUR (because 65% < 80%)
Result: More conservative - only blurs when model is very certain
```

### **Code Implementation**:

**Location**: `HaramBlurAccessibilityService.kt` lines 1155-1164

```kotlin
val genderThreshold = minOf(adaptiveGenderThreshold, settings.genderConfidenceThreshold)

val hasFemaleFaces = result.faceDetectionResult?.detectedFaces?.any { face ->
    val isConfidentFemale = face.genderConfidence > genderThreshold &&
                           face.estimatedGender.toString().contains("FEMALE", ignoreCase = true)
    
    // BLUR if confidence exceeds threshold
    isConfidentFemale
}
```

### **Practical Effect**:

| Threshold | Effect | When to Use |
|-----------|--------|-------------|
| **30-40%** | Very aggressive - blurs uncertain detections | Maximum protection mode |
| **40-50%** | Balanced - standard detection | Default recommended |
| **50-60%** | Conservative - only clear female faces | Reduce false positives |
| **60-80%** | Very strict - only obvious female faces | Minimize interference |

---

## 🔞 **NSFW Confidence Threshold**

**Setting**: `nsfwConfidenceThreshold`  
**Range**: 40% - 70%  
**Default**: 50%  
**UI Location**: Detection Settings → "NSFW Confidence Threshold"

### **What It Does**:

This controls how confident the NSFW detection model needs to be to identify content as inappropriate.

### **Example Scenarios**:

#### **Lower Threshold (40%)**:
```
NSFW Detection Result:
- Content: Potentially inappropriate
- Confidence: 45%

Action: ✅ BLUR (because 45% > 40%)
Result: More aggressive - blurs borderline content
```

#### **Higher Threshold (70%)**:
```
NSFW Detection Result:
- Content: Potentially inappropriate
- Confidence: 55%

Action: ❌ NO BLUR (because 55% < 70%)
Result: More conservative - only blurs obvious NSFW content
```

### **Code Implementation**:

**Location**: `HaramBlurAccessibilityService.kt` lines 1155, 1196-1207

```kotlin
val nsfwThreshold = minOf(adaptiveNSFWThreshold, settings.nsfwConfidenceThreshold)

val hasNSFWContent = result.nsfwDetectionResult?.let { nsfwResult ->
    val isHighConfidenceNSFW = nsfwResult.isNSFW && nsfwResult.confidence > nsfwThreshold
    val isMediumConfidenceNSFW = nsfwResult.confidence > (nsfwThreshold * 0.7f)
    val isAnyNSFWIndicator = nsfwResult.confidence > 0.2f
    
    // Different blur triggers based on sensitivity
    when {
        isHighConfidenceNSFW -> true  // Always blur
        isMediumConfidenceNSFW && settings.detectionSensitivity > 0.6f -> true
        isAnyNSFWIndicator && settings.detectionSensitivity > 0.8f -> true
        else -> false
    }
}
```

### **Practical Effect**:

| Threshold | Effect | What Gets Blurred |
|-----------|--------|-------------------|
| **40-45%** | Very aggressive | Blur everything remotely suspicious |
| **45-55%** | Balanced | Blur clear inappropriate content |
| **55-65%** | Conservative | Only blur obvious NSFW content |
| **65-70%** | Very strict | Only blur extremely explicit content |

---

## 📊 **How They Work Together**

The detection system uses **BOTH** thresholds:

```
Detection Pipeline:
1. Capture screenshot
2. Run ML models:
   - Face Detection → Gender Classification
   - NSFW Detection
3. Compare results to thresholds:
   - Female face confidence > genderThreshold? → Blur face
   - NSFW confidence > nsfwThreshold? → Blur content
4. Apply blur if EITHER condition is met
```

---

## 🎚️ **Recommended Settings**

### **Maximum Protection** (Aggressive):
```
Gender Threshold: 30%
NSFW Threshold: 40%
Detection Sensitivity: 80%

Result: Blur almost everything remotely questionable
Pro: Maximum safety
Con: More false positives
```

### **Balanced Protection** (Recommended):
```
Gender Threshold: 40%
NSFW Threshold: 50%
Detection Sensitivity: 70%

Result: Blur clear inappropriate content
Pro: Good balance
Con: Might miss some edge cases
```

### **Conservative Protection** (Minimal Interference):
```
Gender Threshold: 60%
NSFW Threshold: 60%
Detection Sensitivity: 50%

Result: Only blur obvious inappropriate content
Pro: Fewer false positives
Con: May miss some content
```

---

## ⚡ **Impact on Performance**

**Lower thresholds** (30-40%):
- ✅ More detections
- ✅ Fewer misses
- ❌ More false positives
- ❌ More frequent blur activation
- ❌ Slightly higher battery usage

**Higher thresholds** (60-80%):
- ✅ Fewer false positives
- ✅ Less intrusive
- ✅ Better battery life
- ❌ May miss some content
- ❌ Less protective

---

## 🔧 **Advanced: Adaptive Thresholds**

The app uses **adaptive learning** to optimize thresholds over time:

**Location**: `HaramBlurAccessibilityService.kt` lines 269-270

```kotlin
private var adaptiveNSFWThreshold = 0.4f  // Starts at 40%
private var adaptiveGenderThreshold = 0.4f  // Starts at 40%
```

**How it works**:
```kotlin
// System uses the LOWER of: adaptive threshold OR user setting
val nsfwThreshold = minOf(adaptiveNSFWThreshold, settings.nsfwConfidenceThreshold)
val genderThreshold = minOf(adaptiveGenderThreshold, settings.genderConfidenceThreshold)
```

This means:
- If adaptive threshold = 35% and user sets 50% → Uses 35% (more aggressive)
- If adaptive threshold = 45% and user sets 40% → Uses 40% (respects user choice)

---

## 🎯 **Real-World Examples**

### **Example 1: Female Face Detection**

**Scenario**: Browsing social media, see a profile picture

**ML Model Output**:
```
Face detected: YES
Gender: FEMALE
Confidence: 55%
```

**With different thresholds**:
- Threshold 30%: ✅ BLUR (55% > 30%)
- Threshold 40%: ✅ BLUR (55% > 40%)
- Threshold 50%: ✅ BLUR (55% > 50%)
- Threshold 60%: ❌ NO BLUR (55% < 60%)
- Threshold 80%: ❌ NO BLUR (55% < 80%)

---

### **Example 2: NSFW Content Detection**

**Scenario**: Browsing, see an image with skin tone

**ML Model Output**:
```
NSFW Content: DETECTED
Confidence: 48%
```

**With different thresholds**:
- Threshold 40%: ✅ BLUR (48% > 40%)
- Threshold 45%: ✅ BLUR (48% > 45%)
- Threshold 50%: ❌ NO BLUR (48% < 50%)
- Threshold 60%: ❌ NO BLUR (48% < 60%)

---

## 💡 **Pro Tips**

### **If you're seeing too many false positives**:
1. Increase both thresholds by 10-15%
2. Example: 40% → 50-55%
3. This makes detection more conservative

### **If blur isn't showing when it should**:
1. Lower both thresholds by 10-15%
2. Example: 50% → 35-40%
3. This makes detection more aggressive

### **Finding your sweet spot**:
1. Start with defaults (40% gender, 50% NSFW)
2. Test for a few days
3. Adjust based on experience:
   - Too many false positives? Increase by 10%
   - Missing content? Decrease by 10%

---

## 📱 **How to Check Current Thresholds**

Use logcat to see what thresholds are being used:

```bash
adb logcat | grep "adaptive thresholds"
```

**Output**:
```
D/HaramBlurAccessibilityService: 🧠 Using adaptive thresholds: NSFW=0.45, Gender=0.40
```

---

## 🎓 **Summary**

**Gender Confidence Threshold**:
- Controls when to blur female faces
- Lower = More aggressive (blur uncertain detections)
- Higher = More conservative (only blur obvious female faces)

**NSFW Confidence Threshold**:
- Controls when to blur inappropriate content
- Lower = More protective (blur borderline content)
- Higher = Less intrusive (only blur obvious NSFW content)

**Both work together** to determine what gets blurred. The ML models provide confidence scores, and these thresholds are the "decision points" for whether to trigger blur.

**Key Takeaway**: Lower thresholds = More protection but more false positives. Higher thresholds = Fewer false positives but may miss some content.
