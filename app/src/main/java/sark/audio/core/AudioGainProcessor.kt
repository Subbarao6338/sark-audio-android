package sark.audio.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Volume amplification, attenuation, peak normalization, and fade processor.
 */
object AudioGainProcessor {

    /**
     * Apply gain multiplier (e.g. 1.0 = 100%, 2.0 = 200% volume).
     */
    fun applyGain(audioData: AudioData, multiplier: Float): AudioData {
        if (multiplier == 1.0f) return audioData

        val samples = audioData.samples
        val newSamples = ShortArray(samples.size)

        for (i in samples.indices) {
            val scaled = (samples[i] * multiplier).toInt()
            newSamples[i] = scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return AudioData(audioData.header, newSamples)
    }

    /**
     * Peak normalize audio so that peak matches targetPeak (0.0 to 1.0).
     */
    fun normalize(audioData: AudioData, targetPeak: Float = 0.95f): AudioData {
        var currentPeak = 0
        for (s in audioData.samples) {
            val v = abs(s.toInt())
            if (v > currentPeak) currentPeak = v
        }

        if (currentPeak == 0) return audioData

        val targetValue = (Short.MAX_VALUE * targetPeak).toInt()
        val factor = targetValue.toFloat() / currentPeak

        return applyGain(audioData, factor)
    }

    /**
     * Apply fade-in to the start and fade-out to the end of the audio track.
     */
    fun applyFade(audioData: AudioData, fadeInMs: Long, fadeOutMs: Long): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val samples = audioData.samples
        val newSamples = samples.copyOf()
        val totalFrames = samples.size / channels

        val fadeInFrames = min(totalFrames.toLong(), (fadeInMs * sampleRate) / 1000L).toInt()
        val fadeOutFrames = min(totalFrames.toLong(), (fadeOutMs * sampleRate) / 1000L).toInt()

        // Fade in
        if (fadeInFrames > 0) {
            for (f in 0 until fadeInFrames) {
                val factor = f.toFloat() / fadeInFrames
                for (c in 0 until channels) {
                    val idx = f * channels + c
                    newSamples[idx] = (samples[idx] * factor).toInt().toShort()
                }
            }
        }

        // Fade out
        if (fadeOutFrames > 0) {
            val startFrame = max(0, totalFrames - fadeOutFrames)
            for (f in startFrame until totalFrames) {
                val remaining = totalFrames - f
                val factor = remaining.toFloat() / fadeOutFrames
                for (c in 0 until channels) {
                    val idx = f * channels + c
                    newSamples[idx] = (samples[idx] * factor).toInt().toShort()
                }
            }
        }

        return AudioData(audioData.header, newSamples)
    }
}
