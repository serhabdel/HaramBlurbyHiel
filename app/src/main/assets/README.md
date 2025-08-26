# HaramBlur ML Models - Assets Directory

## ✅ **CORE MODELS AVAILABLE**

This directory contains the essential TensorFlow Lite model files for HaramBlur's ML-based detection:

**Available Models:**
- ✅ NSFW Detection: `models/nsfw_mobilenet_v2_140_224.1.tflite`
- ✅ Gender Classification: `models/model_lite_gender_q.tflite`

**Optional Enhanced Models:**
- ❌ Advanced NSFW: `models/enhanced_nsfw.tflite`
- ❌ Fast Detection: `models/fast_detection.tflite`

### Available Model Files

1. **`models/nsfw_mobilenet_v2_140_224.1.tflite`** - Main NSFW content detection model
   - Referenced in: `MLModelManager.kt`
   - Status: ✅ **FOUND** - MobileNetV2 NSFW detection model with GPU acceleration

2. **`models/model_lite_gender_q.tflite`** - Gender classification model
   - Referenced in: `MLModelManager.kt`, `EnhancedDetectionConfig.kt`
   - Status: ✅ **FOUND** - ML-based gender classification with 96x96 input

3. **`models/enhanced_nsfw.tflite`** - Advanced NSFW detection model
   - Referenced in: `EnhancedDetectionConfig.kt`
   - Status: ❌ **MISSING** - Would provide enhanced NSFW detection if available

4. **`models/fast_detection.tflite`** - Optimized fast detection model
   - Referenced in: `EnhancedDetectionConfig.kt`
   - Status: ❌ **MISSING** - Would provide ultra-fast detection if available

### Current Status

- ✅ **Core ML Support** - 2 out of 4 models available
- ✅ **Gender Detection** - ML-based gender classification available
- ✅ **NSFW Detection** - ML-based NSFW detection with MobileNetV2 model
- 🎯 **High Accuracy** - Both core detection systems use ML models

### What You Need To Do

1. **Obtain TensorFlow Lite models** for NSFW detection and gender classification
2. **Place model files** in the correct locations:
   ```
   app/src/main/assets/
   ├── nsfw_model.tflite
   ├── gender_model.tflite
   └── models/
       ├── gender_detection.tflite
       ├── enhanced_nsfw.tflite
       └── fast_detection.tflite
   ```
3. **Update the model loading code** in `MLModelManager.kt` to actually load these models
4. **Test the models** to ensure they work with the current implementation

### Fallback Behavior

Currently, the app uses:
- **Heuristic-based NSFW detection** (skin tone analysis)
- **Facial feature analysis** for gender detection
- **Basic pattern matching** for content classification

This provides basic functionality but with **significantly reduced accuracy** compared to proper ML models.

### Next Steps

1. **Add the missing model files** to this directory
2. **Implement proper model loading** in the initialization methods
3. **Test ML-based detection** vs heuristic detection
4. **Update documentation** with model requirements

---

**Note**: Without these models, the app will still function but with limited detection capabilities. For production use, proper ML models are essential for accurate content filtering.
