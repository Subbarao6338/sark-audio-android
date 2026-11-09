package sark.audio.core

import kotlin.math.*

/**
 * 13. Audio Filter Processor
 * Biquad digital filters (Low-pass, High-pass, Band-pass, Peaking EQ)
 * based on Robert Bristow-Johnson's Audio EQ Cookbook formulas.
 */
object AudioFilterProcessor {

    enum class FilterType {
        LOW_PASS, HIGH_PASS, BAND_PASS, PEAKING_EQ
    }

    data class BiquadCoefficients(
        val b0: Double, val b1: Double, val b2: Double,
        val a1: Double, val a2: Double
    )

    fun calculateCoefficients(
        type: FilterType,
        cutoffHz: Double,
        q: Double,
        gainDb: Double,
        sampleRate: Int
    ): BiquadCoefficients {
        val w0 = 2.0 * PI * cutoffHz / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q.coerceAtLeast(0.01))
        val a = 10.0.pow(gainDb / 40.0)

        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var a0 = 1.0
        var a1 = 0.0
        var a2 = 0.0

        when (type) {
            FilterType.LOW_PASS -> {
                b0 = (1.0 - cosw0) / 2.0
                b1 = 1.0 - cosw0
                b2 = (1.0 - cosw0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosw0
                a2 = 1.0 - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1.0 + cosw0) / 2.0
                b1 = -(1.0 + cosw0)
                b2 = (1.0 + cosw0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosw0
                a2 = 1.0 - alpha
            }
            FilterType.BAND_PASS -> {
                b0 = alpha
                b1 = 0.0
                b2 = -alpha
                a0 = 1.0 + alpha
                a1 = -2.0 * cosw0
                a2 = 1.0 - alpha
            }
            FilterType.PEAKING_EQ -> {
                b0 = 1.0 + alpha * a
                b1 = -2.0 * cosw0
                b2 = 1.0 - alpha * a
                a0 = 1.0 + alpha / a
                a1 = -2.0 * cosw0
                a2 = 1.0 - alpha / a
            }
        }

        return BiquadCoefficients(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }

    /**
     * Applies the biquad IIR filter to an [AudioData] buffer.
     */
    fun applyFilter(
        audioData: AudioData,
        type: FilterType,
        cutoffHz: Double,
        q: Double = 0.7071,
        gainDb: Double = 0.0
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels

        val coeffs = calculateCoefficients(type, cutoffHz, q, gainDb, sampleRate)
        val output = ShortArray(audioData.samples.size)

        // Maintain delay history per channel
        val x1 = DoubleArray(channels)
        val x2 = DoubleArray(channels)
        val y1 = DoubleArray(channels)
        val y2 = DoubleArray(channels)

        for (f in 0 until totalFrames) {
            for (c in 0 until channels) {
                val idx = f * channels + c
                val x0 = audioData.samples[idx].toDouble() / Short.MAX_VALUE

                val y0 = coeffs.b0 * x0 + coeffs.b1 * x1[c] + coeffs.b2 * x2[c] - coeffs.a1 * y1[c] - coeffs.a2 * y2[c]

                x2[c] = x1[c]
                x1[c] = x0
                y2[c] = y1[c]
                y1[c] = y0

                val samplePcm = (y0 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                output[idx] = samplePcm
            }
        }

        return audioData.copy(samples = output)
    }
}
