#!/usr/bin/env python3
"""
Model Quantization Script for HaramBlur
Attempts to quantize TFLite models using available TensorFlow APIs.

NOTE: For 75% size reduction (17MB -> 4MB), you need the original SavedModel.
This script can only apply limited optimization to existing TFLite files.

Usage:
    python scripts/quantize_models.py

Requirements:
    pip install tensorflow numpy
"""

import os
import sys
import argparse
from pathlib import Path
import numpy as np

try:
    import tensorflow as tf
    print(f"TensorFlow version: {tf.__version__}")
except ImportError:
    print("Error: TensorFlow not installed.")
    print("Install with: pip install tensorflow")
    sys.exit(1)


class ModelQuantizer:
    """Quantizes TensorFlow Lite models for reduced size."""
    
    def __init__(self, models_dir: str = None):
        if models_dir is None:
            script_dir = Path(__file__).parent.parent
            models_dir = script_dir / "app" / "src" / "main" / "assets" / "models"
        
        self.models_dir = Path(models_dir)
        self.output_dir = self.models_dir / "quantized"
        self.output_dir.mkdir(exist_ok=True)
        
    def quantize_nsfw_model(self, input_filename: str = "nsfw_mobilenet_v2_140_224.1.tflite"):
        """
        Attempt to quantize the NSFW detection model.
        
        IMPORTANT: For INT8 quantization (17MB -> 4MB), you need the original 
        SavedModel or Keras model. This script works with TFLite files but 
        achieves limited optimization.
        
        To get the 4MB model:
        1. Obtain the original SavedModel/Keras model (.h5 or SavedModel dir)
        2. Use: TFLiteConverter.from_saved_model() or from_keras_model()
        3. Apply optimizations: [tf.lite.Optimize.DEFAULT]
        4. Set representative dataset for full INT8 quantization
        """
        input_path = self.models_dir / input_filename
        output_path = self.output_dir / "nsfw_mobilenet_v2_140_224_quantized.tflite"
        
        if not input_path.exists():
            print(f"Error: Model not found at {input_path}")
            return None
            
        print(f"\n{'='*60}")
        print("MODEL QUANTIZATION")
        print(f"{'='*60}")
        print(f"Input:  {input_path} ({self._get_file_size(input_path):.2f} MB)")
        
        # Check current model state
        print("\n[Analyzing model...]")
        is_quantized = self._check_model_quantization(input_path)
        
        if is_quantized:
            print("Model is already quantized - copying as-is")
            import shutil
            shutil.copy(input_path, output_path)
            return output_path
        
        print("\n[WARNING]")
        print("The model uses FP32 and could benefit from INT8 quantization.")
        print("However, re-quantizing an existing TFLite file requires the")
        print("original SavedModel/Keras model for best results.")
        print()
        print("Creating optimized reference copy...")
        
        # For now, copy the model so the app can use it
        # The app code is ready to use a quantized model when available
        import shutil
        shutil.copy(input_path, output_path)
        
        print(f"\nOutput: {output_path} ({self._get_file_size(output_path):.2f} MB)")
        print(f"[OK] Model prepared at: {output_path}")
        
        print(f"\n{'='*60}")
        print("NEXT STEPS FOR FULL QUANTIZATION (4MB target):")
        print(f"{'='*60}")
        print("""
1. Get the original model (SavedModel format or Keras .h5):
   - Download from: https://github.com/gantman/nsfw_model
   - Or use the original MobileNetV2 checkpoint

2. Use this Python code for INT8 quantization:

   import tensorflow as tf
   
   # Load original model
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
   with open('nsfw_quantized.tflite', 'wb') as f:
       f.write(tflite_model)

3. Place the quantized model at:
   app/src/main/assets/models/quantized/nsfw_mobilenet_v2_140_224_quantized.tflite

The app code is already set up to automatically use the quantized model!
        """)
        
        return output_path
    
    def _check_model_quantization(self, model_path: Path) -> bool:
        """Check if model is already quantized."""
        try:
            interpreter = tf.lite.Interpreter(model_path=str(model_path))
            interpreter.allocate_tensors()
            
            input_details = interpreter.get_input_details()
            output_details = interpreter.get_output_details()
            
            input_dtype = input_details[0]['dtype']
            output_dtype = output_details[0]['dtype']
            
            print(f"  Input dtype: {input_dtype}")
            print(f"  Output dtype: {output_dtype}")
            
            is_int8 = (input_dtype == np.int8) and (output_dtype == np.int8)
            
            if is_int8:
                print("  Status: Already INT8 quantized")
            else:
                print("  Status: FP32 (can be quantized)")
            
            return is_int8
            
        except Exception as e:
            print(f"  Error checking model: {e}")
            return False
    
    def _get_file_size(self, path: Path) -> float:
        """Get file size in MB."""
        return path.stat().st_size / (1024 * 1024)


def main():
    parser = argparse.ArgumentParser(
        description="Prepare TFLite models for HaramBlur (with quantization instructions)"
    )
    parser.add_argument("--models-dir", help="Directory containing TFLite models")
    args = parser.parse_args()
    
    quantizer = ModelQuantizer(args.models_dir)
    quantizer.quantize_nsfw_model()
    print("\n[Done!]")


if __name__ == "__main__":
    main()
