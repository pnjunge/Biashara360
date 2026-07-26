# Biashara360 Android ProGuard Rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.app.biashara.**$$serializer { *; }
-keepclassmembers class com.app.biashara.** {
    *** Companion;
}
-keepclasseswithmembers class com.app.biashara.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor client
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Koin DI
-keep class org.koin.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class com.app.biashara.db.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep all model classes for serialization
-keep class com.app.biashara.domain.model.** { *; }

# Compile-time annotations and optional JVM logging/debug integrations are not
# present on Android and are safe to omit from the release package.
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
