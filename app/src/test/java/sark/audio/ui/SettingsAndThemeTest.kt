package sark.audio.ui

import org.junit.Assert.*
import org.junit.Test
import sark.audio.data.*
import sark.audio.ui.screens.ToolboxTool
import sark.audio.ui.theme.getNatureColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.ui.graphics.Color

class SettingsAndThemeTest {

    @Test
    fun testAllNatureThemesHaveValidColorSchemes() {
        for (nature in NatureTheme.values()) {
            val darkScheme = getNatureColorScheme(nature, isDark = true)
            val lightScheme = getNatureColorScheme(nature, isDark = false)

            assertNotNull("Dark scheme primary should not be null for $nature", darkScheme.primary)
            assertNotNull("Light scheme primary should not be null for $nature", lightScheme.primary)
            assertNotNull("Dark scheme surface should not be null for $nature", darkScheme.surface)
            assertNotNull("Light scheme surface should not be null for $nature", lightScheme.surface)
            assertNotNull("Dark scheme background should not be null for $nature", darkScheme.background)
            assertNotNull("Light scheme background should not be null for $nature", lightScheme.background)

            // Verify light scheme has light background and dark text, and vice versa
            assertTrue("Dark theme background luminance should be low", darkScheme.background.red < 0.2f && darkScheme.background.green < 0.2f && darkScheme.background.blue < 0.2f)
            assertTrue("Light theme background luminance should be high", lightScheme.background.red > 0.8f && lightScheme.background.green > 0.8f && lightScheme.background.blue > 0.8f)
        }
    }

    @Test
    fun testToolsSortingAlgorithms() {
        val testTools = listOf(
            ToolboxTool("merger", "Audio Merger", "Mixer", "Sequential multi-clip audio track joiner", Icons.Default.GraphicEq, Color.Cyan),
            ToolboxTool("creator", "Audio Creator", "Creator", "Waveform generator", Icons.Default.GraphicEq, Color.Blue),
            ToolboxTool("recorder", "Studio Recorder", "Recorder", "PCM audio capture", Icons.Default.GraphicEq, Color.Red),
            ToolboxTool("autotune", "Autotune Snap", "Vocal Suite", "Pitch quantization", Icons.Default.GraphicEq, Color.Magenta)
        )

        // Default
        assertEquals(listOf("merger", "creator", "recorder", "autotune"), testTools.map { it.id })

        // Name Ascending
        val nameAsc = testTools.sortedBy { it.title.lowercase() }
        assertEquals(listOf("creator", "merger", "autotune", "recorder"), nameAsc.map { it.id })

        // Name Descending
        val nameDesc = testTools.sortedByDescending { it.title.lowercase() }
        assertEquals(listOf("recorder", "autotune", "merger", "creator"), nameDesc.map { it.id })

        // Category
        val byCat = testTools.sortedWith(compareBy({ it.category }, { it.title }))
        assertEquals(listOf("creator", "merger", "recorder", "autotune"), byCat.map { it.id })
    }

    @Test
    fun testAppSettingsDefaultsAndEnums() {
        val defaultSettings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, defaultSettings.themeMode)
        assertEquals(NatureTheme.DEFAULT, defaultSettings.natureTheme)
        assertEquals(DashboardLayout.CARDS, defaultSettings.dashboardLayout)
        assertEquals(ToolsSortOrder.DEFAULT, defaultSettings.toolsSortOrder)
        assertEquals(CornerStyle.ROUNDED, defaultSettings.cornerStyle)
        assertTrue(defaultSettings.showCategoryBadges)
        assertEquals(44100, defaultSettings.defaultSampleRateHz)

        // Verify corner styles
        assertEquals(8, CornerStyle.SUBTLE.cornerRadiusDp)
        assertEquals(16, CornerStyle.ROUNDED.cornerRadiusDp)
        assertEquals(24, CornerStyle.PILL.cornerRadiusDp)

        // Verify nature theme count
        assertEquals(6, NatureTheme.values().size)
    }
}
