package sark.audio.core

import kotlin.math.abs

/**
 * Vocal Processor & Karaoke Generator
 * Implements Center Channel Extraction and Center Channel Cancellation (Overtone Phase Inversion)
 * to remove lead vocals (producing an instrumental / karaoke track) or isolate the center vocal track.
 */
object AudioVocalProcessor {

    /**
     * Removes center-panned vocals to create a Karaoke / Instrumental track.
     * @param vocalReductionStrength 0.0 to 1.0 (1.0 = maximum cancellation)
     * @param bassPreservationHz Preserves low bass/kick frequencies below this threshold
     */
    fun removeVocals(
        audioData: AudioData,
        vocalReductionStrength: Float = 0.95f,
        bassPreservationHz: Double = 120.0
    ): AudioData {
        val stereo = if (audioData.header.channels == 1) {
            AudioMerger.monoToStereo(audioData)
        } else {
            audioData
        }

        val frames = stereo.samples.size / 2
        val outSamples = ShortArray(stereo.samples.size)
        val strength = vocalReductionStrength.coerceIn(0f, 1f)

        // Bass preservation filter: simple single-pole low pass filter
        val dt = 1.0 / stereo.header.sampleRate
        val rc = 1.0 / (2.0 * Math.PI * bassPreservationHz)
        val alpha = (dt / (rc + dt)).toFloat()
        var bassMono = 0f

        for (i in 0 until frames) {
            val left = stereo.samples[i * 2].toFloat()
            val right = stereo.samples[i * 2 + 1].toFloat()

            // Center cancellation: (L - R)
            val diff = (left - right) * 0.7071f

            // Bass extraction only if there is a difference or explicitly enabled
            val center = (left + right) * 0.5f
            bassMono += alpha * (center - bassMono)

            // If bassPreservationHz <= 0, pure cancellation without bass inject
            val bassToAdd = if (bassPreservationHz > 0.0) bassMono * 0.5f else 0f

            val outL = (left * (1.0f - strength) + diff + bassToAdd).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val outR = (right * (1.0f - strength) - diff + bassToAdd).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            outSamples[i * 2] = outL
            outSamples[i * 2 + 1] = outR
        }

        return stereo.copy(samples = outSamples)
    }

    /**
     * Isolates center-panned vocals by subtracting stereo side content.
     */
    fun isolateVocals(audioData: AudioData): AudioData {
        val stereo = if (audioData.header.channels == 1) AudioMerger.monoToStereo(audioData) else audioData
        val frames = stereo.samples.size / 2
        val outSamples = ShortArray(stereo.samples.size)

        for (i in 0 until frames) {
            val left = stereo.samples[i * 2].toFloat()
            val right = stereo.samples[i * 2 + 1].toFloat()

            // Center sum
            val center = ((left + right) * 0.5f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            outSamples[i * 2] = center
            outSamples[i * 2 + 1] = center
        }

        return stereo.copy(samples = outSamples)
    }
}
