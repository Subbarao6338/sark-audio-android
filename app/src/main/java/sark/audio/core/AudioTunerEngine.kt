package sark.audio.core

import kotlin.math.*

/**
 * Chromatic Instrument Tuner & Pitch Meter Engine
 * Detects real-time note name, octave, target frequency, cents deviation (-50 to +50 cents),
 * and in-tune status for musical instruments and vocal pitch tracking.
 */
object AudioTunerEngine {

    data class TunerResult(
        val detectedHz: Double,
        val noteName: String,     // e.g. "A", "C#", "F"
        val octave: Int,          // e.g. 4 for A4
        val targetHz: Double,     // Exact frequency of closest note
        val centsDeviation: Int,  // -50 to +50 cents error
        val isInTune: Boolean     // Within +/- 5 cents
    )

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Analyzes audio frame for instrument pitch and cents deviation.
     */
    fun analyzePitch(audioData: AudioData): TunerResult {
        val sampleRate = audioData.header.sampleRate
        val detectedPitch = AudioAutotuneProcessor.detectPitchAutocorrelation(
            audioData.samples, 0, min(audioData.samples.size, 2048), sampleRate
        )

        if (detectedPitch < 20.0 || detectedPitch > 5000.0) {
            return TunerResult(0.0, "--", 0, 0.0, 0, false)
        }

        // MIDI note number
        val midiNote = AudioAutotuneProcessor.frequencyToMidi(detectedPitch)
        val roundedMidi = round(midiNote).toInt()

        val noteIndex = (roundedMidi % 12 + 12) % 12
        val octave = (roundedMidi / 12) - 1
        val noteName = NOTE_NAMES[noteIndex]

        val targetHz = AudioAutotuneProcessor.midiToFrequency(roundedMidi.toDouble())
        val cents = (1200.0 * log2(detectedPitch / targetHz)).roundToInt().coerceIn(-50, 50)
        val inTune = abs(cents) <= 5

        return TunerResult(
            detectedHz = detectedPitch,
            noteName = noteName,
            octave = octave,
            targetHz = targetHz,
            centsDeviation = cents,
            isInTune = inTune
        )
    }
}
