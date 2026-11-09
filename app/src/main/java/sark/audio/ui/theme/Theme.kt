package sark.audio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import sark.audio.data.NatureTheme
import sark.audio.data.ThemeMode

// ==========================================
// 1. DEFAULT EXPRESSIVE PRO
// ==========================================
val ExpressiveDarkColorScheme = darkColorScheme(
    primary = ExpressivePrimary,
    onPrimary = ExpressiveOnPrimary,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = ExpressiveOnSecondary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveOnTertiary,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = ExpressiveDarkBackground,
    onBackground = ExpressiveDarkOnBackground,
    surface = ExpressiveDarkSurface,
    onSurface = ExpressiveDarkOnSurface,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = ExpressiveDarkOnSurfaceVariant,
    outline = ExpressiveDarkOutline,
    error = ExpressiveError,
    onError = ExpressiveOnError,
    errorContainer = ExpressiveErrorContainer,
    onErrorContainer = ExpressiveOnErrorContainer
)

val ExpressiveLightColorScheme = lightColorScheme(
    primary = ExpressivePrimary,
    onPrimary = ExpressiveOnPrimary,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = ExpressiveOnSecondary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveOnTertiary,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = ExpressiveLightBackground,
    onBackground = ExpressiveLightOnBackground,
    surface = ExpressiveLightSurface,
    onSurface = ExpressiveLightOnSurface,
    surfaceVariant = ExpressiveLightSurfaceVariant,
    onSurfaceVariant = ExpressiveLightOnSurfaceVariant,
    outline = ExpressiveLightOutline,
    error = ExpressiveError,
    onError = ExpressiveOnError,
    errorContainer = ExpressiveErrorContainer,
    onErrorContainer = ExpressiveOnErrorContainer
)

// ==========================================
// 2. FOREST EMERALD
// ==========================================
val ForestDarkColorScheme = darkColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    tertiaryContainer = ForestTertiaryContainer,
    onTertiaryContainer = ForestOnTertiaryContainer,
    background = ForestDarkBackground,
    onBackground = ForestDarkOnBackground,
    surface = ForestDarkSurface,
    onSurface = ForestDarkOnSurface,
    surfaceVariant = ForestDarkSurfaceVariant,
    onSurfaceVariant = ForestDarkOnSurfaceVariant,
    outline = ForestDarkOutline
)

val ForestLightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    tertiaryContainer = ForestTertiaryContainer,
    onTertiaryContainer = ForestOnTertiaryContainer,
    background = ForestLightBackground,
    onBackground = ForestLightOnBackground,
    surface = ForestLightSurface,
    onSurface = ForestLightOnSurface,
    surfaceVariant = ForestLightSurfaceVariant,
    onSurfaceVariant = ForestLightOnSurfaceVariant,
    outline = ForestLightOutline
)

// ==========================================
// 3. OCEAN BREEZE
// ==========================================
val OceanDarkColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    tertiaryContainer = OceanTertiaryContainer,
    onTertiaryContainer = OceanOnTertiaryContainer,
    background = OceanDarkBackground,
    onBackground = OceanDarkOnBackground,
    surface = OceanDarkSurface,
    onSurface = OceanDarkOnSurface,
    surfaceVariant = OceanDarkSurfaceVariant,
    onSurfaceVariant = OceanDarkOnSurfaceVariant,
    outline = OceanDarkOutline
)

val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    tertiaryContainer = OceanTertiaryContainer,
    onTertiaryContainer = OceanOnTertiaryContainer,
    background = OceanLightBackground,
    onBackground = OceanLightOnBackground,
    surface = OceanLightSurface,
    onSurface = OceanLightOnSurface,
    surfaceVariant = OceanLightSurfaceVariant,
    onSurfaceVariant = OceanLightOnSurfaceVariant,
    outline = OceanLightOutline
)

// ==========================================
// 4. SUNSET AMBER
// ==========================================
val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    tertiaryContainer = SunsetTertiaryContainer,
    onTertiaryContainer = SunsetOnTertiaryContainer,
    background = SunsetDarkBackground,
    onBackground = SunsetDarkOnBackground,
    surface = SunsetDarkSurface,
    onSurface = SunsetDarkOnSurface,
    surfaceVariant = SunsetDarkSurfaceVariant,
    onSurfaceVariant = SunsetDarkOnSurfaceVariant,
    outline = SunsetDarkOutline
)

val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    tertiaryContainer = SunsetTertiaryContainer,
    onTertiaryContainer = SunsetOnTertiaryContainer,
    background = SunsetLightBackground,
    onBackground = SunsetLightOnBackground,
    surface = SunsetLightSurface,
    onSurface = SunsetLightOnSurface,
    surfaceVariant = SunsetLightSurfaceVariant,
    onSurfaceVariant = SunsetLightOnSurfaceVariant,
    outline = SunsetLightOutline
)

