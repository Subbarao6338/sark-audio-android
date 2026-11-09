package sark.audio.core

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.tanh

/**
 * 17. Audio Waveshaping & Distortion Processor
 * Non-linear soft-clipping, tube saturation, and fuzz overdrive distortion.
 */
object AudioDistortionProcessor {

    enum class DistortionMode {
        SOFT_CLIP, HARD_CLIP, FUZZ, TUBE_SATURATION
    }

    /**
     * Applies distortion waveshaping to audio.
     * @param drive Gain boost driving into the non-linear stage (1.0 to 10.0)
     * @param mix Wet/dry blend (0.0 to 1.0)
     */
    fun distort(
        audioData: AudioData,
        drive: Float = 3.0f,
        mode: DistortionMode = DistortionMode.SOFT_CLIP,
        mix: Float = 0.8f
    ): AudioData {
        val clampedDrive = drive.coerceAtLeast(1.0f)
        val clampedMix = mix.coerceIn(0f, 1f)
        val output = ShortArray(audioData.samples.size)

        for (i in audioData.samples.indices) {
            val dry = audioData.samples[i].toFloat() / Short.MAX_VALUE
            val driven = dry * clampedDrive

            val wet = when (mode) {
                DistortionMode.SOFT_CLIP -> {
                    // Hyperbolic tangent soft saturation
                    tanh(driven.toDouble()).toFloat()
                }
                DistortionMode.HARD_CLIP -> {
                    driven.coerceIn(-1.0f, 1.0f)
                }
                DistortionMode.FUZZ -> {
                    // Asymmetric fuzz
                    val s = sign(driven)
                    s * (1.0f - kotlin.math.exp(-abs(driven)))
                }
                DistortionMode.TUBE_SATURATION -> {
                    // Quadratic warm curve for positive, tanh for negative
                    if (driven > 0f) {
                        (driven - 0.25f * driven * driven).coerceIn(0f, 1.0f)
                    } else {
                        tanh(driven.toDouble()).toFloat()
                    }
                }
            }

            val blended = dry * (1.0f - clampedMix) + wet * clampedMix
            output[i] = (blended * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return audioData.copy(samples = output)
    }
}
