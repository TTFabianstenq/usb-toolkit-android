# Keep USB and storage related classes
-keep class android.hardware.usb.** { *; }
-keep class android.os.storage.** { *; }
-keep class androidx.documentfile.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
