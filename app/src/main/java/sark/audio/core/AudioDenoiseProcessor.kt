package sark.audio.core

import kotlin.math.*

/**
 * Echo & Reverb Remover (Dereverberation)
 * Attenuates long room decay tails and echoes using spectral gating and transient envelope de-reverb.
 */
object AudioDenoiseProcessor {

    /**
     * Suppresses reverb tails and diffuse echoes.
     * @param reductionStrength 0.0 (off) to 1.0 (aggressive de-reverb)
     */
    fun removeReverbAndEcho(
        audioData: AudioData,
        reductionStrength: Float = 0.7f
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val strength = reductionStrength.coerceIn(0f, 1f)

        val output = ShortArray(audioData.samples.size)

        // Envelope tracker coefficients
        val fastAttackAlpha = exp(-1.0 / (sampleRate * 0.005)) // 5ms attack
        val slowReleaseAlpha = exp(-1.0 / (sampleRate * 0.120)) // 120ms release decay

        var fastEnv = 0.0
        var slowEnv = 0.0

        for (f in 0 until totalFrames) {
            var peak = 0.0
            for (c in 0 until channels) {
                val s = abs(audioData.samples[f * channels + c].toDouble()) / Short.MAX_VALUE
                if (s > peak) peak = s
            }

            // Fast envelope follows direct transients
            fastEnv = if (peak > fastEnv) {
                fastAttackAlpha * fastEnv + (1.0 - fastAttackAlpha) * peak
            } else {
                0.999 * fastEnv
            }

            // Slow envelope models the lingering reverb tail
            slowEnv = slowReleaseAlpha * slowEnv + (1.0 - slowReleaseAlpha) * peak

            // Gain suppression when fast direct energy has passed but slow reverb tail remains
            val transientToTailRatio = (fastEnv / (slowEnv + 1e-4)).coerceIn(0.0, 1.0)
            val dereverbGain = (1.0 - strength) + strength * transientToTailRatio

            for (c in 0 until channels) {
                val idx = f * channels + c
                val s = audioData.samples[idx].toDouble()
                output[idx] = (s * dereverbGain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        return audioData.copy(samples = output)
    }
}
