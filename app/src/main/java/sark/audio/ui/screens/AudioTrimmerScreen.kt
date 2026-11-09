package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import sark.audio.core.*
import sark.audio.ui.components.AudioFileSelectorBar
import sark.audio.ui.components.VideoStyleTimelineView
import sark.audio.ui.components.WaveformVisualizer
import sark.audio.ui.theme.*
import java.io.File
import kotlin.math.max

/**
 * Pro Video-Editor Style Audio Timeline Editor Screen.
 * Complete feature set:
 * - Multi-track Timeline with Interactive Playhead and Ruler
 * - Razor Split at playhead
 * - Sample-accurate trimming & slicing
 * - Speed (0.25x - 4x) & Pitch Transposition (-12 to +12 semitones)
 * - Gain / Volume scaling & Constant-power Panning
 * - In-place Audio Reversal
 * - Fade-In and Fade-Out envelopes (S-Curve & Linear)
 * - Multi-level Undo & Redo History
 * - Direct Export / Save to Library
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AudioFileManager(File(context.filesDir, "audio_library")) }
    val player = remember { AudioPlaybackController() }
    val playbackState by player.playbackState.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()

    var currentFileName by remember { mutableStateOf("sample_master.wav") }

    // Initial audio setup
    val initialAudio = remember {
        WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 6.0,
            channels = 2
        )
    }

    // Timeline Project State
    var timelineProject by remember {
        val initialClip = AudioTimelineEngine.TimelineClip(
            id = "clip_1",
            name = "Main Lead",
            audioData = initialAudio,
            timelineStartMs = 0L,
            durationMs = initialAudio.header.durationMs
        )
        val initialTrack = AudioTimelineEngine.TimelineTrack(
            id = "track_1",
            name = "Audio Track 1",
            clips = listOf(initialClip)
        )
        mutableStateOf(
            AudioTimelineEngine.TimelineProject(
                tracks = listOf(initialTrack)
            )
        )
    }

    var selectedClipId by remember { mutableStateOf<String?>("clip_1") }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var statusMessage by remember { mutableStateOf<String?>("Timeline editor ready.") }

    // Undo / Redo History Stacks
    val undoStack = remember { mutableStateListOf<AudioTimelineEngine.TimelineProject>() }
    val redoStack = remember { mutableStateListOf<AudioTimelineEngine.TimelineProject>() }

    fun pushHistory(newProject: AudioTimelineEngine.TimelineProject) {
        undoStack.add(timelineProject)
        redoStack.clear()
        timelineProject = newProject
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(timelineProject)
            timelineProject = prev
            statusMessage = "Undo performed."
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(timelineProject)
            timelineProject = next
            statusMessage = "Redo performed."
        }
    }

    // Currently rendered master preview audio for playback
    var masterAudio by remember {
        mutableStateOf(AudioTimelineEngine.renderProject(timelineProject))
    }

    LaunchedEffect(timelineProject) {
        masterAudio = AudioTimelineEngine.renderProject(timelineProject)
        player.load(masterAudio)
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    val totalDurationMs = max(timelineProject.totalDurationMs, 1000L)

    // Find currently selected clip
    val selectedClip = remember(timelineProject, selectedClipId) {
        timelineProject.tracks.flatMap { it.clips }.find { it.id == selectedClipId }
    }

    fun updateSelectedClip(commitHistory: Boolean = true, transform: (AudioTimelineEngine.TimelineClip) -> AudioTimelineEngine.TimelineClip) {
        val selId = selectedClipId ?: return
        val newTracks = timelineProject.tracks.map { track ->
            val newClips = track.clips.map { clip ->
                if (clip.id == selId) transform(clip) else clip
            }
            track.copy(clips = newClips)
        }
        if (commitHistory) {
            pushHistory(timelineProject.copy(tracks = newTracks))
        } else {
            timelineProject = timelineProject.copy(tracks = newTracks)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pro Audio Timeline Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Multi-Track Timeline • Video Editor Style",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                    // Redo Button
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                    // Export / Save Button
                    IconButton(
                        onClick = {
                            val rendered = AudioTimelineEngine.renderProject(timelineProject)
                            val saved = manager.saveAudio(rendered, "timeline_export_${System.currentTimeMillis() % 10000}")
                            statusMessage = "Exported master mix to library: ${saved.name}"
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = AccentCoral)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // File Selector & Import Bar
            AudioFileSelectorBar(
                currentFileName = currentFileName,
                buttonText = "Import Clip",
                onAudioLoaded = { loaded ->
                    currentFileName = loaded.fileName
                    val newClip = AudioTimelineEngine.TimelineClip(
                        id = "clip_${System.currentTimeMillis() % 10000}",
                        name = loaded.fileName.substringBeforeLast("."),
                        audioData = loaded.audioData,
                        timelineStartMs = currentPositionMs,
                        durationMs = loaded.audioData.header.durationMs
                    )
                    val targetTrack = timelineProject.tracks.firstOrNull() ?: AudioTimelineEngine.TimelineTrack("track_1", "Track 1")
                    val updatedTracks = if (timelineProject.tracks.isEmpty()) {
                        listOf(targetTrack.copy(clips = listOf(newClip)))
                    } else {
                        timelineProject.tracks.mapIndexed { index, tr ->
                            if (index == 0) tr.copy(clips = tr.clips + newClip) else tr
                        }
                    }
                    pushHistory(timelineProject.copy(tracks = updatedTracks))
                    selectedClipId = newClip.id
                    statusMessage = "Imported clip: ${loaded.fileName}"
                }
            )

            // Transport Control & Timecode Display Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timecode: Current / Total
                    Column {
                        val curSec = currentPositionMs / 1000
                        val curFrac = (currentPositionMs % 1000) / 10
                        val totSec = totalDurationMs / 1000
                        val totFrac = (totalDurationMs % 1000) / 10

                        Text(
                            text = "%02d:%02d.%02d".format(curSec / 60, curSec % 60, curFrac),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AccentCoral
                        )
                        Text(
                            text = "/ %02d:%02d.%02d".format(totSec / 60, totSec % 60, totFrac),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Transport Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Jump Start
                        IconButton(
                            onClick = { player.seekTo(0L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Start", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Step Back 1s
                        IconButton(
                            onClick = { player.seekTo(max(0L, currentPositionMs - 1000L)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = "-1s", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Play / Pause Large Button
                        IconButton(
                            onClick = {
                                if (playbackState == AudioPlaybackController.PlaybackState.Playing) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        ) {
                            Icon(
                                imageVector = if (playbackState == AudioPlaybackController.PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        // Step Forward 1s
                        IconButton(
                            onClick = { player.seekTo((currentPositionMs + 1000L).coerceAtMost(totalDurationMs)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "+1s", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Stop
                        IconButton(
                            onClick = { player.stop() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Zoom Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${"%.1f".format(zoomScale)}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoLight
                        )
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Timeline View (Ruler + Tracks + Playhead)
            VideoStyleTimelineView(
                project = timelineProject,
                currentPositionMs = currentPositionMs,
                zoomScale = zoomScale,
                selectedClipId = selectedClipId,
                onClipSelected = { selectedClipId = it },
                onSeek = { seekMs -> player.seekTo(seekMs) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Video-Editor Style Quick Action Toolbar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Split / Razor at Playhead
                    IconButton(
                        onClick = {
                            val selClip = selectedClip ?: return@IconButton
                            if (currentPositionMs > selClip.timelineStartMs && currentPositionMs < selClip.timelineEndMs) {
                                val (c1, c2) = AudioTimelineEngine.splitClip(selClip, currentPositionMs)
                                val newTracks = timelineProject.tracks.map { track ->
                                    val newClips = track.clips.flatMap { clip ->
                                        if (clip.id == selClip.id) listOf(c1, c2) else listOf(clip)
                                    }
                                    track.copy(clips = newClips)
                                }
                                pushHistory(timelineProject.copy(tracks = newTracks))
                                selectedClipId = c2.id
                                statusMessage = "Split clip at playhead."
                            } else {
                                statusMessage = "Move playhead over selected clip to split."
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Split", tint = AccentCoral, modifier = Modifier.size(20.dp))
                            Text("Split", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Duplicate Clip
                    IconButton(
                        onClick = {
                            val selClip = selectedClip ?: return@IconButton
                            val dupClip = selClip.copy(
                                id = "clip_${System.currentTimeMillis() % 10000}",
                                name = "${selClip.name} (Copy)",
                                timelineStartMs = selClip.timelineEndMs
                            )
                            val newTracks = timelineProject.tracks.map { tr ->
                                if (tr.clips.any { it.id == selClip.id }) tr.copy(clips = tr.clips + dupClip) else tr
                            }
                            pushHistory(timelineProject.copy(tracks = newTracks))
                            selectedClipId = dupClip.id
                            statusMessage = "Duplicated clip."
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = AccentTeal, modifier = Modifier.size(20.dp))
                            Text("Duplicate", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Reverse Clip
                    IconButton(
                        onClick = {
                            updateSelectedClip { it.copy(isReversed = !it.isReversed) }
                            statusMessage = "Toggled reverse on selected clip."
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Reverse", tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Text("Reverse", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Mute / Unmute Clip
                    IconButton(
                        onClick = {
                            updateSelectedClip { it.copy(isMuted = !it.isMuted) }
                            statusMessage = "Toggled mute on selected clip."
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (selectedClip?.isMuted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = PrimaryIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(if (selectedClip?.isMuted == true) "Unmute" else "Mute", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Delete Clip
                    IconButton(
                        onClick = {
                            val selClip = selectedClip ?: return@IconButton
                            val newTracks = timelineProject.tracks.map { tr ->
                                tr.copy(clips = tr.clips.filter { it.id != selClip.id })
                            }
                            pushHistory(timelineProject.copy(tracks = newTracks))
                            selectedClipId = timelineProject.tracks.flatMap { it.clips }.firstOrNull()?.id
                            statusMessage = "Deleted clip."
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AccentCoral, modifier = Modifier.size(20.dp))
                            Text("Delete", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Clip Inspector & Adjustment Card (Shows when a clip is selected)
            if (selectedClip != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "CLIP INSPECTOR: ${selectedClip.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigoLight,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Volume Gain Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Volume: ${(selectedClip.volume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(95.dp))
                            var liveVol by remember(selectedClip.id, selectedClip.volume) { mutableFloatStateOf(selectedClip.volume) }
                            Slider(
                                value = liveVol,
                                onValueChange = { liveVol = it },
                                onValueChangeFinished = {
                                    updateSelectedClip(commitHistory = true) { it.copy(volume = liveVol) }
                                },
                                valueRange = 0f..2.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Pan Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var livePan by remember(selectedClip.id, selectedClip.pan) { mutableFloatStateOf(selectedClip.pan) }
                            val panTxt = when {
                                livePan < -0.05f -> "L ${(-livePan * 100).toInt()}%"
                                livePan > 0.05f -> "R ${(livePan * 100).toInt()}%"
                                else -> "Center"
                            }
                            Text("Pan: $panTxt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(95.dp))
                            Slider(
                                value = livePan,
                                onValueChange = { livePan = it },
                                onValueChangeFinished = {
                                    updateSelectedClip(commitHistory = true) { it.copy(pan = livePan) }
                                },
                                valueRange = -1.0f..1.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Speed Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var liveSpeed by remember(selectedClip.id, selectedClip.speed) { mutableFloatStateOf(selectedClip.speed) }
                            Text("Speed: ${"%.2f".format(liveSpeed)}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(95.dp))
                            Slider(
                                value = liveSpeed,
                                onValueChange = { liveSpeed = it },
                                onValueChangeFinished = {
                                    updateSelectedClip(commitHistory = true) { it.copy(speed = liveSpeed) }
                                },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Pitch Semitone Transpose
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pitch Transpose:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { updateSelectedClip { it.copy(pitchSemitones = (it.pitchSemitones - 1).coerceAtLeast(-12)) } },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-1 Semitone")
                                }
                                Text(
                                    text = "${if (selectedClip.pitchSemitones > 0) "+" else ""}${selectedClip.pitchSemitones} st",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { updateSelectedClip { it.copy(pitchSemitones = (it.pitchSemitones + 1).coerceAtMost(12)) } },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+1 Semitone")
                                }
                            }
                        }

                        // Fade In / Fade Out Envelopes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                var liveFadeIn by remember(selectedClip.id, selectedClip.fadeInMs) { mutableFloatStateOf(selectedClip.fadeInMs.toFloat()) }
                                Text("Fade In: ${liveFadeIn.toLong()}ms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = liveFadeIn,
                                    onValueChange = { liveFadeIn = it },
                                    onValueChangeFinished = {
                                        updateSelectedClip(commitHistory = true) { it.copy(fadeInMs = liveFadeIn.toLong()) }
                                    },
                                    valueRange = 0f..2000f
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                var liveFadeOut by remember(selectedClip.id, selectedClip.fadeOutMs) { mutableFloatStateOf(selectedClip.fadeOutMs.toFloat()) }
                                Text("Fade Out: ${liveFadeOut.toLong()}ms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = liveFadeOut,
                                    onValueChange = { liveFadeOut = it },
                                    onValueChangeFinished = {
                                        updateSelectedClip(commitHistory = true) { it.copy(fadeOutMs = liveFadeOut.toLong()) }
                                    },
                                    valueRange = 0f..2000f
                                )
                            }
                        }
                    }
                }
            }

            // Status message feedback
            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Bottom Actions: Render & Save
            Button(
                onClick = {
                    val rendered = AudioTimelineEngine.renderProject(timelineProject)
                    val saved = manager.saveAudio(rendered, "master_edit_${System.currentTimeMillis() % 10000}")
                    statusMessage = "Master rendered & saved to Audio Library: ${saved.name}"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Render & Save Master Track")
            }
        }
    }
}
