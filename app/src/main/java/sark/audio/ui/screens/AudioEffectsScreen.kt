package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun AudioEffectsScreen(
    onBack: () -> Unit
) {
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    val originalAudio = remember {
        WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 520.0,
            durationSeconds = 4.0,
            channels = 1,
            amplitude = 0.4f
        )
    }

    var processedAudio by remember { mutableStateOf(originalAudio) }
    var waveformAmplitudes by remember {
        mutableStateOf(
            WavFileProcessor.extractWaveformAmplitudes(
                originalAudio.samples,
                originalAudio.header.channels,
                80
            )
        )
    }

    var volumeMultiplier by remember { mutableStateOf(1.0f) }
    var speedMultiplier by remember { mutableStateOf(1.0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(processedAudio) {
        player.load(processedAudio)
        waveformAmplitudes = WavFileProcessor.extractWaveformAmplitudes(
            processedAudio.samples,
            processedAudio.header.channels,
            80
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    val totalMs = processedAudio.header.durationMs
    val playProgress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gain Booster & Effects") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface, titleContentColor = TextPrimary)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AUDIO MONITOR", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${processedAudio.header.durationMs} ms", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        WaveformVisualizer(
                            amplitudes = waveformAmplitudes,
                            playbackProgress = playProgress,
                            onSeek = { ratio ->
                                player.seekTo((ratio * totalMs).toLong())
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Sliders
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Volume / Gain Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume Gain Booster", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${(volumeMultiplier * 100).toInt()}%", color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = volumeMultiplier,
                            onValueChange = { volumeMultiplier = it },
                            valueRange = 0.2f..3.0f
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speed / Tempo Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speed / Tempo", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("%.2fx".format(speedMultiplier), color = AccentAmber, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speedMultiplier,
                            onValueChange = { speedMultiplier = it },
                            valueRange = 0.5f..2.0f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            processedAudio = AudioGainProcessor.normalize(processedAudio)
                            statusMessage = "Peak normalization applied (0.95)."
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Normalize Peak", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            processedAudio = AudioTimePitchProcessor.reverse(processedAudio)
                            statusMessage = "Audio reversed."
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reverse", fontSize = 12.sp)
                    }
                }
            }

            // Bottom section
            Column {
                if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        color = AccentTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Playback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (playbackState == AudioPlaybackController.PlaybackState.Playing) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo)
                    ) {
                        Icon(
                            imageVector = if (playbackState == AudioPlaybackController.PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { player.stop() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        var res = AudioGainProcessor.applyGain(originalAudio, volumeMultiplier)
                        res = AudioTimePitchProcessor.changeSpeed(res, speedMultiplier)
                        processedAudio = res
                        statusMessage = "Applied Volume (${(volumeMultiplier * 100).toInt()}%) & Speed (${"%.2f".format(speedMultiplier)}x)."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process & Render Audio", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
