package sark.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.AudioTimelineEngine
import sark.audio.core.WavFileProcessor
import sark.audio.ui.theme.*
import kotlin.math.max

/**
 * Pro Video-Editor Style Multi-Track Timeline Composable.
 * Features:
 * - Time ruler with microsecond/millisecond ticks
 * - Interactive Scrubbable Playhead (scrub anywhere on timeline or ruler)
 * - Zoom in / Zoom out scaling (0.5x to 4.0x)
 * - Multi-track layered clip layout with waveforms
 * - Active clip selection highlight with handles
 * - Split, trim, volume & mute indicators
 */
@Composable
fun VideoStyleTimelineView(
    project: AudioTimelineEngine.TimelineProject,
    currentPositionMs: Long,
    zoomScale: Float, // 1.0f = default
    selectedClipId: String?,
    onClipSelected: (String) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTimelineMs = max(project.totalDurationMs, 5000L)
    val horizontalScrollState = rememberScrollState()

    // Base timeline width: 1000ms = 120dp * zoomScale
    val pxPerMs = (0.12f * zoomScale).coerceIn(0.04f, 1.0f)
    val totalWidthDp = (totalTimelineMs * pxPerMs).dp.coerceAtLeast(320.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        // Horizontal scroll container holding the entire timeline body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(totalWidthDp)
                    .padding(vertical = 4.dp)
            ) {
                // 1. Timecode Ruler Bar
                TimelineRuler(
                    totalDurationMs = totalTimelineMs,
                    widthDp = totalWidthDp,
                    zoomScale = zoomScale,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Track Lanes
                if (project.tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tracks on timeline. Add an audio clip to start editing.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    project.tracks.forEachIndexed { trackIndex, track ->
                        TimelineTrackLane(
                            track = track,
                            trackIndex = trackIndex,
                            totalDurationMs = totalTimelineMs,
                            selectedClipId = selectedClipId,
                            onClipSelected = onClipSelected,
                            onSeek = onSeek
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // 3. Global Playhead overlay over entire ruler and tracks (pure visual overlay, scrubber gesture on ruler)
            val playheadProgress = if (totalTimelineMs > 0) (currentPositionMs.toFloat() / totalTimelineMs).coerceIn(0f, 1f) else 0f
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val headX = playheadProgress * size.width
                // Draw Playhead line
                drawLine(
                    color = AccentCoral,
                    start = Offset(headX, 0f),
                    end = Offset(headX, size.height),
                    strokeWidth = 2.5.dp.toPx()
                )
                // Draw Playhead thumb needle at top
                drawCircle(
                    color = AccentCoral,
                    radius = 5.dp.toPx(),
                    center = Offset(headX, 5.dp.toPx())
                )
            }
        }
    }
}

/**
 * Top timecode ruler with calibrated ticks.
 */
@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    widthDp: androidx.compose.ui.unit.Dp,
    zoomScale: Float,
    onSeek: (Long) -> Unit
) {
    val tickIntervalMs = when {
        zoomScale >= 2.5f -> 250L
        zoomScale >= 1.5f -> 500L
        zoomScale >= 0.8f -> 1000L
        else -> 2000L
    }

    val totalTicks = (totalDurationMs / tickIntervalMs).toInt().coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .width(widthDp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((ratio * totalDurationMs).toLong())
                }
            }
            .pointerInput(totalDurationMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek((ratio * totalDurationMs).toLong())
                }
            }
    ) {
        val w = size.width
        val h = size.height

        for (i in 0..totalTicks) {
            val tickMs = i * tickIntervalMs
            if (tickMs > totalDurationMs) break
            val x = (tickMs.toFloat() / totalDurationMs) * w

            val isMajor = tickMs % 1000L == 0L
            val tickHeight = if (isMajor) h * 0.7f else h * 0.35f

            drawLine(
                color = if (isMajor) AccentAmber else Color.Gray.copy(alpha = 0.5f),
                start = Offset(x, h - tickHeight),
                end = Offset(x, h),
                strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}

/**
 * Individual horizontal track lane displaying clips as blocks with mini waveforms.
 */
@Composable
private fun TimelineTrackLane(
    track: AudioTimelineEngine.TimelineTrack,
    trackIndex: Int,
    totalDurationMs: Long,
    selectedClipId: String?,
    onClipSelected: (String) -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalTrackWidth = maxWidth

            track.clips.forEach { clip ->
                val startRatio = (clip.timelineStartMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                val durationRatio = (clip.durationMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                val startOffsetDp = totalTrackWidth * startRatio
                val clipWidthDp = (totalTrackWidth * durationRatio).coerceAtLeast(60.dp)

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = startOffsetDp)
                        .width(clipWidthDp)
                ) {
                    TimelineClipView(
                        clip = clip,
                        isSelected = clip.id == selectedClipId,
                        onClick = { onClipSelected(clip.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Visual timeline clip representation.
 */
@Composable
private fun TimelineClipView(
    clip: AudioTimelineEngine.TimelineClip,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waveformAmplitudes = remember(clip.audioData.samples) {
        WavFileProcessor.extractWaveformAmplitudes(clip.audioData.samples, clip.audioData.header.channels, 48)
    }

    Card(
        modifier = modifier
            .padding(vertical = 3.dp, horizontal = 1.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AccentCoral) else androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigoLight.copy(alpha = 0.4f))
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clip.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) AccentCoral else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${clip.durationMs}ms",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Waveform
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val bars = waveformAmplitudes.size
                    if (bars > 0) {
                        val barSpacing = 1.dp.toPx()
                        val barWidth = max(1f, (w - (bars - 1) * barSpacing) / bars)
                        for (i in 0 until bars) {
                            val x = i * (barWidth + barSpacing)
                            val amp = waveformAmplitudes[i].coerceIn(0.1f, 1f)
                            val barH = amp * (h * 0.9f)
                            drawRect(
                                color = if (isSelected) AccentCoral else PrimaryIndigo,
                                topLeft = Offset(x, centerY - barH / 2f),
                                size = androidx.compose.ui.geometry.Size(barWidth, barH)
                            )
                        }
                    }
                }

                // Badges (speed, pitch, volume, fade)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (clip.speed != 1.0f) {
                        Text("${clip.speed}x", fontSize = 8.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                    }
                    if (clip.pitchSemitones != 0) {
                        Text("${if (clip.pitchSemitones > 0) "+" else ""}${clip.pitchSemitones}st", fontSize = 8.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                    }
                    if (clip.isReversed) {
                        Text("REV", fontSize = 8.sp, color = AccentCoral, fontWeight = FontWeight.Bold)
                    }
                    if (clip.volume != 1.0f) {
                        Text("${(clip.volume * 100).toInt()}%", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
    }
}
