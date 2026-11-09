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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMixerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var track1Volume by remember { mutableStateOf(0.8f) }
    var track2Volume by remember { mutableStateOf(0.8f) }
    var track3Volume by remember { mutableStateOf(0.7f) }
    var track1Pan by remember { mutableStateOf(0.0f) }
    var track2Pan by remember { mutableStateOf(-0.4f) }
    var track3Pan by remember { mutableStateOf(0.4f) }
    var track1Mute by remember { mutableStateOf(false) }
    var track2Mute by remember { mutableStateOf(false) }
    var track3Mute by remember { mutableStateOf(false) }
    var track1Solo by remember { mutableStateOf(false) }
    var track2Solo by remember { mutableStateOf(false) }
    var track3Solo by remember { mutableStateOf(false) }
    var track2DelayMs by remember { mutableStateOf(200f) }

    var masterVolume by remember { mutableStateOf(1.0f) }

    var track1Name by remember { mutableStateOf("Track 1 (Lead)") }
    var track2Name by remember { mutableStateOf("Track 2 (Harmony)") }
    var track3Name by remember { mutableStateOf("Track 3 (Bass/Riser)") }

    var track1Audio by remember { mutableStateOf(AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 4.0, amplitude = 0.5f)) }
    var track2Audio by remember { mutableStateOf(AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.TRIANGLE, 660.0, durationSeconds = 3.5, amplitude = 0.4f)) }
    var track3Audio by remember { mutableStateOf(AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.CHIRP, 200.0, stopFrequencyHz = 800.0, durationSeconds = 3.0, amplitude = 0.4f)) }

    val track1Launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val loaded = AudioSystemLoader.loadFromUri(context, uri)
                track1Audio = loaded.audioData
                track1Name = loaded.fileName
            } catch (_: Exception) {}
        }
    }
    val track2Launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val loaded = AudioSystemLoader.loadFromUri(context, uri)
                track2Audio = loaded.audioData
                track2Name = loaded.fileName
            } catch (_: Exception) {}
        }
    }
    val track3Launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val loaded = AudioSystemLoader.loadFromUri(context, uri)
                track3Audio = loaded.audioData
                track3Name = loaded.fileName
            } catch (_: Exception) {}
        }
    }

    var mixedAudio by remember {
        mutableStateOf(
            AudioMixer.mixTracks(
                listOf(
                    AudioMixer.MixTrack(track1Audio, track1Volume, 0L),
                    AudioMixer.MixTrack(track2Audio, track2Volume, track2DelayMs.toLong()),
                    AudioMixer.MixTrack(track3Audio, track3Volume, 0L)
                )
            )
        )
    }

    var waveformAmplitudes by remember {
        mutableStateOf(WavFileProcessor.extractWaveformAmplitudes(mixedAudio.samples, mixedAudio.header.channels, 80))
    }

    var statusMessage by remember { mutableStateOf("Multi-track Mixer active.") }

    LaunchedEffect(mixedAudio) {
        player.load(mixedAudio)
        waveformAmplitudes = WavFileProcessor.extractWaveformAmplitudes(mixedAudio.samples, mixedAudio.header.channels, 80)
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val totalMs = mixedAudio.header.durationMs
    val playProgress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Track Mixer & Remix") },
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
            // Monitor Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("MASTER SUM MONITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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

            // Track Faders Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("MULTI-TRACK FADERS & OFFSETS", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Track 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$track1Name: ${(track1Volume * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Row {
                            FilterChip(selected = track1Mute, onClick = { track1Mute = !track1Mute }, label = { Text("Mute", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(selected = track1Solo, onClick = { track1Solo = !track1Solo }, label = { Text("Solo", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { track1Launcher.launch("audio/*") }) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick", fontSize = 11.sp)
                            }
                        }
                    }
                    Slider(value = track1Volume, onValueChange = { track1Volume = it }, valueRange = 0f..1.5f)
                    Text("Pan: ${"%.1f".format(track1Pan)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = track1Pan, onValueChange = { track1Pan = it }, valueRange = -1.0f..1.0f)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$track2Name: ${(track2Volume * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Row {
                            FilterChip(selected = track2Mute, onClick = { track2Mute = !track2Mute }, label = { Text("Mute", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(selected = track2Solo, onClick = { track2Solo = !track2Solo }, label = { Text("Solo", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { track2Launcher.launch("audio/*") }) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick", fontSize = 11.sp)
                            }
                        }
                    }
                    Slider(value = track2Volume, onValueChange = { track2Volume = it }, valueRange = 0f..1.5f)
                    Text("Pan: ${"%.1f".format(track2Pan)} | Delay: ${track2DelayMs.toInt()}ms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = track2Pan, onValueChange = { track2Pan = it }, valueRange = -1.0f..1.0f)
                    Slider(value = track2DelayMs, onValueChange = { track2DelayMs = it }, valueRange = 0f..1000f)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$track3Name: ${(track3Volume * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Row {
                            FilterChip(selected = track3Mute, onClick = { track3Mute = !track3Mute }, label = { Text("Mute", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(selected = track3Solo, onClick = { track3Solo = !track3Solo }, label = { Text("Solo", fontSize = 10.sp) })
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = { track3Launcher.launch("audio/*") }) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick", fontSize = 11.sp)
                            }
                        }
                    }
                    Slider(value = track3Volume, onValueChange = { track3Volume = it }, valueRange = 0f..1.5f)
                    Text("Pan: ${"%.1f".format(track3Pan)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = track3Pan, onValueChange = { track3Pan = it }, valueRange = -1.0f..1.0f)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                mixedAudio = AudioMixer.mixTracks(
                                    listOf(
                                        AudioMixer.MixTrack(track1Audio, track1Volume, 0L, pan = track1Pan, isMuted = track1Mute, isSolo = track1Solo),
                                        AudioMixer.MixTrack(track2Audio, track2Volume, track2DelayMs.toLong(), pan = track2Pan, isMuted = track2Mute, isSolo = track2Solo),
                                        AudioMixer.MixTrack(track3Audio, track3Volume, 0L, pan = track3Pan, isMuted = track3Mute, isSolo = track3Solo)
                                    ),
                                    masterVolume = masterVolume
                                )
                                statusMessage = "Summed 3 tracks in parallel with soft saturation limiting."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remix")
                        }

                        Button(
                            onClick = {
                                val saved = manager.saveAudio(mixedAudio, "mixed_${System.currentTimeMillis() % 10000}")
                                statusMessage = "Saved mixed multi-track to library: ${saved.name}"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Mix")
                        }
                    }
                }
            }

            Text(statusMessage, color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
