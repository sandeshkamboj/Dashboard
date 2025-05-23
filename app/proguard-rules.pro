# Preserve annotations
-keepattributes *Annotation*

# Required for Hilt (dependency injection)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-keep class sun.misc.Unsafe { *; }

# Required for kotlinx.serialization
-keep class kotlinx.serialization.** { *; }

# Required for Supabase SDK (serialization, reflection)
-keep class io.github.jan.** { *; }

# Required for Gson/JSON parsing (if used)
-keep class com.google.gson.** { *; }

# Required for Google Maps and Play Services
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# Required for Coil (image loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Required for FileProvider
-keep public class androidx.core.content.FileProvider { public *; }

# For Jetpack Compose (may not be strictly necessary)
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# For Hilt code generation
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.ComponentSupplier { *; }

# Required to keep MainActivity and App class
-keep class com.remote.dashboard.MainActivity { *; }
-keep class com.remote.dashboard.App { *; }

# Keep model classes (parcelable/serializable)
-keep class com.remote.dashboard.model.** { *; }

# Keep any other entry points or custom classes as needed
# -keep class com.remote.dashboard.** { *; }

# Remove logging (optional, recommended for release)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# General optimizations
-optimizations !code/allocation/variable

# Don't warn about unused stuff in Kotlin/Compose libraries
-dontwarn kotlin.**
-dontwarn kotlinx.**