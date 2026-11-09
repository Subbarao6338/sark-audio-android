package sark.audio.core

import kotlin.math.*

/**
 * 8D Audio & Spatial Ambience Processor
 * Creates immersive 360-degree rotating headphone audio using circular binaural panning,
 * Head-Related Interaural Time Delay (ITD), Interaural Level Difference (ILD), and room diffusion reverberation.
 */
object Audio8DProcessor {

    /**
     * Processes audio into dynamic 8D circular panning with room ambience.
     * @param rotationSpeedHz Speed of circular panning in Hertz (e.g. 0.15 Hz = 1 full revolution every ~6.6 seconds)
     * @param reverbDepth Wet level of spatial room reflections (0.0 to 1.0)
     */
    fun process8D(
        audioData: AudioData,
        rotationSpeedHz: Double = 0.15,
        reverbDepth: Float = 0.25f
    ): AudioData {
        val stereo = if (audioData.header.channels == 1) AudioMerger.monoToStereo(audioData) else audioData
        val sampleRate = stereo.header.sampleRate
        val totalFrames = stereo.samples.size / 2

        // Delay buffer for interaural time difference (ITD max ~0.7ms = ~31 samples @ 44.1kHz)
        val maxItdSamples = ((0.0007 * sampleRate).toInt()).coerceAtLeast(1)
        val itdBufferL = FloatArray(maxItdSamples + 1)
        val itdBufferR = FloatArray(maxItdSamples + 1)
        var itdWriteIdx = 0

        // Comb filter buffer for room diffusion reverb
        val reverbDelaySamples = (0.025 * sampleRate).toInt() // 25ms reflection
        val reverbBuffer = FloatArray(reverbDelaySamples)
        var reverbWriteIdx = 0

        val outSamples = ShortArray(stereo.samples.size)

        for (f in 0 until totalFrames) {
            val t = f.toDouble() / sampleRate
            // Angle of sound source rotating around listener (0 to 2*PI)
            val angle = 2.0 * PI * rotationSpeedHz * t

            // Interaural Level Difference (ILD): left/right gain modulation
            // When sound is to the left (angle = 3*PI/2), left is max, right is shadowed
            val pan = sin(angle).toFloat() // -1.0 (Left) to +1.0 (Right)
            val leftGain = sqrt(0.5f * (1.0f - pan))
            val rightGain = sqrt(0.5f * (1.0f + pan))

            val dryL = stereo.samples[f * 2].toFloat() / Short.MAX_VALUE
            val dryR = stereo.samples[f * 2 + 1].toFloat() / Short.MAX_VALUE
            val monoSum = (dryL + dryR) * 0.5f

            // ITD calculation: slight time delay on contralateral ear
            val itdSamples = (abs(pan) * maxItdSamples).toInt().coerceIn(0, maxItdSamples)

            itdBufferL[itdWriteIdx] = monoSum
            itdBufferR[itdWriteIdx] = monoSum

            val readIdxL = (itdWriteIdx - (if (pan > 0) itdSamples else 0) + itdBufferL.size) % itdBufferL.size
            val readIdxR = (itdWriteIdx - (if (pan < 0) itdSamples else 0) + itdBufferR.size) % itdBufferR.size

            val pannedL = itdBufferL[readIdxL] * leftGain
            val pannedR = itdBufferR[readIdxR] * rightGain

            // Reverb room reflection simulation
            val delayedReverb = reverbBuffer[reverbWriteIdx]
            reverbBuffer[reverbWriteIdx] = monoSum + delayedReverb * 0.45f

            val finalL = pannedL + delayedReverb * reverbDepth * 0.5f
            val finalR = pannedR + delayedReverb * reverbDepth * 0.5f

            itdWriteIdx = (itdWriteIdx + 1) % itdBufferL.size
            reverbWriteIdx = (reverbWriteIdx + 1) % reverbDelaySamples

            outSamples[f * 2] = (finalL * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            outSamples[f * 2 + 1] = (finalR * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return stereo.copy(samples = outSamples)
    }
}
