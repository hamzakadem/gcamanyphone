# GCam Compatible Camera ProGuard / R8 Rules

# Keep CameraX internal classes and native methods
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep Kotlin coroutines reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep data models
-keepclassmembers class com.gcam.compatible.camera.data.** { *; }
