# Biashara360 Android ProGuard Rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.gigtech.biashara360.**$$serializer { *; }
-keepclassmembers class com.gigtech.biashara360.** {
    *** Companion;
}
-keepclasseswithmembers class com.gigtech.biashara360.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor client
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Koin DI
-keep class org.koin.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class com.gigtech.biashara360.db.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep all model classes for serialization
-keep class com.gigtech.biashara360.domain.model.** { *; }

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
