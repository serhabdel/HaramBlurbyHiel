# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ML Kit Face Detection
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep interface com.google.mlkit.vision.** { *; }
-dontwarn com.google.mlkit.vision.**

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep native library loading methods
-keep class * {
    static { <clinit>(); }
}

# Model loading and inference
-keep class * extends org.tensorflow.lite.Interpreter { *; }
-keep class * implements org.tensorflow.lite.Delegate { *; }

# Reflection-based access
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.hieltech.haramblur.ml.** { *; }
-keep class com.hieltech.haramblur.detection.** { *; }

# Keep all classes referenced by TensorFlow Lite GPU delegate
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-keep class org.tensorflow.lite.gpu.GpuDelegateFactory* { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class **_HiltComponents$SingletonC { *; }

# Keep AccessibilityService classes
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Keep DeviceAdminReceiver classes
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Preserve line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile