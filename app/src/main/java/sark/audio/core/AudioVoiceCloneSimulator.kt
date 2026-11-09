package sark.audio.core

import kotlin.math.*

/**
 * Voice Clone & Formant Vocal Timbre Synthesizer
 * Analyzes spectral formants (F1, F2, F3) of source audio and synthesizes/transposes
 * vocal timbre using a cascade resonator filter bank model.
 */
object AudioVoiceCloneSimulator {

    data class FormantProfile(
        val f1Hz: Double,
        val f2Hz: Double,
        val f3Hz: Double,
        val pitchShiftFactor: Float
    )

    /**
     * Extracts estimated vocal formants from audio data.
     */
    fun extractFormants(audioData: AudioData): FormantProfile {
        val sampleRate = audioData.header.sampleRate
        val spectrum = AudioSpectrumAnalyzer.analyze(audioData, fftSize = 1024)

        // Find primary spectral energy clusters for vowel formants
        var f1 = 500.0
        var f2 = 1500.0
        var f3 = 2500.0

        for (i in spectrum.frequencies.indices) {
            val freq = spectrum.frequencies[i]
            val mag = spectrum.magnitudesDb[i]
            if (freq in 250.0..900.0 && mag > -30f) {
                f1 = freq
            } else if (freq in 900.0..2400.0 && mag > -35f) {
                f2 = freq
            } else if (freq in 2400.0..4000.0 && mag > -40f) {
                f3 = freq
            }
        }

        return FormantProfile(f1, f2, f3, 1.0f)
    }

    /**
     * Imposes target formant profile and vocal characteristics onto an audio signal.
     */
    fun applyVocalCloneProfile(
        inputAudio: AudioData,
        targetProfile: FormantProfile,
        blend: Float = 0.8f
    ): AudioData {
        // Resonant filtering at target formant frequencies
        var processed = AudioFilterProcessor.applyFilter(
            inputAudio,
            AudioFilterProcessor.FilterType.PEAKING_EQ,
            cutoffHz = targetProfile.f1Hz,
            q = 3.0,
            gainDb = 6.0
        )
        processed = AudioFilterProcessor.applyFilter(
            processed,
            AudioFilterProcessor.FilterType.PEAKING_EQ,
            cutoffHz = targetProfile.f2Hz,
            q = 4.0,
            gainDb = 4.0
        )
        processed = AudioFilterProcessor.applyFilter(
            processed,
            AudioFilterProcessor.FilterType.PEAKING_EQ,
            cutoffHz = targetProfile.f3Hz,
            q = 5.0,
            gainDb = 3.0
        )

        val blendedSamples = ShortArray(inputAudio.samples.size)
        val clampedBlend = blend.coerceIn(0f, 1f)
        for (i in inputAudio.samples.indices) {
            val orig = inputAudio.samples[i].toFloat()
            val modeled = processed.samples[i].toFloat()
            blendedSamples[i] = (orig * (1f - clampedBlend) + modeled * clampedBlend).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return inputAudio.copy(samples = blendedSamples)
    }
}
