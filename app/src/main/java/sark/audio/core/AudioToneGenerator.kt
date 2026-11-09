package sark.audio.core

import kotlin.math.*
import kotlin.random.Random

/**
 * 1. Tone Generator
 * Generates pure and algorithmic waveforms: Sine, Square, Triangle, Sawtooth, White Noise, Pink Noise, and Frequency Sweep (Chirp).
 */
object AudioToneGenerator {

    enum class WaveformType {
        SINE, SQUARE, TRIANGLE, SAWTOOTH, WHITE_NOISE, PINK_NOISE, BROWN_NOISE, HARMONICS, CHIRP
    }

    /**
     * ADSR Amplitude Envelope Configuration
     */
    data class AdsrEnvelope(
        val attackSec: Double = 0.05,
        val decaySec: Double = 0.1,
        val sustainLevel: Double = 0.7,
        val releaseSec: Double = 0.1
    )

    /**
     * Synthesizes audio samples based on waveform type.
     */
    fun generateTone(
        type: WaveformType,
        frequencyHz: Double = 440.0,
        stopFrequencyHz: Double = 880.0, // used for CHIRP
        durationSeconds: Double = 1.0,
        sampleRate: Int = 44100,
        channels: Int = 1,
        amplitude: Float = 0.8f,
        envelope: AdsrEnvelope? = null,
        harmonicsWeights: DoubleArray? = null // For HARMONICS additive synthesis [h1, h2, h3, h4]
    ): AudioData {
        val totalFrames = (sampleRate * durationSeconds).toInt().coerceAtLeast(1)
        val totalSamples = totalFrames * channels
        val samples = ShortArray(totalSamples)
        val maxAmp = (Short.MAX_VALUE * amplitude.coerceIn(0f, 1f)).toDouble()

        // State for pink noise generator (Paul Kellet's filtered white noise method)
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var b3 = 0.0
        var b4 = 0.0
        var b5 = 0.0
        var b6 = 0.0
        var brownLast = 0.0

        val hWeights = harmonicsWeights ?: doubleArrayOf(1.0, 0.5, 0.25, 0.125)
        val sumHWeights = hWeights.sum().coerceAtLeast(0.001)

        val attackFrames = ((envelope?.attackSec ?: 0.0) * sampleRate).toInt().coerceAtLeast(0)
        val decayFrames = ((envelope?.decaySec ?: 0.0) * sampleRate).toInt().coerceAtLeast(0)
        val releaseFrames = ((envelope?.releaseSec ?: 0.0) * sampleRate).toInt().coerceAtLeast(0)
        val sustainFrames = (totalFrames - attackFrames - decayFrames - releaseFrames).coerceAtLeast(0)

        for (i in 0 until totalFrames) {
            val t = i.toDouble() / sampleRate
            val sampleVal: Double = when (type) {
                WaveformType.SINE -> {
                    sin(2.0 * PI * frequencyHz * t)
                }
                WaveformType.SQUARE -> {
                    val phase = (frequencyHz * t) % 1.0
                    if (phase < 0.5) 1.0 else -1.0
                }
                WaveformType.TRIANGLE -> {
                    val phase = (frequencyHz * t) % 1.0
                    // Triangle wave between -1 and 1
                    4.0 * abs(phase - 0.5) - 1.0
                }
                WaveformType.SAWTOOTH -> {
                    val phase = (frequencyHz * t) % 1.0
                    2.0 * phase - 1.0
                }
                WaveformType.WHITE_NOISE -> {
                    Random.nextDouble(-1.0, 1.0)
                }
                WaveformType.PINK_NOISE -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    b0 = 0.99886 * b0 + white * 0.0555179
                    b1 = 0.99332 * b1 + white * 0.0750759
                    b2 = 0.96900 * b2 + white * 0.1538520
                    b3 = 0.86650 * b3 + white * 0.3104856
                    b4 = 0.55000 * b4 + white * 0.5329522
                    b5 = -0.7616 * b5 - white * 0.0168980
                    val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                    b6 = white * 0.115926
                    (pink * 0.11).coerceIn(-1.0, 1.0)
                }
                WaveformType.BROWN_NOISE -> {
                    val white = Random.nextDouble(-1.0, 1.0)
                    brownLast = (brownLast + (0.02 * white)) / 1.02
                    (brownLast * 3.5).coerceIn(-1.0, 1.0)
                }
                WaveformType.HARMONICS -> {
                    var sum = 0.0
                    for (h in hWeights.indices) {
                        val harmonicNum = h + 1
                        sum += hWeights[h] * sin(2.0 * PI * (frequencyHz * harmonicNum) * t)
                    }
                    (sum / sumHWeights).coerceIn(-1.0, 1.0)
                }
                WaveformType.CHIRP -> {
                    // Linear frequency sweep
                    val progress = i.toDouble() / totalFrames
                    val currentFreq = frequencyHz + (stopFrequencyHz - frequencyHz) * progress / 2.0
                    sin(2.0 * PI * currentFreq * t)
                }
            }

            // Calculate ADSR gain if envelope provided
            var envGain = 1.0
            if (envelope != null) {
                envGain = when {
                    i < attackFrames -> if (attackFrames > 0) i.toDouble() / attackFrames else 1.0
                    i < attackFrames + decayFrames -> {
                        val decayProgress = (i - attackFrames).toDouble() / decayFrames.coerceAtLeast(1)
                        1.0 - (1.0 - envelope.sustainLevel) * decayProgress
                    }
                    i < attackFrames + decayFrames + sustainFrames -> envelope.sustainLevel
                    else -> {
                        val releaseProgress = (i - (attackFrames + decayFrames + sustainFrames)).toDouble() / releaseFrames.coerceAtLeast(1)
                        envelope.sustainLevel * (1.0 - releaseProgress).coerceAtLeast(0.0)
                    }
                }
            }

            val pcmVal = (sampleVal * envGain * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            for (c in 0 until channels) {
                samples[i * channels + c] = pcmVal
            }
        }

        val header = WavHeader(
            channels = channels,
            sampleRate = sampleRate,
            bitsPerSample = 16,
            dataSize = totalSamples * 2
        )
        return AudioData(header, samples)
    }
}
