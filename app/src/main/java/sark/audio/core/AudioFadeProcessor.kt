package sark.audio.core

import kotlin.math.PI
import kotlin.math.sin

/**
 * 5. Audio Fade Processor
 * Computes Linear and S-curve (logarithmic/smooth) Fade-In and Fade-Out on PCM audio.
 */
object AudioFadeProcessor {

    enum class FadeCurve {
        LINEAR, S_CURVE, EXPONENTIAL, LOGARITHMIC
    }

    /**
     * Applies a fade-in at the beginning of the audio.
     */
    fun fadeIn(
        audioData: AudioData,
        durationMs: Long,
        curve: FadeCurve = FadeCurve.LINEAR
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val fadeFrames = ((durationMs * sampleRate) / 1000L).toInt().coerceIn(0, totalFrames)

        val newSamples = audioData.samples.clone()

        for (i in 0 until fadeFrames) {
            val progress = i.toFloat() / fadeFrames
            val gain = calculateGain(progress, curve)
            for (c in 0 until channels) {
                val idx = i * channels + c
                newSamples[idx] = (newSamples[idx] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        return audioData.copy(samples = newSamples)
    }

    /**
     * Applies a fade-out at the end of the audio.
     */
    fun fadeOut(
        audioData: AudioData,
        durationMs: Long,
        curve: FadeCurve = FadeCurve.LINEAR
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val fadeFrames = ((durationMs * sampleRate) / 1000L).toInt().coerceIn(0, totalFrames)
        val startFrame = totalFrames - fadeFrames

        val newSamples = audioData.samples.clone()

        for (i in 0 until fadeFrames) {
            val progress = 1.0f - (i.toFloat() / fadeFrames)
            val gain = calculateGain(progress, curve)
            val frameIndex = startFrame + i
            for (c in 0 until channels) {
                val idx = frameIndex * channels + c
                newSamples[idx] = (newSamples[idx] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        return audioData.copy(samples = newSamples)
    }

    private fun calculateGain(progress: Float, curve: FadeCurve): Float {
        return when (curve) {
            FadeCurve.LINEAR -> progress
            FadeCurve.S_CURVE -> {
                // Quarter-sine wave smooth power curve
                sin(progress * (PI / 2.0)).toFloat().coerceIn(0f, 1f)
            }
            FadeCurve.EXPONENTIAL -> {
                // Smooth exponential rise
                (progress * progress).coerceIn(0f, 1f)
            }
            FadeCurve.LOGARITHMIC -> {
                // Perceptually uniform logarithmic curve
                kotlin.math.sqrt(progress.toDouble()).toFloat().coerceIn(0f, 1f)
            }
        }
    }
}
