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
fun AudioCreatorScreen(
    onBack: () -> Unit
) {
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var selectedMode by remember { mutableStateOf("wave") } // wave, dtmf, morse, silence
    var waveformType by remember { mutableStateOf(AudioToneGenerator.WaveformType.SINE) }
    var frequencyHz by remember { mutableStateOf(440f) }
    var durationSec by remember { mutableStateOf(2.0f) }
    var dtmfSequence by remember { mutableStateOf("8675309") }
    var morseText by remember { mutableStateOf("SARK AUDIO") }
    var silenceDurationMs by remember { mutableStateOf(1000f) }

    var generatedAudio by remember {
        mutableStateOf(
            AudioToneGenerator.generateTone(
                AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 2.0
            )
        )
    }

    var waveformAmplitudes by remember {
        mutableStateOf(
            WavFileProcessor.extractWaveformAmplitudes(
                generatedAudio.samples,
                generatedAudio.header.channels,
                80
            )
        )
    }

    var statusMessage by remember { mutableStateOf("Ready to synthesize.") }

    LaunchedEffect(generatedAudio) {
        player.load(generatedAudio)
        waveformAmplitudes = WavFileProcessor.extractWaveformAmplitudes(
            generatedAudio.samples,
            generatedAudio.header.channels,
            80
        )
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val totalMs = generatedAudio.header.durationMs
    val playProgress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Creator & Synthesizer") },
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
            // Waveform Monitor Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SYNTHESIS MONITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("${generatedAudio.header.durationMs} ms", color = AccentCoral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    WaveformVisualizer(
                        amplitudes = waveformAmplitudes,
                        playbackProgress = playProgress,
                        onSeek = { ratio -> player.seekTo((ratio * totalMs).toLong()) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
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

            // Mode Selector
            TabRow(
                selectedTabIndex = when (selectedMode) {
                    "wave" -> 0
                    "dtmf" -> 1
                    "morse" -> 2
                    else -> 3
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                contentColor = PrimaryIndigoLight
            ) {
                Tab(selected = selectedMode == "wave", onClick = { selectedMode = "wave" }, text = { Text("Waveforms") })
                Tab(selected = selectedMode == "dtmf", onClick = { selectedMode = "dtmf" }, text = { Text("DTMF Keypad") })
                Tab(selected = selectedMode == "morse", onClick = { selectedMode = "morse" }, text = { Text("Morse Code") })
                Tab(selected = selectedMode == "silence", onClick = { selectedMode = "silence" }, text = { Text("Silence") })
            }

            // Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    when (selectedMode) {
                        "wave" -> {
                            Text("Waveform Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    AudioToneGenerator.WaveformType.SINE,
                                    AudioToneGenerator.WaveformType.SQUARE,
                                    AudioToneGenerator.WaveformType.TRIANGLE,
                                    AudioToneGenerator.WaveformType.SAWTOOTH
                                ).forEach { type ->
                                    FilterChip(
                                        selected = waveformType == type,
                                        onClick = { waveformType = type },
                                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    AudioToneGenerator.WaveformType.WHITE_NOISE,
                                    AudioToneGenerator.WaveformType.PINK_NOISE,
                                    AudioToneGenerator.WaveformType.CHIRP
                                ).forEach { type ->
                                    FilterChip(
                                        selected = waveformType == type,
                                        onClick = { waveformType = type },
                                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Frequency: ${frequencyHz.toInt()} Hz", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Slider(
                                value = frequencyHz,
                                onValueChange = { frequencyHz = it },
                                valueRange = 40f..4000f
                            )

                            Text("Duration: ${"%.1f".format(durationSec)}s", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Slider(
                                value = durationSec,
                                onValueChange = { durationSec = it },
                                valueRange = 0.5f..5.0f
                            )
                        }
                        "dtmf" -> {
                            OutlinedTextField(
                                value = dtmfSequence,
                                onValueChange = { dtmfSequence = it },
                                label = { Text("Dialpad Digits (0-9, *, #, A-D)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "morse" -> {
                            OutlinedTextField(
                                value = morseText,
                                onValueChange = { morseText = it },
                                label = { Text("Text to Encode as Morse") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "silence" -> {
                            Text("Silence Duration: ${silenceDurationMs.toInt()} ms", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Slider(
                                value = silenceDurationMs,
                                onValueChange = { silenceDurationMs = it },
                                valueRange = 100f..5000f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            generatedAudio = when (selectedMode) {
                                "wave" -> AudioToneGenerator.generateTone(
                                    type = waveformType,
                                    frequencyHz = frequencyHz.toDouble(),
                                    durationSeconds = durationSec.toDouble()
                                )
                                "dtmf" -> AudioDtmfGenerator.generateDialSequence(dtmfSequence)
                                "morse" -> AudioDtmfGenerator.generateMorseCode(morseText)
                                else -> AudioSilenceGenerator.generateSilence(silenceDurationMs.toLong())
                            }
                            statusMessage = "Successfully synthesized audio."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesize Waveform")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(statusMessage, color = AccentTeal, fontSize = 12.sp)
                }
            }
        }
    }
}
