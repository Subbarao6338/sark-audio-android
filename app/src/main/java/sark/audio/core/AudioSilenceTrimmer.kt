package sark.audio.core

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * 6. Audio Silence Detector & Trimmer
 * Strips leading, trailing, or all internal silence below a threshold in decibels (dBFS).
 */
object AudioSilenceTrimmer {

    /**
     * Converts a dBFS threshold (e.g. -40.0 dB) to short amplitude.
     */
    fun dBToThreshold(dB: Double): Int {
        val linear = 10.0.pow(dB / 20.0)
        return (linear * Short.MAX_VALUE).toInt().coerceIn(1, Short.MAX_VALUE.toInt())
    }

    /**
     * Trims leading and trailing silence where audio is below [thresholdDb].
     */
    fun trimSilence(
        audioData: AudioData,
        thresholdDb: Double = -45.0,
        windowSizeFrames: Int = 256
    ): AudioData {
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels
        val ampThreshold = dBToThreshold(thresholdDb)

        var firstAudibleFrame = -1
        var lastAudibleFrame = -1

        var frame = 0
        while (frame < totalFrames) {
            val end = (frame + windowSizeFrames).coerceAtMost(totalFrames)
            var maxAmp = 0
            for (f in frame until end) {
                for (c in 0 until channels) {
                    val s = abs(audioData.samples[f * channels + c].toInt())
                    if (s > maxAmp) maxAmp = s
                }
            }

            if (maxAmp >= ampThreshold) {
                if (firstAudibleFrame == -1) {
                    firstAudibleFrame = frame
                }
                lastAudibleFrame = end
            }
            frame += windowSizeFrames
        }

        if (firstAudibleFrame == -1 || lastAudibleFrame <= firstAudibleFrame) {
            // Entire audio is silence or silent buffer
            return AudioSilenceGenerator.generateSilence(100L, audioData.header.sampleRate, channels)
        }

        val keptFrames = lastAudibleFrame - firstAudibleFrame
        val keptSamples = keptFrames * channels
        val newSamples = ShortArray(keptSamples)
        System.arraycopy(audioData.samples, firstAudibleFrame * channels, newSamples, 0, keptSamples)

        return AudioData(
            header = audioData.header.copy(dataSize = keptSamples * 2),
            samples = newSamples
        )
    }
}
