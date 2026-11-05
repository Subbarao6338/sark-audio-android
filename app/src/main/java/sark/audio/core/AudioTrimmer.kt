package sark.audio.core

import kotlin.math.max
import kotlin.math.min

/**
 * High-performance audio trimming and cutting processor.
 * Allows sample-accurate trimming by milliseconds or sample index.
 */
object AudioTrimmer {

    /**
     * Trim audio from startMs to endMs.
     */
    fun trim(audioData: AudioData, startMs: Long, endMs: Long): AudioData {
        val totalMs = audioData.header.durationMs
        val clampedStartMs = max(0L, min(startMs, totalMs))
        val clampedEndMs = max(clampedStartMs, min(endMs, totalMs))

        val channels = audioData.header.channels
        val sampleRate = audioData.header.sampleRate

        val startFrame = ((clampedStartMs * sampleRate) / 1000L).toInt()
        val endFrame = ((clampedEndMs * sampleRate) / 1000L).toInt()

        val startSample = (startFrame * channels).coerceIn(0, audioData.samples.size)
        val endSample = (endFrame * channels).coerceIn(startSample, audioData.samples.size)

        val trimmedSamples = audioData.samples.copyOfRange(startSample, endSample)

        return AudioData(
            header = audioData.header.copy(
                dataSize = trimmedSamples.size * 2
            ),
            samples = trimmedSamples
        )
    }

    /**
     * Cut out a middle section and join remaining left and right segments.
     */
    fun cutOut(audioData: AudioData, cutStartMs: Long, cutEndMs: Long): AudioData {
        val channels = audioData.header.channels
        val sampleRate = audioData.header.sampleRate

        val startFrame = ((cutStartMs * sampleRate) / 1000L).toInt()
        val endFrame = ((cutEndMs * sampleRate) / 1000L).toInt()

        val startSample = (startFrame * channels).coerceIn(0, audioData.samples.size)
        val endSample = (endFrame * channels).coerceIn(startSample, audioData.samples.size)

        val part1 = audioData.samples.copyOfRange(0, startSample)
        val part2 = audioData.samples.copyOfRange(endSample, audioData.samples.size)

        val combined = ShortArray(part1.size + part2.size)
        System.arraycopy(part1, 0, combined, 0, part1.size)
        System.arraycopy(part2, 0, combined, part1.size, part2.size)

        return AudioData(
            header = audioData.header.copy(
                dataSize = combined.size * 2
            ),
            samples = combined
        )
    }
}
