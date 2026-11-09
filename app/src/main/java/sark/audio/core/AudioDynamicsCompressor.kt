package sark.audio.core

import kotlin.math.*

/**
 * 14. Audio Dynamics Compressor
 * Professional audio dynamic range compression with threshold, ratio, attack, release, and makeup gain.
 */
object AudioDynamicsCompressor {

    /**
     * Applies dynamic range compression.
     * @param thresholdDb Level in dB above which compression starts (e.g. -18.0 dB)
     * @param ratio Compression ratio (e.g. 4.0 for 4:1)
     * @param attackMs Attack time in milliseconds (e.g. 20 ms)
     * @param releaseMs Release time in milliseconds (e.g. 150 ms)
     * @param makeupGainDb Gain applied after compression to compensate for level reduction
     */
    fun compress(
        audioData: AudioData,
        thresholdDb: Double = -18.0,
        ratio: Double = 4.0,
        attackMs: Double = 20.0,
        releaseMs: Double = 150.0,
        makeupGainDb: Double = 3.0
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels

        val attackAlpha = exp(-1.0 / (sampleRate * (attackMs / 1000.0).coerceAtLeast(0.001)))
        val releaseAlpha = exp(-1.0 / (sampleRate * (releaseMs / 1000.0).coerceAtLeast(0.001)))
        val makeupLinear = 10.0.pow(makeupGainDb / 20.0)

        val output = ShortArray(audioData.samples.size)
        var envelopeDb = -96.0

        for (f in 0 until totalFrames) {
            // Find max peak across channels for sidechain detection
            var maxPeak = 0.00001
            for (c in 0 until channels) {
                val absVal = abs(audioData.samples[f * channels + c].toDouble()) / Short.MAX_VALUE
                if (absVal > maxPeak) maxPeak = absVal
            }

            val inputDb = 20.0 * log10(maxPeak.coerceAtLeast(1e-5))

            // Peak envelope follower
            envelopeDb = if (inputDb > envelopeDb) {
                attackAlpha * envelopeDb + (1.0 - attackAlpha) * inputDb
            } else {
                releaseAlpha * envelopeDb + (1.0 - releaseAlpha) * inputDb
            }

            // Gain computer
            var gainReductionDb = 0.0
            if (envelopeDb > thresholdDb) {
                val overshoot = envelopeDb - thresholdDb
                gainReductionDb = overshoot * (1.0 - (1.0 / ratio.coerceAtLeast(1.0)))
            }

            val gainLinear = 10.0.pow(-gainReductionDb / 20.0) * makeupLinear

            for (c in 0 until channels) {
                val idx = f * channels + c
                val compressedSample = (audioData.samples[idx] * gainLinear).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                output[idx] = compressedSample
            }
        }

        return audioData.copy(samples = output)
    }
}
