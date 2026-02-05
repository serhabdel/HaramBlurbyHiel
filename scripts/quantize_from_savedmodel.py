#!/usr/bin/env python3
"""
Quantize NSFW model from SavedModel to INT8 TFLite (17MB -> 4MB)
Uses the original model files provided.
"""

import tensorflow as tf
import numpy as np
from pathlib import Path
import sys

print(f"TensorFlow version: {tf.__version__}")

# Paths
MODEL_DIR = Path("mobilenet_v2_140_224")
SAVED_MODEL_PATH = MODEL_DIR / "saved_model.pb"
OUTPUT_DIR = Path("app/src/main/assets/models/quantized")
OUTPUT_PATH = OUTPUT_DIR / "nsfw_mobilenet_v2_140_224_quantized.tflite"

# Ensure output directory exists
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

print(f"\n{'='*60}")
print("QUANTIZING NSFW MODEL FROM SAVEDMODEL")
print(f"{'='*60}")
print(f"Input: {MODEL_DIR}")
print(f"Output: {OUTPUT_PATH}")

def representative_dataset():
    """Generate representative dataset for INT8 calibration"""
    for _ in range(100):
        # Random data in [0, 1] range, shape [1, 224, 224, 3]
        data = np.random.rand(1, 224, 224, 3).astype(np.float32)
        yield [data]

try:
    print("\n[1/4] Loading SavedModel...")
    
    # Load the saved model
    converter = tf.lite.TFLiteConverter.from_saved_model(str(MODEL_DIR))
    
    print("[2/4] Configuring INT8 quantization...")
    
    # Enable optimizations
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Full integer quantization for maximum size reduction
    converter.representative_dataset = representative_dataset
    converter.target_spec.supported_types = [tf.int8]
    converter.inference_input_type = tf.int8
    converter.inference_output_type = tf.int8
    
    print("[3/4] Converting model (this may take a minute)...")
    
    # Convert
    tflite_model = converter.convert()
    
    print("[4/4] Saving quantized model...")
    
    # Save
    with open(OUTPUT_PATH, 'wb') as f:
        f.write(tflite_model)
    
    # Calculate sizes
    original_size = (MODEL_DIR / "saved_model.tflite").stat().st_size / (1024 * 1024)
    quantized_size = len(tflite_model) / (1024 * 1024)
    reduction = (1 - quantized_size / original_size) * 100
    
    print(f"\n{'='*60}")
    print("QUANTIZATION SUCCESSFUL!")
    print(f"{'='*60}")
    print(f"Original size: {original_size:.2f} MB")
    print(f"Quantized size: {quantized_size:.2f} MB")
    print(f"Reduction: {reduction:.1f}%")
    print(f"\nOutput saved to: {OUTPUT_PATH}")
    print(f"{'='*60}")
    
    # Verify model
    print("\n[Verifying quantized model...]")
    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input shape: {input_details[0]['shape']}")
    print(f"Input dtype: {input_details[0]['dtype']}")
    print(f"Output shape: {output_details[0]['shape']}")
    print(f"Output dtype: {output_details[0]['dtype']}")
    
    if input_details[0]['dtype'] == np.int8:
        print("\n[OK] Model successfully quantized to INT8!")
    else:
        print("\n[WARNING] Model may not be fully INT8 quantized")
    
except Exception as e:
    print(f"\n[ERROR] Quantization failed: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

print("\n[Done!]")
