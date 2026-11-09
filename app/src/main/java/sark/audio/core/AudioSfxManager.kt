package sark.audio.core

import kotlin.math.*
import kotlin.random.Random

/**
 * SFX Manager & Sound Effect Synthesizer
 * Generates custom procedural sound effects (Beep, Impact, Riser, Laser, Retro Jump, Applause)
 * and overlays them onto existing audio tracks at specified millisecond timestamps.
 */
object AudioSfxManager {

    enum class SfxType {
        CENSOR_BEEP,      // 1000 Hz pure sine beep
        IMPACT_BOOM,      // Low sub-drop impact
        PITCH_RISER,      // Upward frequency build-up
        RETRO_LASER,      // Downward frequency zap
        APPLAUSE_BURST    // Shaped noise clap burst
    }

    /**
     * Synthesizes a standalone sound effect.
     */
    fun generateSfx(type: SfxType, sampleRate: Int = 44100): AudioData {
        return when (type) {
            SfxType.CENSOR_BEEP -> {
                AudioToneGenerator.generateTone(
                    type = AudioToneGenerator.WaveformType.SINE,
                    frequencyHz = 1000.0,
                    durationSeconds = 0.5,
                    sampleRate = sampleRate,
                    amplitude = 0.85f
                )
            }
            SfxType.IMPACT_BOOM -> {
                val totalFrames = (sampleRate * 0.8).toInt()
                val samples = ShortArray(totalFrames)
                for (i in 0 until totalFrames) {
                    val t = i.toDouble() / sampleRate
                    val freq = 120.0 * exp(-4.0 * t) + 30.0
                    val amp = exp(-3.0 * t) * 0.95
                    val s = sin(2.0 * PI * freq * t) * amp
                    samples[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }
                AudioData(WavHeader(channels = 1, sampleRate = sampleRate, bitsPerSample = 16, dataSize = totalFrames * 2), samples)
            }
            SfxType.PITCH_RISER -> {
                AudioToneGenerator.generateTone(
                    type = AudioToneGenerator.WaveformType.CHIRP,
                    frequencyHz = 200.0,
                    stopFrequencyHz = 3000.0,
                    durationSeconds = 1.2,
                    sampleRate = sampleRate,
                    amplitude = 0.8f
                )
            }
            SfxType.RETRO_LASER -> {
                val totalFrames = (sampleRate * 0.25).toInt()
                val samples = ShortArray(totalFrames)
                for (i in 0 until totalFrames) {
                    val t = i.toDouble() / sampleRate
                    val freq = 2400.0 * exp(-15.0 * t) + 150.0
                    val amp = (1.0 - t / 0.25) * 0.9
                    val phase = (freq * t) % 1.0
                    val s = (if (phase < 0.5) 1.0 else -1.0) * amp
                    samples[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }
                AudioData(WavHeader(channels = 1, sampleRate = sampleRate, bitsPerSample = 16, dataSize = totalFrames * 2), samples)
            }
            SfxType.APPLAUSE_BURST -> {
                val totalFrames = (sampleRate * 1.0).toInt()
                val samples = ShortArray(totalFrames)
                for (i in 0 until totalFrames) {
                    val t = i.toDouble() / sampleRate
                    val envelope = sin(PI * t / 1.0).pow(0.5) * 0.7
                    val noise = Random.nextDouble(-1.0, 1.0) * envelope
                    samples[i] = (noise * Short.MAX_VALUE).toInt().toShort()
                }
                AudioData(WavHeader(channels = 1, sampleRate = sampleRate, bitsPerSample = 16, dataSize = totalFrames * 2), samples)
            }
        }
    }

    /**
     * Overlays a sound effect on top of an existing audio track at [timestampMs].
     */
    fun overlaySfx(baseAudio: AudioData, sfx: AudioData, timestampMs: Long, sfxVolume: Float = 0.8f): AudioData {
        return AudioMixer.mixTracks(
            listOf(
                AudioMixer.MixTrack(baseAudio, volume = 1.0f, delayOffsetMs = 0L),
                AudioMixer.MixTrack(sfx, volume = sfxVolume, delayOffsetMs = timestampMs)
            ),
            targetSampleRate = baseAudio.header.sampleRate,
            targetChannels = baseAudio.header.channels
        )
    }
}
