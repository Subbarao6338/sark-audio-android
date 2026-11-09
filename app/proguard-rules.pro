# Sark Audio Aggressive Proguard / R8 Optimization Rules

# Enable aggressive code optimizations and maximum shrinking
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Activity and entry points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Jetpack Compose runtime essentials
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Preserve metadata / models needed for reflection if any
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip debug info / logging where appropriate
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Suppress R8 Kotlin metadata version discrepancy warnings and standard suppressions
-dontwarn kotlin.Metadata
-dontwarn **
