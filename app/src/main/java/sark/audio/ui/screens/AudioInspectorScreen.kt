package sark.audio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInspectorScreen(
    onBack: () -> Unit
) {
    var activeAudio by remember {
        mutableStateOf(
            WavFileProcessor.generateSineWave(
                sampleRate = 48000,
                frequencyHz = 440.0,
                durationSeconds = 5.25,
                channels = 2,
                amplitude = 0.75f
            )
        )
    }

    var metadata by remember {
        mutableStateOf(AudioMetadataInspector.inspectAudioData(activeAudio, "master_take_01.wav"))
    }

    var targetSampleRate by remember { mutableStateOf(44100) }
    var targetChannels by remember { mutableStateOf(1) }
    var conversionStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeAudio) {
        metadata = AudioMetadataInspector.inspectAudioData(activeAudio, "master_take_01.wav")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Inspector & Converter") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio File Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("FILE SPECIFICATIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    MetadataRow(icon = Icons.Default.AudioFile, label = "File Name", value = metadata.fileName)
                    MetadataRow(icon = Icons.Default.Category, label = "Format", value = metadata.format)
                    MetadataRow(icon = Icons.Default.Timer, label = "Duration", value = metadata.durationFormatted)
                    MetadataRow(icon = Icons.Default.GraphicEq, label = "Sample Rate", value = "${metadata.sampleRate} Hz")
                    MetadataRow(icon = Icons.Default.Speaker, label = "Channels", value = if (metadata.channels == 2) "Stereo (2)" else "Mono (1)")
                    MetadataRow(icon = Icons.Default.Numbers, label = "Bit Depth", value = "${metadata.bitsPerSample}-bit PCM")
                    MetadataRow(icon = Icons.Default.Speed, label = "Bitrate", value = "${metadata.bitRateKbps} kbps")
                    MetadataRow(icon = Icons.Default.Storage, label = "File Size", value = metadata.fileSizeFormatted)
                }
            }

            // Audio Converter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("FORMAT & SPEC CONVERTER", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Target Sample Rate", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(22050, 44100, 48000).forEach { rate ->
                            FilterChip(
                                selected = targetSampleRate == rate,
                                onClick = { targetSampleRate = rate },
                                label = { Text("$rate Hz") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Target Channels", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = targetChannels == 1,
                            onClick = { targetChannels = 1 },
                            label = { Text("Mono") }
                        )
                        FilterChip(
                            selected = targetChannels == 2,
                            onClick = { targetChannels = 2 },
                            label = { Text("Stereo") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (conversionStatus != null) {
                        Text(
                            text = conversionStatus ?: "",
                            color = AccentTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            var converted = activeAudio
                            if (converted.header.sampleRate != targetSampleRate) {
                                converted = AudioTimePitchProcessor.resample(converted, targetSampleRate)
                            }
                            if (converted.header.channels != targetChannels) {
                                converted = if (targetChannels == 2) {
                                    AudioMerger.monoToStereo(converted)
                                } else {
                                    AudioMerger.stereoToMono(converted)
                                }
                            }
                            activeAudio = converted
                            conversionStatus = "Audio successfully converted to $targetSampleRate Hz, ${if (targetChannels == 2) "Stereo" else "Mono"}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convert Audio Format")
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = TextSecondary, fontSize = 14.sp)
        }
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