// ==========================================
// 5. CHERRY BLOSSOM
// ==========================================
val BlossomDarkColorScheme = darkColorScheme(
    primary = BlossomPrimary,
    onPrimary = BlossomOnPrimary,
    primaryContainer = BlossomPrimaryContainer,
    onPrimaryContainer = BlossomOnPrimaryContainer,
    secondary = BlossomSecondary,
    onSecondary = BlossomOnSecondary,
    secondaryContainer = BlossomSecondaryContainer,
    onSecondaryContainer = BlossomOnSecondaryContainer,
    tertiary = BlossomTertiary,
    onTertiary = BlossomOnTertiary,
    tertiaryContainer = BlossomTertiaryContainer,
    onTertiaryContainer = BlossomOnTertiaryContainer,
    background = BlossomDarkBackground,
    onBackground = BlossomDarkOnBackground,
    surface = BlossomDarkSurface,
    onSurface = BlossomDarkOnSurface,
    surfaceVariant = BlossomDarkSurfaceVariant,
    onSurfaceVariant = BlossomDarkOnSurfaceVariant,
    outline = BlossomDarkOutline
)

val BlossomLightColorScheme = lightColorScheme(
    primary = BlossomPrimary,
    onPrimary = BlossomOnPrimary,
    primaryContainer = BlossomPrimaryContainer,
    onPrimaryContainer = BlossomOnPrimaryContainer,
    secondary = BlossomSecondary,
    onSecondary = BlossomOnSecondary,
    secondaryContainer = BlossomSecondaryContainer,
    onSecondaryContainer = BlossomOnSecondaryContainer,
    tertiary = BlossomTertiary,
    onTertiary = BlossomOnTertiary,
    tertiaryContainer = BlossomTertiaryContainer,
    onTertiaryContainer = BlossomOnTertiaryContainer,
    background = BlossomLightBackground,
    onBackground = BlossomLightOnBackground,
    surface = BlossomLightSurface,
    onSurface = BlossomLightOnSurface,
    surfaceVariant = BlossomLightSurfaceVariant,
    onSurfaceVariant = BlossomLightOnSurfaceVariant,
    outline = BlossomLightOutline
)

// ==========================================
// 6. ARCTIC FROST
// ==========================================
val ArcticDarkColorScheme = darkColorScheme(
    primary = ArcticPrimary,
    onPrimary = ArcticOnPrimary,
    primaryContainer = ArcticPrimaryContainer,
    onPrimaryContainer = ArcticOnPrimaryContainer,
    secondary = ArcticSecondary,
    onSecondary = ArcticOnSecondary,
    secondaryContainer = ArcticSecondaryContainer,
    onSecondaryContainer = ArcticOnSecondaryContainer,
    tertiary = ArcticTertiary,
    onTertiary = ArcticOnTertiary,
    tertiaryContainer = ArcticTertiaryContainer,
    onTertiaryContainer = ArcticOnTertiaryContainer,
    background = ArcticDarkBackground,
    onBackground = ArcticDarkOnBackground,
    surface = ArcticDarkSurface,
    onSurface = ArcticDarkOnSurface,
    surfaceVariant = ArcticDarkSurfaceVariant,
    onSurfaceVariant = ArcticDarkOnSurfaceVariant,
    outline = ArcticDarkOutline
)

val ArcticLightColorScheme = lightColorScheme(
    primary = ArcticPrimary,
    onPrimary = ArcticOnPrimary,
    primaryContainer = ArcticPrimaryContainer,
    onPrimaryContainer = ArcticOnPrimaryContainer,
    secondary = ArcticSecondary,
    onSecondary = ArcticOnSecondary,
    secondaryContainer = ArcticSecondaryContainer,
    onSecondaryContainer = ArcticOnSecondaryContainer,
    tertiary = ArcticTertiary,
    onTertiary = ArcticOnTertiary,
    tertiaryContainer = ArcticTertiaryContainer,
    onTertiaryContainer = ArcticOnTertiaryContainer,
    background = ArcticLightBackground,
    onBackground = ArcticLightOnBackground,
    surface = ArcticLightSurface,
    onSurface = ArcticLightOnSurface,
    surfaceVariant = ArcticLightSurfaceVariant,
    onSurfaceVariant = ArcticLightOnSurfaceVariant,
    outline = ArcticLightOutline
)

/**
 * Returns matching Material 3 ColorScheme based on ThemeMode and NatureTheme.
 */
fun getNatureColorScheme(
    natureTheme: NatureTheme,
    isDark: Boolean
): ColorScheme {
    return when (natureTheme) {
        NatureTheme.DEFAULT -> if (isDark) ExpressiveDarkColorScheme else ExpressiveLightColorScheme
        NatureTheme.FOREST_EMERALD -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
        NatureTheme.OCEAN_BREEZE -> if (isDark) OceanDarkColorScheme else OceanLightColorScheme
        NatureTheme.SUNSET_AMBER -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
        NatureTheme.CHERRY_BLOSSOM -> if (isDark) BlossomDarkColorScheme else BlossomLightColorScheme
        NatureTheme.ARCTIC_FROST -> if (isDark) ArcticDarkColorScheme else ArcticLightColorScheme
    }
}

/**
 * Material 3 Expressive Theme supporting Nature Inspiration themes, Light/Dark modes,
 * and dynamic Android 12+ wallpaper colors.
 */
@Composable
fun SarkAudioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    natureTheme: NatureTheme = NatureTheme.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getNatureColorScheme(natureTheme, isDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
