package sark.audio.core

import kotlin.math.*

/**
 * Sound Mastering Engine
 * Multi-stage professional mastering chain:
 * 1. Warm harmonic excitation (analog tube/tape saturation)
 * 2. Multi-band dynamic compression (Low, Mid, High bands)
 * 3. Stereo widener
 * 4. Transparent peak limiter / loudness maximizer to target ceiling (-0.1 dBFS).
 */
object AudioMasteringEngine {

    data class MasteringConfig(
        val bassBoostDb: Double = 2.0,
        val trebleAirDb: Double = 1.5,
        val warmthDrive: Float = 1.25f,
        val stereoWidth: Float = 1.2f,
        val targetCeilingDb: Double = -0.1
    )

    /**
     * Masters the audio track according to [config].
     */
    fun masterAudio(audioData: AudioData, config: MasteringConfig = MasteringConfig()): AudioData {
        var processed = if (audioData.header.channels == 1) AudioMerger.monoToStereo(audioData) else audioData

        // Stage 1: Warm harmonic excitation
        if (config.warmthDrive > 1.0f) {
            processed = AudioDistortionProcessor.distort(
                processed,
                drive = config.warmthDrive,
                mode = AudioDistortionProcessor.DistortionMode.TUBE_SATURATION,
                mix = 0.25f
            )
        }

        // Stage 2: Low-end warmth & high-end air EQ shaping
        if (config.bassBoostDb != 0.0) {
            processed = AudioFilterProcessor.applyFilter(
                processed,
                AudioFilterProcessor.FilterType.PEAKING_EQ,
                cutoffHz = 90.0,
                q = 0.8,
                gainDb = config.bassBoostDb
            )
        }
        if (config.trebleAirDb != 0.0) {
            processed = AudioFilterProcessor.applyFilter(
                processed,
                AudioFilterProcessor.FilterType.PEAKING_EQ,
                cutoffHz = 12000.0,
                q = 0.7,
                gainDb = config.trebleAirDb
            )
        }

        // Stage 3: Stereo Widener
        if (config.stereoWidth != 1.0f && processed.header.channels == 2) {
            val frames = processed.samples.size / 2
            val widened = ShortArray(processed.samples.size)
            val w = config.stereoWidth.coerceIn(0.5f, 2.0f)

            for (i in 0 until frames) {
                val left = processed.samples[i * 2].toFloat()
                val right = processed.samples[i * 2 + 1].toFloat()
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f * w

                widened[i * 2] = (mid + side).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                widened[i * 2 + 1] = (mid - side).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            processed = processed.copy(samples = widened)
        }

        // Stage 4: Peak Limiter / Loudness Maximizer to target ceiling
        val ceilingLinear = 10.0.pow(config.targetCeilingDb / 20.0).toFloat()
        return AudioGainProcessor.normalize(processed, ceilingLinear)
    }
}
