package sark.audio.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Visual Theme Mode selection.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Nature-inspired themes with distinctive palettes.
 */
enum class NatureTheme(val displayName: String, val description: String) {
    DEFAULT("Expressive Pro", "Original high-contrast indigo studio palette"),
    FOREST_EMERALD("Forest Emerald", "Pine canopy greens, fresh moss & tranquil woods"),
    OCEAN_BREEZE("Ocean Breeze", "Deep marine azure, sea foam teal & coastal breeze"),
    SUNSET_AMBER("Sunset Amber", "Golden hour glow, warm terracotta & dusk coral"),
    CHERRY_BLOSSOM("Cherry Blossom", "Delicate sakura petals, rose quartz & plum warmth"),
    ARCTIC_FROST("Arctic Frost", "Glacial slate, cool arctic cyan & frosty mint")
}

/**
 * Dashboard layout style: Detailed Cards vs Quick Tiles.
 */
enum class DashboardLayout {
    CARDS,
    TILES
}

/**
 * Tools sorting options.
 */
enum class ToolsSortOrder(val displayName: String) {
    DEFAULT("Featured Default"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    CATEGORY("Category")
}

/**
 * Shape roundness personalization for tiles and cards.
 */
enum class CornerStyle(val displayName: String, val cornerRadiusDp: Int) {
    SUBTLE("Subtle (8dp)", 8),
    ROUNDED("Rounded (16dp)", 16),
    PILL("Expressive Soft (24dp)", 24)
}

/**
 * Comprehensive application user settings data class.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val natureTheme: NatureTheme = NatureTheme.DEFAULT,
    val dashboardLayout: DashboardLayout = DashboardLayout.CARDS,
    val toolsSortOrder: ToolsSortOrder = ToolsSortOrder.DEFAULT,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val showCategoryBadges: Boolean = true,
    val dynamicColor: Boolean = false,
    val compactTilesGrid: Boolean = false,
    val defaultSampleRateHz: Int = 44100
)

/**
 * Thread-safe preferences repository for persisting deep customizations.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sark_audio_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val natureThemeStr = prefs.getString(KEY_NATURE_THEME, NatureTheme.DEFAULT.name) ?: NatureTheme.DEFAULT.name
        val layoutStr = prefs.getString(KEY_LAYOUT, DashboardLayout.CARDS.name) ?: DashboardLayout.CARDS.name
        val sortOrderStr = prefs.getString(KEY_SORT_ORDER, ToolsSortOrder.DEFAULT.name) ?: ToolsSortOrder.DEFAULT.name
        val cornerStyleStr = prefs.getString(KEY_CORNER_STYLE, CornerStyle.ROUNDED.name) ?: CornerStyle.ROUNDED.name
        val badges = prefs.getBoolean(KEY_SHOW_BADGES, true)
        val dynamicCol = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        val compactTiles = prefs.getBoolean(KEY_COMPACT_TILES, false)
        val sampleRate = prefs.getInt(KEY_SAMPLE_RATE, 44100)

        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            natureTheme = runCatching { NatureTheme.valueOf(natureThemeStr) }.getOrDefault(NatureTheme.DEFAULT),
            dashboardLayout = runCatching { DashboardLayout.valueOf(layoutStr) }.getOrDefault(DashboardLayout.CARDS),
            toolsSortOrder = runCatching { ToolsSortOrder.valueOf(sortOrderStr) }.getOrDefault(ToolsSortOrder.DEFAULT),
            cornerStyle = runCatching { CornerStyle.valueOf(cornerStyleStr) }.getOrDefault(CornerStyle.ROUNDED),
            showCategoryBadges = badges,
            dynamicColor = dynamicCol,
            compactTilesGrid = compactTiles,
            defaultSampleRateHz = sampleRate
        )
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateNatureTheme(theme: NatureTheme) {
        prefs.edit().putString(KEY_NATURE_THEME, theme.name).apply()
        _settings.value = _settings.value.copy(natureTheme = theme)
    }

    fun updateDashboardLayout(layout: DashboardLayout) {
        prefs.edit().putString(KEY_LAYOUT, layout.name).apply()
        _settings.value = _settings.value.copy(dashboardLayout = layout)
    }

    fun updateToolsSortOrder(order: ToolsSortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
        _settings.value = _settings.value.copy(toolsSortOrder = order)
    }

    fun updateCornerStyle(style: CornerStyle) {
        prefs.edit().putString(KEY_CORNER_STYLE, style.name).apply()
        _settings.value = _settings.value.copy(cornerStyle = style)
    }

    fun updateShowCategoryBadges(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BADGES, show).apply()
        _settings.value = _settings.value.copy(showCategoryBadges = show)
    }

    fun updateDynamicColor(enable: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enable).apply()
        _settings.value = _settings.value.copy(dynamicColor = enable)
    }

    fun updateCompactTilesGrid(compact: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT_TILES, compact).apply()
        _settings.value = _settings.value.copy(compactTilesGrid = compact)
    }

    fun updateDefaultSampleRate(rateHz: Int) {
        prefs.edit().putInt(KEY_SAMPLE_RATE, rateHz).apply()
        _settings.value = _settings.value.copy(defaultSampleRateHz = rateHz)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settings.value = AppSettings()
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NATURE_THEME = "nature_theme"
        private const val KEY_LAYOUT = "dashboard_layout"
        private const val KEY_SORT_ORDER = "tools_sort_order"
        private const val KEY_CORNER_STYLE = "corner_style"
        private const val KEY_SHOW_BADGES = "show_category_badges"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_COMPACT_TILES = "compact_tiles_grid"
        private const val KEY_SAMPLE_RATE = "default_sample_rate"
    }
}
