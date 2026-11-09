package sark.audio.core

/**
 * 7. Audio Invert Processor
 * Inverts the phase of audio samples (180-degree polarity flip), useful for phase cancellation & stereo widener.
 */
object AudioInvertProcessor {

    /**
     * Inverts the polarity of all audio samples.
     */
    fun invertPhase(audioData: AudioData): AudioData {
        val newSamples = ShortArray(audioData.samples.size)
        for (i in audioData.samples.indices) {
            val s = audioData.samples[i].toInt()
            newSamples[i] = (-s).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return audioData.copy(samples = newSamples)
    }

    /**
     * Inverts phase of an individual channel only (e.g. right channel for stereo widening effect).
     */
    fun invertChannel(audioData: AudioData, targetChannel: Int): AudioData {
        val channels = audioData.header.channels
        if (targetChannel !in 0 until channels) return audioData

        val newSamples = audioData.samples.clone()
        val totalFrames = newSamples.size / channels

        for (f in 0 until totalFrames) {
            val idx = f * channels + targetChannel
            newSamples[idx] = (-newSamples[idx].toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return audioData.copy(samples = newSamples)
    }
}
