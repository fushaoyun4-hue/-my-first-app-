# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# ONNX Runtime
-keep class com.microsoft.onnxruntime.** { *; }

# Kotlin + Compose
-keep class kotlin.** { *; }
-keep class androidx.compose.** { *; }

# 保护模型文件不被打包时混淆
-keep class com.watermark.inference.** { *; }
