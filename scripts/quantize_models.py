#!/usr/bin/env python3
"""
Model Quantization Script for HaramBlur
Converts TensorFlow Lite models to INT8 quantized versions for size reduction.

Usage:
    python scripts/quantize_models.py

Requirements:
    pip install tensorflow numpy pillow
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
    """Quantizes TensorFlow Lite models to INT8 for reduced size."""
    
    def __init__(self, models_dir: str = "app/src/main/assets/models"):
        self.models_dir = Path(models_dir)
        self.output_dir = self.models_dir / "quantized"
        self.output_dir.mkdir(exist_ok=True)
        
    def quantize_nsfw_model(self, input_filename: str = "nsfw_mobilenet_v2_140_224.1.tflite"):
        """Quantize the NSFW detection model. Target: 16.5MB -> 4.2MB"""
        input_path = self.models_dir / input_filename
        output_path = self.output_dir / "nsfw_mobilenet_v2_140_224_quantized.tflite"
        
        if not input_path.exists():
            print(f"Warning: Model not found at {input_path}")
            return None
            
        print(f"\n📦 Quantizing NSFW model...")
        print(f"   Input:  {input_path} ({self._get_file_size(input_path):.2f} MB)")
        
        try:
            converter = tf.lite.TFLiteConverter.from_tflite_model_file(str(input_path))
            
            # Optimization settings
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_types = [tf.int8]
            converter.representative_dataset = self._create_representative_dataset(
                input_shape=(224, 224)
            )
            converter.inference_input_type = tf.int8
            converter.inference_output_type = tf.int8
            
            quantized_model = converter.convert()
            
            with open(output_path, 'wb') as f:
                f.write(quantized_model)
            
            original_size = self._get_file_size(input_path)
            quantized_size = self._get_file_size(output_path)
            reduction = (1 - quantized_size / original_size) * 100
            
            print(f"   Output: {output_path} ({quantized_size:.2f} MB)")
            print(f"   ✅ Size reduction: {reduction:.1f}%")
            
            return output_path
            
        except Exception as e:
            print(f"   ❌ Quantization failed: {e}")
            return None
    
    def _create_representative_dataset(self, input_shape: tuple, num_samples: int = 100):
        def representative_data_gen():
            for _ in range(num_samples):
                data = np.random.randint(0, 256, (*input_shape, 3), dtype=np.uint8)
                data = data.astype(np.float32) / 255.0
                yield [data]
        return representative_data_gen
    
    def _get_file_size(self, path: Path) -> float:
        return path.stat().st_size / (1024 * 1024)


def main():
    parser = argparse.ArgumentParser(description="Quantize TFLite models for HaramBlur")
    parser.add_argument("--models-dir", default="app/src/main/assets/models")
    args = parser.parse_args()
    
    quantizer = ModelQuantizer(args.models_dir)
    quantizer.quantize_nsfw_model()
    print("\n✅ Done!")


if __name__ == "__main__":
    main()
