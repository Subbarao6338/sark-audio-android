package sark.audio.core

import kotlin.math.*

/**
 * Chorus & Modulation Effect Engine
 * Multi-voice chorus effect with dual delay-line LFO modulation and stereo phase spread
 * to produce thick, lush vocal and instrumental doubling.
 */
object AudioChorusProcessor {

    /**
     * Applies stereo chorus effect.
     * @param rateHz LFO modulation speed in Hertz (e.g. 1.2 Hz)
     * @param depthMs Modulation depth in milliseconds (e.g. 3.0 ms)
     * @param mix Wet/dry blend (0.0 to 1.0)
     */
    fun applyChorus(
        audioData: AudioData,
        rateHz: Double = 1.2,
        depthMs: Double = 3.0,
        mix: Float = 0.5f
    ): AudioData {
        val stereo = if (audioData.header.channels == 1) AudioMerger.monoToStereo(audioData) else audioData
        val sampleRate = stereo.header.sampleRate
        val totalFrames = stereo.samples.size / 2

        val baseDelayMs = 20.0
        val maxDelayFrames = (((baseDelayMs + depthMs + 5.0) * sampleRate) / 1000.0).toInt()
        val ringL = FloatArray(maxDelayFrames)
        val ringR = FloatArray(maxDelayFrames)
        var writeIdx = 0

        val outSamples = ShortArray(stereo.samples.size)

        for (f in 0 until totalFrames) {
            val t = f.toDouble() / sampleRate
            // LFOs for left and right with 90 degree phase offset
            val lfoL = sin(2.0 * PI * rateHz * t)
            val lfoR = cos(2.0 * PI * rateHz * t)

            val currentDelayMsL = baseDelayMs + depthMs * lfoL
            val currentDelayMsR = baseDelayMs + depthMs * lfoR

            val delayFramesL = (currentDelayMsL * sampleRate / 1000.0)
            val delayFramesR = (currentDelayMsR * sampleRate / 1000.0)

            val dryL = stereo.samples[f * 2].toFloat() / Short.MAX_VALUE
            val dryR = stereo.samples[f * 2 + 1].toFloat() / Short.MAX_VALUE

            ringL[writeIdx] = dryL
            ringR[writeIdx] = dryR

            // Linear interpolation reading from delay ring
            val readPosL = (writeIdx - delayFramesL + ringL.size) % ringL.size
            val idxL1 = readPosL.toInt()
            val fracL = (readPosL - idxL1).toFloat()
            val idxL2 = (idxL1 + 1) % ringL.size
            val wetL = ringL[idxL1] * (1f - fracL) + ringL[idxL2] * fracL

            val readPosR = (writeIdx - delayFramesR + ringR.size) % ringR.size
            val idxR1 = readPosR.toInt()
            val fracR = (readPosR - idxR1).toFloat()
            val idxR2 = (idxR1 + 1) % ringR.size
            val wetR = ringR[idxR1] * (1f - fracR) + ringR[idxR2] * fracR

            val outL = dryL * (1f - mix) + wetL * mix
            val outR = dryR * (1f - mix) + wetR * mix

            outSamples[f * 2] = (outL * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            outSamples[f * 2 + 1] = (outR * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            writeIdx = (writeIdx + 1) % maxDelayFrames
        }

        return stereo.copy(samples = outSamples)
    }
}
