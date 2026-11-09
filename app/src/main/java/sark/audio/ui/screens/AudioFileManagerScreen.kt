package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFileManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()

    var filesList by remember { mutableStateOf(manager.listFiles()) }
    var currentPlayingFile by remember { mutableStateOf<String?>(null) }
    var renameDialogTarget by remember { mutableStateOf<SavedAudioFile?>(null) }
    var renameInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (filesList.isEmpty()) {
            // Seed a starter sample track
            val demo = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 3.0)
            manager.saveAudio(demo, "demo_take_01")
            filesList = manager.listFiles()
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Library Manager") },
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SAVED AUDIO FILES (${filesList.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val sample = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SAWTOOTH, 520.0, durationSeconds = 2.0)
                        manager.saveAudio(sample, "take_${System.currentTimeMillis() % 10000}")
                        filesList = manager.listFiles()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Sample", tint = AccentTeal)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audio recordings found in library.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filesList) { audioFile ->
                        val isPlaying = currentPlayingFile == audioFile.file.absolutePath && playbackState == AudioPlaybackController.PlaybackState.Playing

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            player.pause()
                                        } else {
                                            try {
                                                val data = WavFileProcessor.decodeWav(audioFile.file)
                                                player.load(data)
                                                player.play()
                                                currentPlayingFile = audioFile.file.absolutePath
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) AccentCoral else PrimaryIndigo)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(audioFile.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${audioFile.durationMs / 1000}s • ${audioFile.sampleRate} Hz • ${if (audioFile.channels == 2) "Stereo" else "Mono"} • ${audioFile.sizeBytes / 1024} KB",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        renameDialogTarget = audioFile
                                        renameInput = audioFile.name.removeSuffix(".wav")
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = {
                                        player.stop()
                                        manager.deleteFile(audioFile.file)
                                        filesList = manager.listFiles()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentCoral, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Rename Dialog
            if (renameDialogTarget != null) {
                AlertDialog(
                    onDismissRequest = { renameDialogTarget = null },
                    title = { Text("Rename Audio File") },
                    text = {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("File Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                renameDialogTarget?.let {
                                    manager.renameFile(it.file, renameInput)
                                    filesList = manager.listFiles()
                                }
                                renameDialogTarget = null
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { renameDialogTarget = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
