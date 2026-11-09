package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
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

data class TrackItem(
    val id: String,
    val title: String,
    val audioData: AudioData,
    val waveform: FloatArray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMergerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    val trackList = remember {
        mutableStateListOf(
            TrackItem(
                id = "1",
                title = "Clip 1 - Intro Tone (440Hz)",
                audioData = WavFileProcessor.generateSineWave(44100, 440.0, 2.0, 1),
                waveform = WavFileProcessor.extractWaveformAmplitudes(
                    WavFileProcessor.generateSineWave(44100, 440.0, 2.0, 1).samples, 1, 40
                )
            ),
            TrackItem(
                id = "2",
                title = "Clip 2 - Harmony Tone (880Hz)",
                audioData = WavFileProcessor.generateSineWave(44100, 880.0, 1.5, 1),
                waveform = WavFileProcessor.extractWaveformAmplitudes(
                    WavFileProcessor.generateSineWave(44100, 880.0, 1.5, 1).samples, 1, 40
                )
            )
        )
    }

    var mergedAudio by remember { mutableStateOf<AudioData?>(null) }
    var mergedWaveform by remember { mutableStateOf<FloatArray?>(null) }
    var crossfadeMs by remember { mutableStateOf(200f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val loaded = AudioSystemLoader.loadFromUri(context, uri)
                val nextId = (trackList.size + 1).toString()
                trackList.add(
                    TrackItem(
                        id = nextId,
                        title = loaded.fileName,
                        audioData = loaded.audioData,
                        waveform = WavFileProcessor.extractWaveformAmplitudes(
                            loaded.audioData.samples,
                            loaded.audioData.header.channels,
                            40
                        )
                    )
                )
                statusMessage = "Added file: ${loaded.fileName}"
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Merger & Joiner") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TRACK SEQUENCER (${trackList.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                    Row {
                        Button(
                            onClick = { filePickerLauncher.launch("audio/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open File", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        TextButton(
                            onClick = {
                                val nextId = (trackList.size + 1).toString()
                                val freq = 300.0 + (trackList.size * 150)
                                val newTone = WavFileProcessor.generateSineWave(44100, freq, 1.5, 1)
                                trackList.add(
                                    TrackItem(
                                        id = nextId,
                                        title = "Clip $nextId - Synth (${freq.toInt()}Hz)",
                                        audioData = newTone,
                                        waveform = WavFileProcessor.extractWaveformAmplitudes(newTone.samples, 1, 40)
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Tone", color = PrimaryIndigoLight)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(trackList) { index, track ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = PrimaryIndigoLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("${track.audioData.header.durationMs}ms | ${track.audioData.header.sampleRate}Hz", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }

                                IconButton(
                                    onClick = {
                                        if (trackList.size > 1) {
                                            trackList.removeAt(index)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Crossfade setting
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Crossfade Transition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("${crossfadeMs.toInt()} ms", style = MaterialTheme.typography.bodySmall, color = AccentTeal)
                        }
                        Slider(
                            value = crossfadeMs,
                            onValueChange = { crossfadeMs = it },
                            valueRange = 0f..1000f
                        )
                    }
                }
            }

            // Merged preview and control panel
            Column {
                if (mergedAudio != null && mergedWaveform != null) {
                    val mAudio = mergedAudio!!
                    val totalMs = mAudio.header.durationMs
                    val progress = if (totalMs > 0) (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("MERGED PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            WaveformVisualizer(
                                amplitudes = mergedWaveform!!,
                                playbackProgress = progress,
                                onSeek = { ratio ->
                                    player.seekTo((ratio * totalMs).toLong())
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        ) {
                            Icon(
                                imageVector = if (playbackState == AudioPlaybackController.PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Merged",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = AccentTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val tracks = trackList.map { it.audioData }
                            val merged = AudioMerger.mergeTracks(tracks, crossfadeMs.toInt())
                            mergedAudio = merged
                            mergedWaveform = WavFileProcessor.extractWaveformAmplitudes(merged.samples, merged.header.channels, 60)
                            player.load(merged)
                            val saved = manager.saveAudio(merged, "merged_${System.currentTimeMillis() % 10000}")
                            statusMessage = "Merged ${tracks.size} tracks & saved to library: ${saved.name}"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Merge & Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
