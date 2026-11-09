package sark.audio.core

/**
 * Stems Splitter
 * Deconstructs mixed audio into discrete frequency-domain musical stems:
 * 1. Bass Stem (Low frequencies < 220 Hz)
 * 2. Vocals Stem (Presence band 300 Hz - 3400 Hz center-isolated)
 * 3. Drums / Percussion Stem (High transient / high frequency snap > 3500 Hz)
 * 4. Melody / Other Instruments Stem (Harmonic background content)
 */
object AudioStemsSplitter {

    data class StemsResult(
        val vocalsStem: AudioData,
        val bassStem: AudioData,
        val drumsStem: AudioData,
        val melodyStem: AudioData
    )

    /**
     * Splits full-mix audio into 4 stems.
     */
    fun splitStems(audioData: AudioData): StemsResult {
        // Bass: steep low-pass at 220 Hz
        val bass = AudioFilterProcessor.applyFilter(
            audioData, AudioFilterProcessor.FilterType.LOW_PASS, cutoffHz = 220.0, q = 0.9
        )

        // Drums / Transients: high pass at 3800 Hz
        val drums = AudioFilterProcessor.applyFilter(
            audioData, AudioFilterProcessor.FilterType.HIGH_PASS, cutoffHz = 3800.0, q = 0.8
        )

        // Vocals: center channel extraction with vocal band-pass
        val centerIsolated = AudioVocalProcessor.isolateVocals(audioData)
        val vocals = AudioFilterProcessor.applyFilter(
            centerIsolated, AudioFilterProcessor.FilterType.BAND_PASS, cutoffHz = 1600.0, q = 0.8
        )

        // Melody: audio minus (bass + drums + vocals)
        val channels = audioData.header.channels
        val melodySamples = ShortArray(audioData.samples.size)
        for (i in audioData.samples.indices) {
            val total = audioData.samples[i].toInt()
            val b = bass.samples[i].toInt()
            val d = drums.samples[i].toInt()
            val v = vocals.samples[i].toInt()
            val m = total - (b + d + v) / 2
            melodySamples[i] = m.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val melody = audioData.copy(samples = melodySamples)

        return StemsResult(
            vocalsStem = vocals,
            bassStem = bass,
            drumsStem = drums,
            melodyStem = melody
        )
    }
}
