package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAnalyzerScreen(
    onBack: () -> Unit
) {
    var currentFileName by remember { mutableStateOf("demo_tone_520Hz.wav") }
    var activeAudio by remember {
        mutableStateOf(
            AudioToneGenerator.generateTone(
                AudioToneGenerator.WaveformType.SINE, 520.0, durationSeconds = 1.0, channels = 1
            )
        )
    }

    var spectrumResult by remember {
        mutableStateOf(AudioSpectrumAnalyzer.analyze(activeAudio, fftSize = 512))
    }

    var tunerResult by remember {
        mutableStateOf(AudioTunerEngine.analyzePitch(activeAudio))
    }

    var testFreq by remember { mutableStateOf(440f) }

    LaunchedEffect(activeAudio) {
        spectrumResult = AudioSpectrumAnalyzer.analyze(activeAudio, fftSize = 512)
        tunerResult = AudioTunerEngine.analyzePitch(activeAudio)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spectrum Analyzer & Tuner") },
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
                }
            )

            // Tuner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CHROMATIC INSTRUMENT TUNER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${tunerResult.noteName}${if (tunerResult.octave > 0) tunerResult.octave else ""}",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tunerResult.isInTune) AccentTeal else AccentAmber
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detected: ${"%.1f".format(tunerResult.detectedHz)} Hz • Target: ${"%.1f".format(tunerResult.targetHz)} Hz",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${if (tunerResult.centsDeviation > 0) "+" else ""}${tunerResult.centsDeviation} Cents",
                        color = if (tunerResult.isInTune) AccentTeal else AccentCoral,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // FFT Spectrum Graph
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("COOLEY-TUKEY FFT SPECTRUM", style = MaterialTheme.typography.labelSmall, color = PrimaryIndigoLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Peak Frequency: ${spectrumResult.peakFrequencyHz.toInt()} Hz • RMS: ${"%.1f".format(spectrumResult.rmsDb)} dBFS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spectrum bar visualization (subset of 32 bins)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val numBars = 32
                        val step = (spectrumResult.magnitudesDb.size / numBars).coerceAtLeast(1)
                        for (i in 0 until numBars) {
                            val db = spectrumResult.magnitudesDb.getOrElse(i * step) { -96f }
                            val heightFraction = ((db + 96f) / 96f).coerceIn(0.05f, 1f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(PrimaryIndigo)
                            )
                        }
                    }
                }
            }

            // Test Tone Generator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("INJECT TEST SIGNAL", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Signal Frequency: ${testFreq.toInt()} Hz", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Slider(
                        value = testFreq,
                        onValueChange = { testFreq = it },
                        valueRange = 80f..2000f
                    )

                    Button(
                        onClick = {
                            activeAudio = AudioToneGenerator.generateTone(
                                AudioToneGenerator.WaveformType.SINE,
                                testFreq.toDouble(),
                                durationSeconds = 0.5
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Frequency")
                    }
                }
            }
        }
    }
}
