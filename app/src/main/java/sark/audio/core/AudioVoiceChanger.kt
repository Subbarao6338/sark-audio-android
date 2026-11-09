package sark.audio.core

import kotlin.math.*

/**
 * Voice Changer
 * Implements dramatic vocal transformations: Chipmunk, Deep Robot, Alien, Monster, and Helium
 * through pitch shifting, formant modulation, ring modulation, and resonant filtering.
 */
object AudioVoiceChanger {

    enum class VoicePreset {
        CHIPMUNK,     // High pitch + preserved tempo
        DEEP_MONSTER, // Sub-bass low pitch
        ROBOT,        // Ring modulation carrier tone
        ALIEN,        // Tremolo LFO pitch modulation
        HELIUM,       // High formant upward shift
        RADIO         // Telephone / megaphone bandpass
    }

    /**
     * Applies voice changer preset.
     */
    fun applyPreset(audioData: AudioData, preset: VoicePreset): AudioData {
        return when (preset) {
            VoicePreset.CHIPMUNK -> {
                // Pitch up 1.5x (~7 semitones)
                AudioTimePitchProcessor.changePitch(audioData, 1.5f)
            }
            VoicePreset.DEEP_MONSTER -> {
                // Pitch down 0.7x (~ -6 semitones) and mild soft clipping
                val pitched = AudioTimePitchProcessor.changePitch(audioData, 0.7f)
                AudioDistortionProcessor.distort(pitched, drive = 1.8f, mode = AudioDistortionProcessor.DistortionMode.TUBE_SATURATION)
            }
            VoicePreset.ROBOT -> {
                // Ring modulation with 60 Hz carrier wave
                val sampleRate = audioData.header.sampleRate
                val channels = audioData.header.channels
                val totalFrames = audioData.samples.size / channels
                val out = ShortArray(audioData.samples.size)

                for (f in 0 until totalFrames) {
                    val carrier = sin(2.0 * PI * 60.0 * (f.toDouble() / sampleRate))
                    for (c in 0 until channels) {
                        val idx = f * channels + c
                        val s = audioData.samples[idx].toDouble()
                        val modulated = s * (0.4 + 0.6 * carrier)
                        out[idx] = modulated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                audioData.copy(samples = out)
            }
            VoicePreset.ALIEN -> {
                // Ring modulation + LFO vibrato
                val sampleRate = audioData.header.sampleRate
                val channels = audioData.header.channels
                val totalFrames = audioData.samples.size / channels
                val out = ShortArray(audioData.samples.size)

                for (f in 0 until totalFrames) {
                    val lfo = sin(2.0 * PI * 8.0 * (f.toDouble() / sampleRate))
                    val carrier = sin(2.0 * PI * (300.0 + lfo * 50.0) * (f.toDouble() / sampleRate))
                    for (c in 0 until channels) {
                        val idx = f * channels + c
                        val s = audioData.samples[idx].toDouble()
                        val modulated = s * (0.3 + 0.7 * carrier)
                        out[idx] = modulated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                audioData.copy(samples = out)
            }
            VoicePreset.HELIUM -> {
                AudioTimePitchProcessor.changePitch(audioData, 1.8f)
            }
            VoicePreset.RADIO -> {
                // Bandpass filter 400 Hz to 3500 Hz + soft overdrive
                val filtered = AudioFilterProcessor.applyFilter(
                    audioData, AudioFilterProcessor.FilterType.BAND_PASS, cutoffHz = 1800.0, q = 1.5
                )
                AudioDistortionProcessor.distort(filtered, drive = 2.5f, mode = AudioDistortionProcessor.DistortionMode.SOFT_CLIP)
            }
        }
    }
}
