# Sark Audio Proguard Rules

# Keep Jetpack Compose runtime and UI essentials
-keep class androidx.compose.** { *; }

# Keep App components
-keep class sark.audio.** { *; }

# Keep data models / serializable classes if needed
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Standard Android optimizations
-dontwarn **
