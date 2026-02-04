# Model Quantization Scripts

## Overview

These scripts help prepare the ML models for the HaramBlur app. The goal is to reduce the NSFW model from 17MB to 4MB (75% reduction) using INT8 quantization.

## Current Status

| Model | Original Size | Quantized Size | Status |
|-------|--------------|----------------|--------|
| NSFW MobileNetV2 | 16.55 MB | 16.55 MB* | Pending full quantization |
| Gender | 0.64 MB | 0.64 MB | Already optimized |

*The model file is prepared but requires the original SavedModel for full INT8 quantization.

## Prerequisites

```bash
pip install tensorflow numpy
```

## Usage

### Check Model Status
```bash
python scripts/check_model.py
```

### Prepare/Quantize Models
```bash
python scripts/quantize_models.py
```

## Full INT8 Quantization (Target: 4MB)

To achieve the 75% size reduction (16.5MB → 4MB), you need the original SavedModel or Keras model:

### Option 1: Using Original NSFW Model

1. Download the original model from https://github.com/gantman/nsfw_model
2. Run the quantization code:

```python
import tensorflow as tf
import numpy as np

# Load original SavedModel
converter = tf.lite.TFLiteConverter.from_saved_model('nsfw_saved_model')

# Enable optimizations
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# INT8 quantization requires representative dataset
def representative_dataset():
    for _ in range(100):
        data = np.random.rand(1, 224, 224, 3).astype(np.float32)
        yield [data]

converter.representative_dataset = representative_dataset
converter.target_spec.supported_types = [tf.int8]
converter.inference_input_type = tf.int8
converter.inference_output_type = tf.int8

# Convert
tflite_model = converter.convert()

# Save
with open('nsfw_mobilenet_v2_140_224_quantized.tflite', 'wb') as f:
    f.write(tflite_model)

print(f"Model size: {len(tflite_model) / 1024 / 1024:.2f} MB")
```

3. Copy the quantized model:
```bash
cp nsfw_mobilenet_v2_140_224_quantized.tflite \
   app/src/main/assets/models/quantized/
```

### Option 2: Using TensorFlow Model Optimization Toolkit

```bash
pip install tensorflow-model-optimization
```

```python
import tensorflow as tf
import tensorflow_model_optimization as tfmot

# Quantization-aware training or post-training quantization
quantize_model = tfmot.quantization.keras.quantize_model

# Apply quantization
q_aware_model = quantize_model(model)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(q_aware_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
quantized_tflite_model = converter.convert()
```

## App Integration

The app code is already configured to:

1. **Automatically detect** the quantized model
2. **Fall back** to the full model if quantized version not found
3. **Report** which model is being used in diagnostics

See `MLModelManager.kt`:
```kotlin
private const val NSFW_MODEL_QUANTIZED = "models/quantized/nsfw_mobilenet_v2_140_224_quantized.tflite"
private const val NSFW_MODEL_FALLBACK = "models/nsfw_mobilenet_v2_140_224.1.tflite"

// Tries quantized model first, falls back automatically
val modelBuffer = try {
    FileUtil.loadMappedFile(context, NSFW_MODEL_QUANTIZED)
} catch (e: IOException) {
    FileUtil.loadMappedFile(context, NSFW_MODEL_FALLBACK)
}
```

## Expected Results

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Model Size | 16.55 MB | ~4.2 MB | -75% |
| Inference Speed | ~200ms | ~60ms | -70% |
| Accuracy | Baseline | ~95% | Minimal loss |

## Notes

- The current TFLite model is FP32 (not yet quantized)
- Re-quantizing an existing TFLite file is limited; original SavedModel is preferred
- The app will work fine with the current model (16.55 MB)
- Quantization is a nice-to-have optimization, not critical for functionality
