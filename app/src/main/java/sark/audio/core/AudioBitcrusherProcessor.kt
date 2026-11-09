package sark.audio.core

import kotlin.math.roundToInt

/**
 * 16. Audio Bitcrusher & Sample Rate Decimator
 * Creates lo-fi retro aesthetic by reducing resolution to target bit depth and downsampling.
 */
object AudioBitcrusherProcessor {

    /**
     * Bitcrushing and downsampling lo-fi effect.
     * @param bitDepth Target resolution bits (1 to 16, e.g. 8 or 4 for arcade sound)
     * @param downsampleFactor Downsampling integer hold factor (1 = off, 2 = half rate, 8 = heavy lo-fi)
     */
    fun process(
        audioData: AudioData,
        bitDepth: Int = 8,
        downsampleFactor: Int = 4
    ): AudioData {
        val clampedBits = bitDepth.coerceIn(1, 16)
        val clampedFactor = downsampleFactor.coerceAtLeast(1)
        val levels = (1 shl clampedBits) - 1
        val halfLevels = levels / 2.0

        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val output = ShortArray(audioData.samples.size)

        val heldSample = ShortArray(channels)

        for (f in 0 until totalFrames) {
            val shouldUpdate = (f % clampedFactor == 0)

            for (c in 0 until channels) {
                val idx = f * channels + c
                if (shouldUpdate) {
                    val normalized = audioData.samples[idx].toDouble() / Short.MAX_VALUE
                    // Quantize to bit depth
                    val quantized = (normalized * halfLevels).roundToInt() / halfLevels
                    val crushed = (quantized * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    heldSample[c] = crushed
                }
                output[idx] = heldSample[c]
            }
        }

        return audioData.copy(samples = output)
    }
}
