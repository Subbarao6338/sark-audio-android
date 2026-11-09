package sark.audio.core

import kotlin.math.floor
import kotlin.math.min

/**
 * High quality linear interpolation resampler and tempo/speed modifier.
 */
object AudioTimePitchProcessor {

    /**
     * Resample audio data to a new sample rate using linear interpolation.
     */
    fun resample(audioData: AudioData, targetSampleRate: Int): AudioData {
        if (audioData.header.sampleRate == targetSampleRate) return audioData

        val srcRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val srcFrames = audioData.samples.size / channels
        val targetFrames = ((srcFrames.toLong() * targetSampleRate) / srcRate).toInt()

        val newSamples = ShortArray(targetFrames * channels)
        val ratio = srcRate.toDouble() / targetSampleRate.toDouble()

        for (frame in 0 until targetFrames) {
            val srcPos = frame * ratio
            val srcIndex = floor(srcPos).toInt()
            val fraction = (srcPos - srcIndex).toFloat()
            val nextIndex = min(srcIndex + 1, srcFrames - 1)

            for (c in 0 until channels) {
                val s1 = audioData.samples[srcIndex * channels + c].toInt()
                val s2 = audioData.samples[nextIndex * channels + c].toInt()
                val interpolated = ((1f - fraction) * s1 + fraction * s2).toInt()
                newSamples[frame * channels + c] = interpolated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        val newHeader = audioData.header.copy(
            sampleRate = targetSampleRate,
            dataSize = newSamples.size * 2
        )
        return AudioData(newHeader, newSamples)
    }

    /**
     * Change speed/tempo by factor (e.g. 1.25x faster, 0.75x slower).
     * Uses synchronous time-domain sample stepping with linear interpolation.
     */
    fun changeSpeed(audioData: AudioData, speedMultiplier: Float): AudioData {
        if (speedMultiplier <= 0f || speedMultiplier == 1.0f) return audioData

        val channels = audioData.header.channels
        val srcFrames = audioData.samples.size / channels
        val targetFrames = (srcFrames / speedMultiplier).toInt()

        val newSamples = ShortArray(targetFrames * channels)

        for (frame in 0 until targetFrames) {
            val srcPos = frame * speedMultiplier.toDouble()
            val srcIndex = floor(srcPos).toInt()
            val fraction = (srcPos - srcIndex).toFloat()
            val nextIndex = min(srcIndex + 1, srcFrames - 1)

            for (c in 0 until channels) {
                val s1 = audioData.samples[srcIndex * channels + c].toInt()
                val s2 = audioData.samples[nextIndex * channels + c].toInt()
                val interpolated = ((1f - fraction) * s1 + fraction * s2).toInt()
                newSamples[frame * channels + c] = interpolated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        return AudioData(
            header = audioData.header.copy(dataSize = newSamples.size * 2),
            samples = newSamples
        )
    }

    /**
     * Pitch shift using varispeed resampling coupled with inverse duration compensation.
     */
    fun changePitch(audioData: AudioData, pitchFactor: Float): AudioData {
        if (pitchFactor <= 0f || pitchFactor == 1.0f) return audioData
        // Resample by factor to shift pitch, then keep sample rate header
        val originalSampleRate = audioData.header.sampleRate
        val resampled = resample(audioData, (originalSampleRate / pitchFactor).toInt().coerceIn(4000, 192000))
        return resampled.copy(header = resampled.header.copy(sampleRate = originalSampleRate))
    }

    /**
     * Reverse audio in place.
     */
    fun reverse(audioData: AudioData): AudioData {
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val reversed = ShortArray(audioData.samples.size)

        for (f in 0 until totalFrames) {
            val srcF = totalFrames - 1 - f
            for (c in 0 until channels) {
                reversed[f * channels + c] = audioData.samples[srcF * channels + c]
            }
        }

        return AudioData(audioData.header, reversed)
    }
}
