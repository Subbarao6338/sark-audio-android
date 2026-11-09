package sark.audio.core

import kotlin.math.PI
import kotlin.math.sin

/**
 * 2. DTMF & Morse Code Synthesizer
 * Generates telephone touch-tones (dual-tone multi-frequency) and ITU Morse code audio.
 */
object AudioDtmfGenerator {

    // Standard DTMF low frequencies (Hz) for rows
    private val ROW_FREQS = mapOf(
        '1' to 697.0, '2' to 697.0, '3' to 697.0, 'A' to 697.0,
        '4' to 770.0, '5' to 770.0, '6' to 770.0, 'B' to 770.0,
        '7' to 852.0, '8' to 852.0, '9' to 852.0, 'C' to 852.0,
        '*' to 941.0, '0' to 941.0, '#' to 941.0, 'D' to 941.0
    )

    // Standard DTMF high frequencies (Hz) for columns
    private val COL_FREQS = mapOf(
        '1' to 1209.0, '4' to 1209.0, '7' to 1209.0, '*' to 1209.0,
        '2' to 1336.0, '5' to 1336.0, '8' to 1336.0, '0' to 1336.0,
        '3' to 1477.0, '6' to 1477.0, '9' to 1477.0, '#' to 1477.0,
        'A' to 1633.0, 'B' to 1633.0, 'C' to 1633.0, 'D' to 1633.0
    )

    private val MORSE_MAP = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '0' to "-----", ' ' to " "
    )

    /**
     * Synthesizes a DTMF tone for a single key.
     */
    fun generateKeyTone(
        key: Char,
        durationMs: Long = 160L,
        sampleRate: Int = 44100,
        amplitude: Float = 0.8f
    ): AudioData {
        val upperKey = key.uppercaseChar()
        val fLow = ROW_FREQS[upperKey] ?: 697.0
        val fHigh = COL_FREQS[upperKey] ?: 1209.0

        val totalFrames = ((durationMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
        val samples = ShortArray(totalFrames)
        val maxAmp = Short.MAX_VALUE * (amplitude.coerceIn(0f, 1f) / 2.0)

        for (i in 0 until totalFrames) {
            val t = i.toDouble() / sampleRate
            val sampleVal = sin(2.0 * PI * fLow * t) + sin(2.0 * PI * fHigh * t)
            samples[i] = (sampleVal * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return AudioData(
            WavHeader(channels = 1, sampleRate = sampleRate, bitsPerSample = 16, dataSize = totalFrames * 2),
            samples
        )
    }

    /**
     * Synthesizes a sequence of DTMF tones (e.g. "8675309").
     */
    fun generateDialSequence(
        sequence: String,
        toneDurationMs: Long = 120L,
        pauseDurationMs: Long = 60L,
        sampleRate: Int = 44100
    ): AudioData {
        val tracks = mutableListOf<AudioData>()
        val pauseData = AudioSilenceGenerator.generateSilence(pauseDurationMs, sampleRate, 1)

        for (char in sequence) {
            val upper = char.uppercaseChar()
            if (ROW_FREQS.containsKey(upper)) {
                tracks.add(generateKeyTone(upper, toneDurationMs, sampleRate))
                tracks.add(pauseData)
            } else if (char == ' ') {
                tracks.add(AudioSilenceGenerator.generateSilence(toneDurationMs + pauseDurationMs, sampleRate, 1))
            }
        }

        return if (tracks.isEmpty()) {
            AudioSilenceGenerator.generateSilence(100L, sampleRate, 1)
        } else {
            AudioMerger.mergeTracks(tracks)
        }
    }

    /**
     * Synthesizes audible Morse code for text input.
     */
    fun generateMorseCode(
        text: String,
        frequencyHz: Double = 700.0,
        unitDurationMs: Long = 60L,
        sampleRate: Int = 44100
    ): AudioData {
        val tracks = mutableListOf<AudioData>()
        val dit = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, frequencyHz, durationSeconds = unitDurationMs / 1000.0, sampleRate = sampleRate)
        val dah = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, frequencyHz, durationSeconds = (unitDurationMs * 3) / 1000.0, sampleRate = sampleRate)
        val intraElementSilence = AudioSilenceGenerator.generateSilence(unitDurationMs, sampleRate, 1)
        val interCharSilence = AudioSilenceGenerator.generateSilence(unitDurationMs * 3, sampleRate, 1)
        val wordSilence = AudioSilenceGenerator.generateSilence(unitDurationMs * 7, sampleRate, 1)

        for (char in text.uppercase()) {
            if (char == ' ') {
                tracks.add(wordSilence)
                continue
            }
            val morse = MORSE_MAP[char] ?: continue
            for ((index, symbol) in morse.withIndex()) {
                if (symbol == '.') tracks.add(dit)
                else if (symbol == '-') tracks.add(dah)

                if (index < morse.length - 1) {
                    tracks.add(intraElementSilence)
                }
            }
            tracks.add(interCharSilence)
        }

        return if (tracks.isEmpty()) {
            AudioSilenceGenerator.generateSilence(100L, sampleRate, 1)
        } else {
            AudioMerger.mergeTracks(tracks)
        }
    }
}
