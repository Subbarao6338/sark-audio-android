package sark.audio.core

import kotlin.math.abs

/**
 * Track Splitter & Audio Markers
 * Splits audio files into multiple discrete tracks or cue markers by:
 * 1. Fixed duration segments (e.g. split into 30-second clips)
 * 2. Custom timestamp cue markers
 * 3. Automatic silence detection (splitting whenever silence exceeds threshold ms).
 */
object AudioTrackSplitter {

    data class AudioMarker(
        val timestampMs: Long,
        val label: String
    )

    /**
     * Splits audio by fixed chunk duration (in milliseconds).
     */
    fun splitByDuration(audioData: AudioData, segmentDurationMs: Long): List<AudioData> {
        val totalMs = audioData.header.durationMs
        if (segmentDurationMs <= 0 || totalMs <= segmentDurationMs) {
            return listOf(audioData)
        }

        val parts = mutableListOf<AudioData>()
        var start = 0L
        while (start < totalMs) {
            val end = (start + segmentDurationMs).coerceAtMost(totalMs)
            parts.add(AudioTrimmer.trim(audioData, start, end))
            start = end
        }
        return parts
    }

    /**
     * Splits audio at explicit cue markers.
     */
    fun splitAtMarkers(audioData: AudioData, markerTimestampsMs: List<Long>): List<AudioData> {
        val totalMs = audioData.header.durationMs
        val sortedMarkers = markerTimestampsMs.filter { it in 1 until totalMs }.distinct().sorted()
        if (sortedMarkers.isEmpty()) return listOf(audioData)

        val parts = mutableListOf<AudioData>()
        var prev = 0L
        for (m in sortedMarkers) {
            parts.add(AudioTrimmer.trim(audioData, prev, m))
            prev = m
        }
        if (prev < totalMs) {
            parts.add(AudioTrimmer.trim(audioData, prev, totalMs))
        }
        return parts
    }

    /**
     * Automatically splits audio on silent pauses (e.g. dividing a podcast or song into separate tracks).
     */
    fun splitBySilence(
        audioData: AudioData,
        silenceThresholdDb: Double = -40.0,
        minSilenceDurationMs: Long = 400L
    ): List<AudioData> {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val ampThreshold = AudioSilenceTrimmer.dBToThreshold(silenceThresholdDb)
        val minSilenceFrames = ((minSilenceDurationMs * sampleRate) / 1000L).toInt()

        val splitPointsMs = mutableListOf<Long>()
        var consecutiveSilentFrames = 0
        var silenceStartFrame = -1

        for (f in 0 until totalFrames) {
            var peak = 0
            for (c in 0 until channels) {
                val s = abs(audioData.samples[f * channels + c].toInt())
                if (s > peak) peak = s
            }

            if (peak < ampThreshold) {
                if (silenceStartFrame == -1) silenceStartFrame = f
                consecutiveSilentFrames++
            } else {
                if (consecutiveSilentFrames >= minSilenceFrames && silenceStartFrame > 0) {
                    val midPointFrame = silenceStartFrame + (consecutiveSilentFrames / 2)
                    val ms = (midPointFrame.toLong() * 1000L) / sampleRate
                    splitPointsMs.add(ms)
                }
                silenceStartFrame = -1
                consecutiveSilentFrames = 0
            }
        }

        return if (splitPointsMs.isEmpty()) listOf(audioData) else splitAtMarkers(audioData, splitPointsMs)
    }
}
