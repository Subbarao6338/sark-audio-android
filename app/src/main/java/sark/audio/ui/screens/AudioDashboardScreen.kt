package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.data.AppSettings
import sark.audio.data.DashboardLayout
import sark.audio.data.SettingsRepository
import sark.audio.data.ToolsSortOrder
import sark.audio.ui.theme.*

data class ToolboxTool(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDashboardScreen(
    settings: AppSettings? = null,
    settingsRepository: SettingsRepository? = null,
    onNavigateToTool: (String) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val currentSettings = settings ?: AppSettings()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val allTools = remember {
        listOf(
            // --- Core Pillars: Creator, Editor, Recorder, Manager ---
            ToolboxTool("creator", "Audio Creator / Wave Synthesizer", "Creator", "Synthesize Sine, Square, Triangle, Sawtooth, Chirp, White/Pink/Brown noise", Icons.Default.GraphicEq, AccentCoral),
            ToolboxTool("dtmf_creator", "DTMF & Morse Generator", "Creator", "Synthesize telephone DTMF touch-tones and audible Morse code sequences", Icons.Default.Dialpad, AccentAmber),
            ToolboxTool("silence_creator", "Digital Silence Generator", "Creator", "Create microsecond-accurate digital silent audio files", Icons.AutoMirrored.Filled.VolumeMute, AccentTeal),
            ToolboxTool("editor", "Audio Cutter & Trimmer", "Editor", "Cut, slice, crop boundaries, invert phase and insert silence segments", Icons.Default.ContentCut, PrimaryIndigoLight),
            ToolboxTool("fade_editor", "Fade In / Out Shaper", "Editor", "Apply Linear and S-curve audio volume fades seamlessly", Icons.AutoMirrored.Filled.TrendingUp, PrimaryIndigo),
            ToolboxTool("silence_strip", "Silence Remover", "Editor", "Automatically strip quiet leading, trailing, and internal silence", Icons.Default.ContentCut, AccentCoral),
            ToolboxTool("track_splitter", "Track & Marker Splitter", "Editor", "Split tracks by duration, custom cue markers, or pauses", Icons.AutoMirrored.Filled.CallSplit, AccentAmber),
            ToolboxTool("recorder", "Studio Audio Recorder", "Recorder", "Studio grade PCM audio capture with real-time spectrum & waveform", Icons.Default.Mic, AccentCoral),
            ToolboxTool("manager", "Audio Library Manager", "Manager", "Browse, play, rename, delete and manage your audio takes & creations", Icons.Default.FolderOpen, AccentTeal),

            // --- Multi-track & Mixing Tools ---
            ToolboxTool("merger", "Audio Merger & Joiner", "Mixer", "Sequential multi-clip audio track joiner with crossfade blending", Icons.AutoMirrored.Filled.MergeType, AccentTeal),
            ToolboxTool("mixer", "Multi-Track Audio Mixer & Remix", "Mixer", "Parallel multi-channel audio mixer with per-track volume & soft limiter", Icons.Default.Layers, AccentAmber),
            ToolboxTool("channel_processor", "Stereo / Mono & Panning", "Tools", "Stereo splitting, mono upmixing, channel swapping, and balance panning", Icons.Default.Speaker, PrimaryIndigoLight),
            ToolboxTool("loop_creator", "Audio Loop Creator", "Tools", "Seamlessly repeat loops with click-free crossfade seam blending", Icons.Default.Repeat, AccentCoral),
            ToolboxTool("sfx_manager", "SFX Overlay & Sound Effects", "Tools", "Synthesize and layer beeps, risers, impacts, and laser FX", Icons.Default.NotificationsActive, AccentAmber),

            // --- Vocal & Karaoke Suite ---
            ToolboxTool("vocal_remover", "Vocal Remover & Karaoke", "Vocal Suite", "Center-channel cancellation for instant karaoke instrumental tracks", Icons.Default.MusicOff, AccentCoral),
            ToolboxTool("autotune", "Vocal Autotune", "Vocal Suite", "Real-time pitch detection and scale quantization snap", Icons.Default.AutoFixHigh, PrimaryIndigoLight),
            ToolboxTool("voice_changer", "Voice Changer Presets", "Vocal Suite", "Transform voice into Chipmunk, Deep Robot, Alien, Monster, Helium", Icons.Default.RecordVoiceOver, AccentAmber),
            ToolboxTool("stems_splitter", "Musical Stems Splitter", "Vocal Suite", "Isolate Vocals, Bass, Drums, and Melody into separate stems", Icons.Default.PieChart, AccentTeal),
            ToolboxTool("voice_clone", "Voice Clone Simulator", "Vocal Suite", "Formant resonator model capturing and imposing vocal timbre", Icons.Default.Fingerprint, PrimaryIndigo),

            // --- Effects & Sound Shaping Tools ---
            ToolboxTool("mastering", "Sound Mastering Engine", "Effects", "Warmth excitation, multi-band shaping, stereo widen & ceiling limiter", Icons.Default.Stars, AccentAmber),
            ToolboxTool("effects", "Gain Booster & Speed", "Effects", "Volume amplifier, peak normalizer, RMS leveling & tempo transformer", Icons.Default.Speed, AccentAmber),
            ToolboxTool("equalizer", "Parametric Equalizer", "Effects", "Multi-band Biquad EQ (Bass, Mid, Treble, Low/High Pass)", Icons.Default.Tune, PrimaryIndigo),
            ToolboxTool("compressor", "Dynamics Compressor", "Effects", "Studio dynamic range compression with threshold, ratio, attack, release", Icons.Default.Compress, AccentTeal),
            ToolboxTool("eight_d", "8D Spatial Audio", "Effects", "Immersive 360-degree binaural rotating audio with room ambience", Icons.Default.SpatialAudio, AccentCoral),
            ToolboxTool("delay", "Echo & Delay Unit", "Effects", "Feedback delay network with millisecond timing and wet/dry mix", Icons.Default.Schedule, PrimaryIndigoLight),
            ToolboxTool("chorus", "Stereo Chorus", "Effects", "Lush vocal and instrument doubling with dual LFO phase modulation", Icons.Default.SurroundSound, AccentTeal),
            ToolboxTool("distortion", "Distortion & Overdrive", "Effects", "Non-linear waveshaping, soft clipping saturation and analog fuzz", Icons.Default.Bolt, AccentCoral),
            ToolboxTool("bitcrusher", "Bitcrusher & Lo-Fi", "Effects", "Vintage 8-bit / 4-bit downsampling and resolution decimator", Icons.Default.Memory, AccentAmber),
            ToolboxTool("denoise", "Echo & Reverb Remover", "Effects", "Transient envelope gating and spectral de-reverberation", Icons.Default.Hearing, PrimaryIndigo),

            // --- Analysis & Conversion Tools ---
            ToolboxTool("tuner", "Instrument & Vocal Tuner", "Analysis", "Chromatic pitch tuner with cents deviation (-50 to +50 cents)", Icons.Default.Sensors, AccentTeal),
            ToolboxTool("analyzer", "FFT Spectrum Analyzer", "Analysis", "Real-time Cooley-Tukey FFT frequency graph and dominant pitch meter", Icons.Default.QueryStats, AccentTeal),
            ToolboxTool("inspector", "Inspector & Converter", "Analysis", "Deep WAV format metadata inspector, bit depth & sample rate converter", Icons.Default.Info, PrimaryIndigo),
            ToolboxTool("tag_editor", "Audio Tag & Metadata Editor", "Tools", "Edit Title, Artist, Album, Year, Genre, Track metadata chunks", Icons.AutoMirrored.Filled.Label, PrimaryIndigoLight),
            ToolboxTool("bulk_converter", "Bulk Audio Converter", "Tools", "Batch queue conversion of sample rate, channels, and bit depth", Icons.Default.DynamicFeed, AccentAmber),
            ToolboxTool("video_to_audio", "Video to Audio Extractor", "Tools", "Extract high fidelity PCM audio from MP4, MKV, and WEBM video files", Icons.Default.VideoFile, AccentCoral)
        )
    }

    val categories = listOf("All", "Creator", "Editor", "Recorder", "Manager", "Mixer", "Vocal Suite", "Effects", "Tools", "Analysis")

    // Filter by category and search text
    val filteredList = remember(searchQuery, selectedCategory, allTools) {
        allTools.filter { tool ->
            (selectedCategory == "All" || tool.category == selectedCategory) &&
                    (searchQuery.isBlank() || tool.title.contains(searchQuery, ignoreCase = true) || tool.description.contains(searchQuery, ignoreCase = true))
        }
    }

    // Sort by currentSettings.toolsSortOrder
    val sortedTools = remember(filteredList, currentSettings.toolsSortOrder) {
        when (currentSettings.toolsSortOrder) {
            ToolsSortOrder.DEFAULT -> filteredList
            ToolsSortOrder.NAME_ASC -> filteredList.sortedBy { it.title.lowercase() }
            ToolsSortOrder.NAME_DESC -> filteredList.sortedByDescending { it.title.lowercase() }
            ToolsSortOrder.CATEGORY -> filteredList.sortedWith(compareBy({ it.category }, { it.title }))
        }
    }

    val cornerRadius = currentSettings.cornerStyle.cornerRadiusDp.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SARK Audio Suite", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${allTools.size} Tools • ${currentSettings.natureTheme.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick layout toggle button (Cards <-> Tiles)
                    IconButton(
                        onClick = {
                            val newLayout = if (currentSettings.dashboardLayout == DashboardLayout.CARDS) DashboardLayout.TILES else DashboardLayout.CARDS
                            settingsRepository?.updateDashboardLayout(newLayout)
                        }
                    ) {
                        Icon(
                            imageVector = if (currentSettings.dashboardLayout == DashboardLayout.CARDS) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = "Switch Layout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tools Sort dropdown button
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Tools",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            ToolsSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            order.displayName,
                                            fontWeight = if (currentSettings.toolsSortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSettings.toolsSortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (currentSettings.toolsSortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        settingsRepository?.updateToolsSortOrder(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Settings Navigation
                    if (onNavigateToSettings != null) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${allTools.size} Audio Tools...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips scroll row
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subheader with tool count and layout indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AUDIO WORKBENCH (${sortedTools.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${currentSettings.toolsSortOrder.displayName} • ${if (currentSettings.dashboardLayout == DashboardLayout.TILES) "Tiles" else "Cards"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid rendering: 1 column for Cards, 2 columns for Tiles
            val columns = if (currentSettings.dashboardLayout == DashboardLayout.TILES) {
                GridCells.Fixed(2)
            } else {
                GridCells.Fixed(1)
            }

            LazyVerticalGrid(
                columns = columns,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedTools, key = { it.id }) { tool ->
                    if (currentSettings.dashboardLayout == DashboardLayout.TILES) {
                        ToolboxTile(
                            tool = tool,
                            cornerRadius = cornerRadius,
                            showBadge = currentSettings.showCategoryBadges,
                            onClick = { onNavigateToTool(tool.id) }
                        )
                    } else {
                        ToolboxCard(
                            tool = tool,
                            cornerRadius = cornerRadius,
                            showBadge = currentSettings.showCategoryBadges,
                            onClick = { onNavigateToTool(tool.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Cards view with rich descriptions and horizontal layout.
 */
@Composable
fun ToolboxCard(
    tool: ToolboxTool,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    showBadge: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(cornerRadius * 0.7f))
                    .background(tool.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = tool.color.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tool.category,
                                color = tool.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Compact quick-action Tiles view (2-column grid) with elevated visual punch.
 */
@Composable
fun ToolboxTile(
    tool: ToolboxTool,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    showBadge: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(cornerRadius * 0.7f))
                        .background(tool.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showBadge) {
                    Surface(
                        color = tool.color.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tool.category,
                            color = tool.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
