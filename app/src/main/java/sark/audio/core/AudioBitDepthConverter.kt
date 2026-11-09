package sark.audio.core

import kotlin.random.Random

/**
 * 20. Audio Bit Depth Converter & Dither
 * Converts between 8-bit unsigned PCM and 16-bit signed PCM with optional Triangular Dithering (TPDF).
 */
object AudioBitDepthConverter {

    /**
     * Converts 16-bit signed PCM [AudioData] to an 8-bit unsigned PCM representation.
     */
    fun convertTo8Bit(audioData: AudioData, useDither: Boolean = true): ByteArray {
        val samples = audioData.samples
        val bytes = ByteArray(samples.size)

        for (i in samples.indices) {
            var sample = samples[i].toFloat()
            if (useDither) {
                // Triangular Probability Density Function (TPDF) dither
                val r1 = Random.nextFloat() - 0.5f
                val r2 = Random.nextFloat() - 0.5f
                sample += (r1 + r2) * 128f
            }

            // Map [-32768, 32767] to [0, 255]
            val mapped = ((sample / 256.0f) + 128.0f).toInt().coerceIn(0, 255)
            bytes[i] = mapped.toByte()
        }
        return bytes
    }

    /**
     * Converts 8-bit unsigned PCM byte array to 16-bit signed PCM [AudioData].
     */
    fun convertFrom8Bit(
        bytes: ByteArray,
        sampleRate: Int = 44100,
        channels: Int = 1
    ): AudioData {
        val samples = ShortArray(bytes.size)
        for (i in bytes.indices) {
            val unsigned = bytes[i].toInt() and 0xFF
            samples[i] = ((unsigned - 128) * 256).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val header = WavHeader(
            channels = channels,
            sampleRate = sampleRate,
            bitsPerSample = 16,
            dataSize = samples.size * 2
        )
        return AudioData(header, samples)
    }
}
