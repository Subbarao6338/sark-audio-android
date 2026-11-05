package sark.audio.ui.theme

import androidx.compose.ui.graphics.Color

// Expressive Core Palette Tokens
val ExpressivePrimary = Color(0xFF6366F1)
val ExpressiveOnPrimary = Color(0xFFFFFFFF)
val ExpressivePrimaryContainer = Color(0xFF4338CA)
val ExpressiveOnPrimaryContainer = Color(0xFFE0E7FF)

val ExpressiveSecondary = Color(0xFF14B8A6)
val ExpressiveOnSecondary = Color(0xFFFFFFFF)
val ExpressiveSecondaryContainer = Color(0xFF0F766E)
val ExpressiveOnSecondaryContainer = Color(0xFFCCFBF1)

val ExpressiveTertiary = Color(0xFFF43F5E)
val ExpressiveOnTertiary = Color(0xFFFFFFFF)
val ExpressiveTertiaryContainer = Color(0xFFBE123C)
val ExpressiveOnTertiaryContainer = Color(0xFFFFE4E6)

val ExpressiveError = Color(0xFFEF4444)
val ExpressiveOnError = Color(0xFFFFFFFF)
val ExpressiveErrorContainer = Color(0xFF991B1B)
val ExpressiveOnErrorContainer = Color(0xFFFEE2E2)

// Dark Theme Surfaces & Backgrounds
val ExpressiveDarkBackground = Color(0xFF0F1117)
val ExpressiveDarkOnBackground = Color(0xFFF8FAFC)
val ExpressiveDarkSurface = Color(0xFF1A1D26)
val ExpressiveDarkOnSurface = Color(0xFFF8FAFC)
val ExpressiveDarkSurfaceVariant = Color(0xFF222736)
val ExpressiveDarkOnSurfaceVariant = Color(0xFFCBD5E1)
val ExpressiveDarkOutline = Color(0xFF334155)

// Light Theme Surfaces & Backgrounds
val ExpressiveLightBackground = Color(0xFFF8FAFC)
val ExpressiveLightOnBackground = Color(0xFF0F172A)
val ExpressiveLightSurface = Color(0xFFFFFFFF)
val ExpressiveLightOnSurface = Color(0xFF0F172A)
val ExpressiveLightSurfaceVariant = Color(0xFFF1F5F9)
val ExpressiveLightOnSurfaceVariant = Color(0xFF475569)
val ExpressiveLightOutline = Color(0xFFCBD5E1)

// Backward compatible aliases and custom utility colors
val DarkBackground = ExpressiveDarkBackground
val DarkSurface = ExpressiveDarkSurface
val DarkCard = ExpressiveDarkSurfaceVariant

val PrimaryIndigo = ExpressivePrimary
val PrimaryIndigoLight = Color(0xFF818CF8)
val AccentTeal = ExpressiveSecondary
val AccentCoral = ExpressiveTertiary
val AccentAmber = Color(0xFFF59E0B)

val TextPrimary = ExpressiveDarkOnBackground
val TextSecondary = Color(0xFF94A3B8)
val BorderColor = ExpressiveDarkOutline

val WaveformActive = ExpressivePrimary
val WaveformInactive = ExpressiveDarkOutline
val WaveformMarker = ExpressiveTertiary
