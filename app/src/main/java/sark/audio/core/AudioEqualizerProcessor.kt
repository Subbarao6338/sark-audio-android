package sark.audio.core

/**
 * Multi-band Parametric Equalizer
 * Provides standard 5-band or 3-band studio audio equalization:
 * Band 1: Low-shelf (Sub-Bass)
 * Band 2: Peaking EQ (Low Mids)
 * Band 3: Peaking EQ (Presence / Vocal Mids)
 * Band 4: Peaking EQ (High Mids)
 * Band 5: High-shelf (Air / Treble)
 */
object AudioEqualizerProcessor {

    data class EqBand(
        val type: AudioFilterProcessor.FilterType,
        val frequencyHz: Double,
        val q: Double = 0.7071,
        val gainDb: Double = 0.0
    )

    fun applyEq(audioData: AudioData, bands: List<EqBand>): AudioData {
        var current = audioData
        for (band in bands) {
            if (band.gainDb != 0.0) {
                current = AudioFilterProcessor.applyFilter(
                    current,
                    type = band.type,
                    cutoffHz = band.frequencyHz,
                    q = band.q,
                    gainDb = band.gainDb
                )
            }
        }
        return current
    }

    /**
     * Standard 3-Band tone control: Bass (100 Hz), Mid (1 kHz), Treble (10 kHz).
     */
    fun apply3Band(
        audioData: AudioData,
        bassGainDb: Double = 0.0,
        midGainDb: Double = 0.0,
        trebleGainDb: Double = 0.0
    ): AudioData {
        val bands = listOf(
            EqBand(AudioFilterProcessor.FilterType.PEAKING_EQ, 100.0, 0.7, bassGainDb),
            EqBand(AudioFilterProcessor.FilterType.PEAKING_EQ, 1000.0, 1.0, midGainDb),
            EqBand(AudioFilterProcessor.FilterType.PEAKING_EQ, 10000.0, 0.7, trebleGainDb)
        )
        return applyEq(audioData, bands)
    }
}
