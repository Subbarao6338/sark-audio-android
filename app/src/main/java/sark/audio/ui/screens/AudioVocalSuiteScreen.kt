package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVocalSuiteScreen(
    onBack: () -> Unit
) {
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var activeAudio by remember {
        mutableStateOf(
            AudioToneGenerator.generateTone(
                AudioToneGenerator.WaveformType.SINE, 350.0, durationSeconds = 3.0, channels = 2, amplitude = 0.6f
            )
        )
    }

    var waveformAmplitudes by remember {
        mutableStateOf(WavFileProcessor.extractWaveformAmplitudes(activeAudio.samples, activeAudio.header.channels, 80))
    }

    var selectedVoicePreset by remember { mutableStateOf(AudioVoiceChanger.VoicePreset.CHIPMUNK) }
    var selectedScale by remember { mutableStateOf(AudioAutotuneProcessor.MusicalScale.CHROMATIC) }
    var autotuneStrength by remember { mutableStateOf(0.85f) }
    var vocalCutStrength by remember { mutableStateOf(0.95f) }
    var statusText by remember { mutableStateOf("Vocal Suite Ready") }

    LaunchedEffect(activeAudio) {
        player.load(activeAudio)
        waveformAmplitudes = WavFileProcessor.extractWaveformAmplitudes(activeAudio.samples, activeAudio.header.channels, 80)
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val totalMs = activeAudio.header.durationMs
    val playProgress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocal, Karaoke & Autotune Suite") },
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
            // Waveform Monitor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AUDIO PLAYBACK MONITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    WaveformVisualizer(
                        amplitudes = waveformAmplitudes,
                        playbackProgress = playProgress,
                        onSeek = { ratio -> player.seekTo((ratio * totalMs).toLong()) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (playbackState == AudioPlaybackController.PlaybackState.Playing) player.pause() else player.play()
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        ) {
                            Icon(
                                imageVector = if (playbackState == AudioPlaybackController.PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { player.stop() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Karaoke / Vocal Remover Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("VOCAL REMOVER & KARAOKE", style = MaterialTheme.typography.labelSmall, color = AccentCoral, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vocal Cut Strength: ${(vocalCutStrength * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(
                        value = vocalCutStrength,
                        onValueChange = { vocalCutStrength = it },
                        valueRange = 0.5f..1.0f
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                activeAudio = AudioVocalProcessor.removeVocals(activeAudio, vocalReductionStrength = vocalCutStrength)
                                statusText = "Vocal cancellation applied."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove Vocals", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                activeAudio = AudioVocalProcessor.isolateVocals(activeAudio)
                                statusText = "Center vocals isolated."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Isolate Vocals", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Autotune Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("VOCAL AUTOTUNE & SCALE SNAP", style = MaterialTheme.typography.labelSmall, color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AudioAutotuneProcessor.MusicalScale.CHROMATIC,
                            AudioAutotuneProcessor.MusicalScale.MAJOR,
                            AudioAutotuneProcessor.MusicalScale.MINOR,
                            AudioAutotuneProcessor.MusicalScale.PENTATONIC
                        ).forEach { scale ->
                            FilterChip(
                                selected = selectedScale == scale,
                                onClick = { selectedScale = scale },
                                label = { Text(scale.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Correction Snap: ${(autotuneStrength * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(
                        value = autotuneStrength,
                        onValueChange = { autotuneStrength = it },
                        valueRange = 0.1f..1.0f
                    )
                    Button(
                        onClick = {
                            activeAudio = AudioAutotuneProcessor.applyAutotune(activeAudio, selectedScale, autotuneStrength)
                            statusText = "Autotune quantized to ${selectedScale.name}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Autotune Pitch Snap")
                    }
                }
            }

            // Voice Changer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("VOICE CHANGER PRESETS", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AudioVoiceChanger.VoicePreset.CHIPMUNK,
                            AudioVoiceChanger.VoicePreset.DEEP_MONSTER,
                            AudioVoiceChanger.VoicePreset.ROBOT
                        ).forEach { preset ->
                            FilterChip(
                                selected = selectedVoicePreset == preset,
                                onClick = { selectedVoicePreset = preset },
                                label = { Text(preset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AudioVoiceChanger.VoicePreset.ALIEN,
                            AudioVoiceChanger.VoicePreset.HELIUM,
                            AudioVoiceChanger.VoicePreset.RADIO
                        ).forEach { preset ->
                            FilterChip(
                                selected = selectedVoicePreset == preset,
                                onClick = { selectedVoicePreset = preset },
                                label = { Text(preset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            activeAudio = AudioVoiceChanger.applyPreset(activeAudio, selectedVoicePreset)
                            statusText = "Applied ${selectedVoicePreset.name} voice preset."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Voice Changer")
                    }
                }
            }

            // Stems & Voice Clone Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("STEMS & VOICE TIMBRE CLONE", style = MaterialTheme.typography.labelSmall, color = AccentTeal, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val stems = AudioStemsSplitter.splitStems(activeAudio)
                                activeAudio = stems.vocalsStem
                                statusText = "Split 4 stems (Vocals, Bass, Drums, Melody). Playing Vocals."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Split Stems", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val profile = AudioVoiceCloneSimulator.extractFormants(activeAudio)
                                activeAudio = AudioVoiceCloneSimulator.applyVocalCloneProfile(activeAudio, profile)
                                statusText = "Modeled vocal formants (F1: ${profile.f1Hz.toInt()}Hz, F2: ${profile.f2Hz.toInt()}Hz)."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clone Timbre", fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(statusText, color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
