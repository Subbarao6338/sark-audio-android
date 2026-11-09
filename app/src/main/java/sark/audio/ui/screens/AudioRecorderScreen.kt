package sark.audio.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.AudioPlaybackController
import sark.audio.core.AudioRecorderController
import sark.audio.core.WavFileProcessor
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorderController() }
    val player = remember { AudioPlaybackController() }

    val recorderState by recorder.state.collectAsState()
    val amplitude by recorder.amplitude.collectAsState()
    val elapsedMs by recorder.elapsedTimeMs.collectAsState()

    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    val amplitudeHistory = remember { mutableStateListOf<Float>() }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordedAmplitudes by remember { mutableStateOf<FloatArray?>(null) }

    LaunchedEffect(amplitude) {
        if (recorderState is AudioRecorderController.State.Recording) {
            amplitudeHistory.add(amplitude)
            if (amplitudeHistory.size > 60) {
                amplitudeHistory.removeAt(0)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.reset()
            player.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Recorder") },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live status & timer card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val statusText = when (recorderState) {
                        is AudioRecorderController.State.Idle -> "READY TO RECORD"
                        is AudioRecorderController.State.Recording -> "RECORDING LIVE"
                        is AudioRecorderController.State.Paused -> "RECORDING PAUSED"
                        is AudioRecorderController.State.Finished -> "RECORDING SAVED"
                        is AudioRecorderController.State.Error -> "RECORDING ERROR"
                    }

                    Text(
                        text = statusText,
                        color = if (recorderState is AudioRecorderController.State.Recording) AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val displaySeconds = elapsedMs / 1000
                    val minutes = displaySeconds / 60
                    val seconds = displaySeconds % 60
                    val millis = (elapsedMs % 1000) / 100

                    Text(
                        text = "%02d:%02d.%d".format(minutes, seconds, millis),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Waveform display
                    if (recorderState is AudioRecorderController.State.Finished && recordedAmplitudes != null) {
                        val totalDuration = (recordedFile?.length()?.minus(44) ?: 0L) / (44100 * 2) * 1000
                        val progress = if (totalDuration > 0) (currentPositionMs.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                        WaveformVisualizer(
                            amplitudes = recordedAmplitudes ?: FloatArray(0),
                            playbackProgress = progress,
                            onSeek = { ratio ->
                                player.seekTo((ratio * totalDuration).toLong())
                            }
                        )
                    } else {
                        val ampArray = if (amplitudeHistory.isEmpty()) FloatArray(50) { 0.05f } else amplitudeHistory.toFloatArray()
                        WaveformVisualizer(amplitudes = ampArray)
                    }
                }
            }

            // Preview controls if recording finished
            AnimatedVisibility(visible = recorderState is AudioRecorderController.State.Finished) {
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
                            contentDescription = "Preview Play",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = {
                            player.stop()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Recording Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                if (recorderState is AudioRecorderController.State.Recording || recorderState is AudioRecorderController.State.Paused) {
                    IconButton(
                        onClick = {
                            if (recorderState is AudioRecorderController.State.Recording) {
                                recorder.pauseRecording()
                            } else {
                                recorder.resumeRecording()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (recorderState is AudioRecorderController.State.Recording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pause/Resume",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Main Record / Stop Action Button
                val isRecording = recorderState is AudioRecorderController.State.Recording || recorderState is AudioRecorderController.State.Paused
                FloatingActionButton(
                    onClick = {
                        if (!isRecording) {
                            val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
                            amplitudeHistory.clear()
                            recordedFile = null
                            recorder.startRecording(file)
                        } else {
                            val savedFile = recorder.stopRecording()
                            recordedFile = savedFile
                            if (savedFile != null && savedFile.exists()) {
                                val audioData = WavFileProcessor.decodeWav(savedFile)
                                recordedAmplitudes = WavFileProcessor.extractWaveformAmplitudes(audioData.samples, audioData.header.channels, 60)
                                player.load(audioData)
                            }
                        }
                    },
                    containerColor = if (isRecording) AccentCoral else PrimaryIndigo,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Reset Button
                if (recorderState is AudioRecorderController.State.Finished) {
                    IconButton(
                        onClick = {
                            player.stop()
                            recorder.reset()
                            amplitudeHistory.clear()
                            recordedFile = null
                            recordedAmplitudes = null
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "New Recording", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
