package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.data.*
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings & Customization", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Themes, Tiles, Cards & Deep Personalization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Appearance & Light Themes
            SettingsCard(title = "APPEARANCE & THEME MODE", icon = Icons.Default.Palette) {
                Text(
                    "Choose between dynamic system tracking, clean expressive Light themes, or dark studio mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val selected = settings.themeMode == mode
                        val (icon, label) = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "System"
                            ThemeMode.LIGHT -> Icons.Default.LightMode to "Light"
                            ThemeMode.DARK -> Icons.Default.DarkMode to "Dark"
                        }

                        OutlinedCard(
                            onClick = { settingsRepository.updateThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(settings.cornerStyle.cornerRadiusDp.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(
                                enabled = true
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Section: Nature Inspiration Themes
            SettingsCard(title = "NATURE INSPIRATION THEMES", icon = Icons.Default.Forest) {
                Text(
                    "Tailored color schemes inspired by natural elements with rich Light and Dark variants.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NatureTheme.values().forEach { nature ->
                        val isSelected = settings.natureTheme == nature
                        val previewColors = when (nature) {
                            NatureTheme.DEFAULT -> listOf(ExpressivePrimary, ExpressiveSecondary, ExpressiveTertiary)
                            NatureTheme.FOREST_EMERALD -> listOf(ForestPrimary, ForestSecondary, ForestTertiary)
                            NatureTheme.OCEAN_BREEZE -> listOf(OceanPrimary, OceanSecondary, OceanTertiary)
                            NatureTheme.SUNSET_AMBER -> listOf(SunsetPrimary, SunsetSecondary, SunsetTertiary)
                            NatureTheme.CHERRY_BLOSSOM -> listOf(BlossomPrimary, BlossomSecondary, BlossomTertiary)
                            NatureTheme.ARCTIC_FROST -> listOf(ArcticPrimary, ArcticSecondary, ArcticTertiary)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(settings.cornerStyle.cornerRadiusDp.dp))
                                .clickable { settingsRepository.updateNatureTheme(nature) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Theme preview swatch
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(previewColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nature.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = nature.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { settingsRepository.updateNatureTheme(nature) }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Dashboard Layout (Tiles vs Cards)
            SettingsCard(title = "DASHBOARD LAYOUT (TILES & CARDS)", icon = Icons.Default.DashboardCustomize) {
                Text(
                    "Switch between high-density quick action Tiles or comprehensive detailed Cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cards Option
                    OutlinedCard(
                        onClick = { settingsRepository.updateDashboardLayout(DashboardLayout.CARDS) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(settings.cornerStyle.cornerRadiusDp.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (settings.dashboardLayout == DashboardLayout.CARDS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Cards View", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Full descriptions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }

                    // Tiles Option
                    OutlinedCard(
                        onClick = { settingsRepository.updateDashboardLayout(DashboardLayout.TILES) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(settings.cornerStyle.cornerRadiusDp.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (settings.dashboardLayout == DashboardLayout.TILES) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tiles View", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("2-Column Grid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Section: Tools Sorting Order
            SettingsCard(title = "TOOLS SORT & ARRANGEMENT", icon = Icons.AutoMirrored.Filled.Sort) {
                Text(
                    "Select how audio workbench tools are ordered across the dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ToolsSortOrder.values().forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsRepository.updateToolsSortOrder(order) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.toolsSortOrder == order,
                            onClick = { settingsRepository.updateToolsSortOrder(order) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(order.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                }
            }

            // Section: Deep Customizations (Corner radius, Badges, DSP defaults)
            SettingsCard(title = "DEEP CUSTOMIZATIONS", icon = Icons.Default.Tune) {
                Text(
                    "Card & Tile Shape Roundness",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CornerStyle.values().forEach { style ->
                        val selected = settings.cornerStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { settingsRepository.updateCornerStyle(style) },
                            label = { Text(style.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Show category tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Category Badges", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Display colored taxonomy badges on tiles & cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showCategoryBadges,
                        onCheckedChange = { settingsRepository.updateShowCategoryBadges(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default DSP Sample Rate selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Audio Engine Rate", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("PCM synthesis and mastering target rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(44100, 48000).forEach { rate ->
                            FilterChip(
                                selected = settings.defaultSampleRateHz == rate,
                                onClick = { settingsRepository.updateDefaultSampleRate(rate) },
                                label = { Text("${rate / 1000} kHz") }
                            )
                        }
                    }
                }
            }

            // Reset to Defaults
            OutlinedButton(
                onClick = { settingsRepository.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Customizations to Defaults")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
