package sark.audio.core

/**
 * 3. Silence Generator & Inserter
 * Generates exact duration digital silence and inserts silence into audio buffers.
 */
object AudioSilenceGenerator {

    /**
     * Generates digital silence for the specified duration.
     */
    fun generateSilence(
        durationMs: Long,
        sampleRate: Int = 44100,
        channels: Int = 1
    ): AudioData {
        val totalFrames = ((durationMs * sampleRate) / 1000L).toInt().coerceAtLeast(0)
        val totalSamples = totalFrames * channels
        val samples = ShortArray(totalSamples)

        val header = WavHeader(
            channels = channels,
            sampleRate = sampleRate,
            bitsPerSample = 16,
            dataSize = totalSamples * 2
        )
        return AudioData(header, samples)
    }

    /**
     * Inserts silence at a specific timestamp offset (in ms).
     */
    fun insertSilence(
        audioData: AudioData,
        offsetMs: Long,
        silenceDurationMs: Long
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels

        val offsetFrames = ((offsetMs * sampleRate) / 1000L).toInt().coerceIn(0, totalFrames)
        val silenceFrames = ((silenceDurationMs * sampleRate) / 1000L).toInt().coerceAtLeast(0)

        val newTotalFrames = totalFrames + silenceFrames
        val newSamples = ShortArray(newTotalFrames * channels)

        val offsetSamples = offsetFrames * channels
        val silenceSamples = silenceFrames * channels
        val remainingSamples = (totalFrames - offsetFrames) * channels

        // Copy before silence
        System.arraycopy(audioData.samples, 0, newSamples, 0, offsetSamples)
        // Silence remains 0s
        // Copy after silence
        System.arraycopy(audioData.samples, offsetSamples, newSamples, offsetSamples + silenceSamples, remainingSamples)

        return AudioData(
            header = audioData.header.copy(dataSize = newSamples.size * 2),
            samples = newSamples
        )
    }
}
