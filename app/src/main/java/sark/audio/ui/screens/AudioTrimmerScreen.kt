package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.AudioData
import sark.audio.core.AudioPlaybackController
import sark.audio.core.AudioTrimmer
import sark.audio.core.WavFileProcessor
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerScreen(
    onBack: () -> Unit
) {
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var activeAudio by remember {
        mutableStateOf(
            WavFileProcessor.generateSineWave(
                sampleRate = 44100,
                frequencyHz = 440.0,
                durationSeconds = 6.0,
                channels = 1
            )
        )
    }

    var waveformAmplitudes by remember {
        mutableStateOf(
            WavFileProcessor.extractWaveformAmplitudes(
                activeAudio.samples,
                activeAudio.header.channels,
                80
            )
        )
    }

    var startCutMs by remember { mutableStateOf(1000L) }
    var endCutMs by remember { mutableStateOf(4500L) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeAudio) {
        player.load(activeAudio)
        waveformAmplitudes = WavFileProcessor.extractWaveformAmplitudes(
            activeAudio.samples,
            activeAudio.header.channels,
            80
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    val totalMs = activeAudio.header.durationMs
    val startProgress = if (totalMs > 0) (startCutMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val endProgress = if (totalMs > 0) (endCutMs.toFloat() / totalMs).coerceIn(0f, 1f) else 1f
    val playProgress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Cutter & Trimmer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                            Text("WAVEFORM TRIM VIEW", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                "Selected: ${((endCutMs - startCutMs) / 1000.0).coerceAtLeast(0.0)}s",
                                color = AccentCoral,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        WaveformVisualizer(
                            amplitudes = waveformAmplitudes,
                            playbackProgress = playProgress,
                            startMarkerProgress = startProgress,
                            endMarkerProgress = endProgress,
                            onSeek = { ratio ->
                                player.seekTo((ratio * totalMs).toLong())
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "00:00",
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                "%02d:%02d".format((totalMs / 1000) / 60, (totalMs / 1000) % 60),
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Boundary Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("CUT BOUNDARIES", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Start Marker Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Start: ${startCutMs}ms", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = startCutMs.toFloat(),
                                onValueChange = {
                                    startCutMs = it.toLong().coerceIn(0L, endCutMs)
                                },
                                valueRange = 0f..totalMs.toFloat().coerceAtLeast(1f),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // End Marker Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("End: ${endCutMs}ms", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = endCutMs.toFloat(),
                                onValueChange = {
                                    endCutMs = it.toLong().coerceIn(startCutMs, totalMs)
                                },
                                valueRange = 0f..totalMs.toFloat().coerceAtLeast(1f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bottom action section
            Column {
                if (snackbarMessage != null) {
                    Text(
                        text = snackbarMessage ?: "",
                        color = AccentTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // Playback control bar
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

                // Actions: Trim or Delete Middle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            activeAudio = AudioTrimmer.trim(activeAudio, startCutMs, endCutMs)
                            startCutMs = 0L
                            endCutMs = activeAudio.header.durationMs
                            snackbarMessage = "Trimmed selected segment successfully."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keep Segment")
                    }

                    Button(
                        onClick = {
                            activeAudio = AudioTrimmer.cutOut(activeAudio, startCutMs, endCutMs)
                            startCutMs = 0L
                            endCutMs = activeAudio.header.durationMs
                            snackbarMessage = "Deleted selected segment successfully."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Selection")
                    }
                }
            }
        }
    }
}
