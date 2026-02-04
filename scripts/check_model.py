#!/usr/bin/env python3
"""Check model quantization status."""
import tensorflow as tf
import numpy as np
from pathlib import Path

model_path = Path('app/src/main/assets/models/nsfw_mobilenet_v2_140_224.1.tflite')
print(f'Model size: {model_path.stat().st_size / 1024 / 1024:.2f} MB')

# Load model
interpreter = tf.lite.Interpreter(model_path=str(model_path))
interpreter.allocate_tensors()

# Get model info
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f'Input dtype: {input_details[0]["dtype"]}')
print(f'Output dtype: {output_details[0]["dtype"]}')
print(f'Input shape: {input_details[0]["shape"]}')
print(f'Output shape: {output_details[0]["shape"]}')

# Check if already quantized
is_int8_input = input_details[0]['dtype'] == np.int8
is_int8_output = output_details[0]['dtype'] == np.int8

if is_int8_input and is_int8_output:
    print('\nModel is already INT8 quantized!')
    print('No further quantization needed.')
else:
    print(f'\nModel uses FP32 - could be quantized further')
    print(f'(INT8 input: {is_int8_input}, INT8 output: {is_int8_output})')
