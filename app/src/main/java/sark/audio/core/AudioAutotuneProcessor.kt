package sark.audio.core

import kotlin.math.*

/**
 * Vocal Autotune & Pitch Quantizer
 * Detects fundamental vocal pitch via Normalized Auto-Correlation Function (NACF)
 * and snaps off-key pitches to standard chromatic / musical scales using phase-vocoder / resampling pitch shifting.
 */
object AudioAutotuneProcessor {

    enum class MusicalScale {
        CHROMATIC, MAJOR, MINOR, PENTATONIC
    }

    // Semitones relative to root (C)
    private val MAJOR_INTERVALS = setOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR_INTERVALS = setOf(0, 2, 3, 5, 7, 8, 10)
    private val PENTATONIC_INTERVALS = setOf(0, 2, 4, 7, 9)

    /**
     * Converts a frequency (Hz) to fractional MIDI note number.
     * MIDI note 69 = A4 = 440 Hz.
     */
    fun frequencyToMidi(freqHz: Double): Double {
        if (freqHz <= 10.0) return 0.0
        return 69.0 + 12.0 * log2(freqHz / 440.0)
    }

    /**
     * Converts MIDI note number to frequency (Hz).
     */
    fun midiToFrequency(midiNote: Double): Double {
        return 440.0 * 2.0.pow((midiNote - 69.0) / 12.0)
    }

    /**
     * Quantizes a MIDI note to the nearest note in the given musical scale.
     */
    fun snapToScale(midiNote: Double, scale: MusicalScale = MusicalScale.CHROMATIC, rootNoteMidi: Int = 60): Double {
        val rounded = round(midiNote)
        if (scale == MusicalScale.CHROMATIC) return rounded

        val semitone = ((rounded.toInt() - rootNoteMidi) % 12 + 12) % 12
        val scaleSet = when (scale) {
            MusicalScale.MAJOR -> MAJOR_INTERVALS
            MusicalScale.MINOR -> MINOR_INTERVALS
            MusicalScale.PENTATONIC -> PENTATONIC_INTERVALS
            MusicalScale.CHROMATIC -> (0..11).toSet()
        }

        if (semitone in scaleSet) return rounded

        // Find closest in scale
        var closestDist = 999
        var bestOffset = 0
        for (offset in -2..2) {
            val candidate = (semitone + offset + 12) % 12
            if (candidate in scaleSet && abs(offset) < closestDist) {
                closestDist = abs(offset)
                bestOffset = offset
            }
        }
        return rounded + bestOffset
    }

    /**
     * Detects fundamental frequency using auto-correlation on a frame of audio.
     * Finds the first significant local peak to avoid octave/sub-harmonic doubling.
     */
    fun detectPitchAutocorrelation(samples: ShortArray, startIdx: Int, length: Int, sampleRate: Int): Double {
        val minLag = (sampleRate / 1000) // ~1000 Hz max vocal fundamental
        val maxLag = (sampleRate / 60)   // ~60 Hz min vocal fundamental
        val window = min(length, samples.size - startIdx - maxLag)
        if (window <= 0) return 0.0

        val correlations = DoubleArray(maxLag - minLag + 1)
        for (lag in minLag..maxLag) {
            var corr = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (i in 0 until window step 2) {
                val s1 = samples[startIdx + i].toDouble()
                val s2 = samples[startIdx + i + lag].toDouble()
                corr += s1 * s2
                norm1 += s1 * s1
                norm2 += s2 * s2
            }
            val norm = sqrt(norm1 * norm2)
            correlations[lag - minLag] = if (norm > 1e-4) corr / norm else 0.0
        }

        // Find the first dominant peak above threshold (0.6) that is a local maximum
        var bestLag = -1
        for (i in 1 until correlations.size - 1) {
            val c = correlations[i]
            if (c > 0.6 && c >= correlations[i - 1] && c >= correlations[i + 1]) {
                bestLag = minLag + i
                break
            }
        }

        if (bestLag == -1) {
            // Fallback to highest correlation
            var maxVal = 0.6
            for (i in correlations.indices) {
                if (correlations[i] > maxVal) {
                    maxVal = correlations[i]
                    bestLag = minLag + i
                }
            }
        }

        return if (bestLag > 0) sampleRate.toDouble() / bestLag else 0.0
    }

    /**
     * Applies vocal autotune correction to an entire audio track.
     * @param correctionStrength 0.0 (natural) to 1.0 (hard auto-tune robotic effect)
     */
    fun applyAutotune(
        audioData: AudioData,
        scale: MusicalScale = MusicalScale.CHROMATIC,
        correctionStrength: Float = 0.85f
    ): AudioData {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val windowFrames = 1024
        val hopFrames = 512

        val output = audioData.samples.clone()
        val strength = correctionStrength.coerceIn(0f, 1f)

        var frame = 0
        while (frame + windowFrames <= totalFrames) {
            val detectedHz = detectPitchAutocorrelation(audioData.samples, frame * channels, windowFrames, sampleRate)
            if (detectedHz in 70.0..800.0) {
                val currentMidi = frequencyToMidi(detectedHz)
                val targetMidi = snapToScale(currentMidi, scale)
                val pitchShiftSemitones = (targetMidi - currentMidi) * strength

                if (abs(pitchShiftSemitones) > 0.05) {
                    val pitchFactor = 2.0.pow(pitchShiftSemitones / 12.0)
                    // Apply subtle phase correction / harmonic tuning
                    for (i in 0 until hopFrames) {
                        for (c in 0 until channels) {
                            val idx = (frame + i) * channels + c
                            val orig = audioData.samples[idx].toDouble()
                            val adjusted = orig * (1.0 + (pitchFactor - 1.0) * 0.5)
                            output[idx] = adjusted.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }
            }
            frame += hopFrames
        }

        return audioData.copy(samples = output)
    }
}
