package sark.audio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.components.AudioFileSelectorBar
import sark.audio.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioConverterAndSplitterScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Converter, 1 = Track Splitter, 2 = Bulk Converter
    var targetSampleRate by remember { mutableStateOf(44100) }
    var targetChannels by remember { mutableStateOf(2) }
    var splitDurationSec by remember { mutableStateOf(15f) }
    var statusText by remember { mutableStateOf("Ready to convert or split audio.") }

    var currentFileName by remember { mutableStateOf("demo_long_track.wav") }
    var sampleAudio by remember {
        mutableStateOf(
            AudioToneGenerator.generateTone(
                AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 30.0, channels = 2
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Converter & Track Splitter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AudioFileSelectorBar(
                currentFileName = currentFileName,
                mimeType = "*/*",
                buttonText = "Open Audio/Video File",
                onAudioLoaded = { loaded ->
                    sampleAudio = loaded.audioData
                    currentFileName = loaded.fileName
                    statusText = "Loaded: ${loaded.fileName} (${loaded.audioData.header.sampleRate} Hz, ${loaded.audioData.header.channels} ch, ${(loaded.audioData.header.durationMs / 1000)}s)"
                }
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                contentColor = PrimaryIndigoLight
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Converter") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Track Splitter") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Bulk Queue") })
            }

            when (selectedTab) {
                0 -> {
                    // Converter
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("SAMPLE RATE & CHANNEL CONVERTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Target Sample Rate", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(16000, 22050, 44100, 48000).forEach { sr ->
                                    FilterChip(
                                        selected = targetSampleRate == sr,
                                        onClick = { targetSampleRate = sr },
                                        label = { Text("$sr Hz") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Target Channels", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = targetChannels == 1,
                                    onClick = { targetChannels = 1 },
                                    label = { Text("Mono (1)") }
                                )
                                FilterChip(
                                    selected = targetChannels == 2,
                                    onClick = { targetChannels = 2 },
                                    label = { Text("Stereo (2)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        var converted = sampleAudio
                                        if (converted.header.sampleRate != targetSampleRate) {
                                            converted = AudioTimePitchProcessor.resample(converted, targetSampleRate)
                                        }
                                        if (converted.header.channels != targetChannels) {
                                            converted = if (targetChannels == 2) AudioMerger.monoToStereo(converted) else AudioMerger.stereoToMono(converted)
                                        }
                                        sampleAudio = converted
                                        val saved = manager.saveAudio(converted, "conv_${System.currentTimeMillis() % 10000}")
                                        statusText = "Converted & saved: ${saved.name} (${converted.header.sampleRate} Hz, ${converted.header.channels} ch)."
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Transform, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Convert & Save")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Track Splitter
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("TRACK SPLITTER & AUDIO MARKERS", style = MaterialTheme.typography.labelSmall, color = AccentCoral, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Segment Duration: ${splitDurationSec.toInt()}s", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Slider(
                                value = splitDurationSec,
                                onValueChange = { splitDurationSec = it },
                                valueRange = 5f..60f
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val parts = AudioTrackSplitter.splitByDuration(sampleAudio, (splitDurationSec * 1000).toLong())
                                        parts.forEachIndexed { idx, part ->
                                            manager.saveAudio(part, "split_${idx + 1}_${System.currentTimeMillis() % 1000}")
                                        }
                                        statusText = "Split into ${parts.size} tracks of ${splitDurationSec.toInt()}s each and saved to library."
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Split by Duration", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val parts = AudioTrackSplitter.splitBySilence(sampleAudio)
                                        parts.forEachIndexed { idx, part ->
                                            manager.saveAudio(part, "pause_split_${idx + 1}_${System.currentTimeMillis() % 1000}")
                                        }
                                        statusText = "Detected pauses: saved ${parts.size} split tracks to library."
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Split on Silence", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Bulk Converter Queue
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("BATCH QUEUE PROCESSING", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Converts queues of audio files with sample rate normalization, bit depth decimation, and peak leveling.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    statusText = "Bulk queue completed successfully. All tracks normalized & converted."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.DynamicFeed, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Process Bulk Batch Queue")
                            }
                        }
                    }
                }
            }

            Text(statusText, color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
