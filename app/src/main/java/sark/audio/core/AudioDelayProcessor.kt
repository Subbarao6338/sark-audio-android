package sark.audio.core

import kotlin.math.abs

/**
 * 15. Audio Delay & Echo Processor
 * Studio delay and echo effect with feedback, delay time, and wet/dry mix.
 */
object AudioDelayProcessor {

    /**
     * Applies an echo / feedback delay effect.
     * @param delayMs Delay time in milliseconds (e.g. 250 ms)
     * @param feedback Echo decay feedback level (0.0 to 0.9)
     * @param wetMix Wet delayed signal level (0.0 to 1.0)
     * @param dryMix Dry original signal level (0.0 to 1.0)
     */
    fun applyDelay(
        audioData: AudioData,
        delayMs: Long = 250L,
        feedback: Float = 0.45f,
        wetMix: Float = 0.5f,
        dryMix: Float = 0.8f
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val delayFrames = ((delayMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)

        val totalFrames = audioData.samples.size / channels
        // Ring buffer for delay lines
        val delayLine = FloatArray(delayFrames * channels)
        var delayWriteIdx = 0

        val output = ShortArray(audioData.samples.size)

        for (f in 0 until totalFrames) {
            for (c in 0 until channels) {
                val idx = f * channels + c
                val ringIdx = delayWriteIdx * channels + c

                val drySample = audioData.samples[idx].toFloat() / Short.MAX_VALUE
                val delayedSample = delayLine[ringIdx]

                // Wet + Dry sum
                val mixed = drySample * dryMix + delayedSample * wetMix

                // Write feedback into delay line
                delayLine[ringIdx] = drySample + delayedSample * feedback.coerceIn(0f, 0.95f)

                output[idx] = (mixed * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            delayWriteIdx = (delayWriteIdx + 1) % delayFrames
        }

        return audioData.copy(samples = output)
    }
}
