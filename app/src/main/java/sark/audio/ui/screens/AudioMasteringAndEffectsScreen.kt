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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.components.AudioFileSelectorBar
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMasteringAndEffectsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var currentFileName by remember { mutableStateOf("demo_master.wav") }
    var activeAudio by remember {
        mutableStateOf(
            AudioToneGenerator.generateTone(
                AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 3.5, channels = 2, amplitude = 0.5f
            )
        )
    }

    var waveformAmplitudes by remember {
        mutableStateOf(WavFileProcessor.extractWaveformAmplitudes(activeAudio.samples, activeAudio.header.channels, 80))
    }

    // EQ Sliders
    var bassGain by remember { mutableStateOf(2.0f) }
    var midGain by remember { mutableStateOf(0.0f) }
    var trebleGain by remember { mutableStateOf(1.5f) }

    // 8D Audio
    var eightDSpeedHz by remember { mutableStateOf(0.15f) }

    // Chorus
    var chorusRate by remember { mutableStateOf(1.2f) }
    var chorusDepth by remember { mutableStateOf(3.0f) }

    var statusText by remember { mutableStateOf("Mastering & DSP Chain Ready") }

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
                title = { Text("Mastering, EQ, 8D & Effects") },
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
                onAudioLoaded = { loaded ->
                    activeAudio = loaded.audioData
                    currentFileName = loaded.fileName
                    statusText = "Loaded ${loaded.fileName} for mastering & effects."
                }
            )

            // Monitor Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DSP MONITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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

            // Mastering Engine
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("SOUND MASTERING ENGINE", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Multi-band excitation, warm analog tape saturation, stereo widen & -0.1 dBFS ceiling limiter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            activeAudio = AudioMasteringEngine.masterAudio(activeAudio)
                            statusText = "Mastered audio with multi-stage dynamics & peak limiter."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Studio Audio Mastering")
                    }
                }
            }

            // 3-Band Parametric EQ
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("3-BAND PARAMETRIC EQUALIZER", style = MaterialTheme.typography.labelSmall, color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bass (100 Hz): ${"%.1f".format(bassGain)} dB", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(value = bassGain, onValueChange = { bassGain = it }, valueRange = -12f..12f)

                    Text("Mid (1 kHz): ${"%.1f".format(midGain)} dB", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(value = midGain, onValueChange = { midGain = it }, valueRange = -12f..12f)

                    Text("Treble (10 kHz): ${"%.1f".format(trebleGain)} dB", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(value = trebleGain, onValueChange = { trebleGain = it }, valueRange = -12f..12f)

                    Button(
                        onClick = {
                            activeAudio = AudioEqualizerProcessor.apply3Band(activeAudio, bassGain.toDouble(), midGain.toDouble(), trebleGain.toDouble())
                            statusText = "Applied 3-band parametric EQ."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply EQ Shaping")
                    }
                }
            }

            // 8D Audio & Spatial Ambience
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("8D AUDIO & BINAURAL SPATIAL PANNING", style = MaterialTheme.typography.labelSmall, color = AccentCoral, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rotation Speed: ${"%.2f".format(eightDSpeedHz)} Hz", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(value = eightDSpeedHz, onValueChange = { eightDSpeedHz = it }, valueRange = 0.05f..0.5f)

                    Button(
                        onClick = {
                            activeAudio = Audio8DProcessor.process8D(activeAudio, rotationSpeedHz = eightDSpeedHz.toDouble())
                            statusText = "Converted to 360-degree 8D spatial headphone audio."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SpatialAudio, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Render 8D Audio")
                    }
                }
            }

            // Chorus & Reverb Remover
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("CHORUS & REVERB REMOVER", style = MaterialTheme.typography.labelSmall, color = AccentTeal, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                activeAudio = AudioChorusProcessor.applyChorus(activeAudio, chorusRate.toDouble(), chorusDepth.toDouble())
                                statusText = "Stereo Chorus applied."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply Chorus", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                activeAudio = AudioDenoiseProcessor.removeReverbAndEcho(activeAudio)
                                statusText = "Echo and room decay tails attenuated."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove Reverb", fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(statusText, color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Button(
                onClick = {
                    val saved = manager.saveAudio(activeAudio, "mastered_${System.currentTimeMillis() % 10000}")
                    statusText = "Saved mastered audio to library: ${saved.name}"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Mastered Audio to Library")
            }
        }
    }
}
