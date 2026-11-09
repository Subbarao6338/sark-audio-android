package sark.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import sark.audio.ui.theme.AccentCoral
import kotlin.math.max

/**
 * Interactive audio waveform visualizer supporting scrub seeking,
 * playback head tracking, and trimming boundary markers.
 * Dynamic theming adapts cleanly across Light and Nature theme modes.
 */
@Composable
fun WaveformVisualizer(
    amplitudes: FloatArray,
    playbackProgress: Float = 0f, // 0.0 to 1.0
    startMarkerProgress: Float? = null, // for trimming
    endMarkerProgress: Float? = null, // for trimming
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp),
    barColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    activeBarColor: Color = MaterialTheme.colorScheme.primary,
    markerColor: Color = AccentCoral,
    playheadColor: Color = MaterialTheme.colorScheme.onSurface,
    onSeek: ((Float) -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(ratio)
                    }
                }
            }
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(ratio)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        if (amplitudes.isEmpty()) {
            // Draw empty guideline
            drawLine(
                color = barColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val totalBars = amplitudes.size
        val barSpacing = 2.dp.toPx()
        val barWidth = max(2f, (width - (totalBars - 1) * barSpacing) / totalBars)

        for (i in 0 until totalBars) {
            val x = i * (barWidth + barSpacing)
            val normAmp = amplitudes[i].coerceIn(0.05f, 1f)
            val barHeight = normAmp * (height * 0.85f)
            val top = centerY - (barHeight / 2f)

            val barProgress = x / width
            val isPlayed = barProgress <= playbackProgress

            // Check if inside trimming window if markers are set
            val isInTrimRange = if (startMarkerProgress != null && endMarkerProgress != null) {
                barProgress in startMarkerProgress..endMarkerProgress
            } else {
                true
            }

            val color = when {
                !isInTrimRange -> barColor.copy(alpha = 0.3f)
                isPlayed -> activeBarColor
                else -> barColor
            }

            drawRect(
                color = color,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight)
            )
        }

        // Draw start trim marker
        if (startMarkerProgress != null) {
            val markerX = (startMarkerProgress * width).coerceIn(0f, width)
            drawLine(
                color = markerColor,
                start = Offset(markerX, 0f),
                end = Offset(markerX, height),
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(
                color = markerColor,
                radius = 6.dp.toPx(),
                center = Offset(markerX, 10.dp.toPx())
            )
        }

        // Draw end trim marker
        if (endMarkerProgress != null) {
            val markerX = (endMarkerProgress * width).coerceIn(0f, width)
            drawLine(
                color = markerColor,
                start = Offset(markerX, 0f),
                end = Offset(markerX, height),
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(
                color = markerColor,
                radius = 6.dp.toPx(),
                center = Offset(markerX, height - 10.dp.toPx())
            )
        }

        // Draw Playhead
        val playheadX = (playbackProgress * width).coerceIn(0f, width)
        drawLine(
            color = playheadColor,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, height),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}
