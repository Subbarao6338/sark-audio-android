package sark.audio.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * 12. Audio Channel Processor
 * Stereo to Mono downmixing, Mono to Stereo upmixing, Channel Splitting, Channel Swapping,
 * and Constant-Power Stereo Panning (-1.0 Left to +1.0 Right).
 */
object AudioChannelProcessor {

    /**
     * Splits a stereo audio buffer into two separate mono [AudioData] tracks (Left, Right).
     */
    fun splitStereo(audioData: AudioData): Pair<AudioData, AudioData> {
        val channels = audioData.header.channels
        if (channels != 2) {
            // Already mono or non-stereo, duplicate as both
            val monoHeader = audioData.header.copy(channels = 1, dataSize = audioData.samples.size * 2)
            return Pair(audioData.copy(header = monoHeader), audioData.copy(header = monoHeader))
        }

        val frames = audioData.samples.size / 2
        val leftSamples = ShortArray(frames)
        val rightSamples = ShortArray(frames)

        for (i in 0 until frames) {
            leftSamples[i] = audioData.samples[i * 2]
            rightSamples[i] = audioData.samples[i * 2 + 1]
        }

        val monoHeader = audioData.header.copy(channels = 1, dataSize = frames * 2)
        return Pair(AudioData(monoHeader, leftSamples), AudioData(monoHeader, rightSamples))
    }

    /**
     * Swaps the left and right audio channels.
     */
    fun swapChannels(audioData: AudioData): AudioData {
        if (audioData.header.channels != 2) return audioData

        val frames = audioData.samples.size / 2
        val swapped = ShortArray(audioData.samples.size)

        for (i in 0 until frames) {
            swapped[i * 2] = audioData.samples[i * 2 + 1]
            swapped[i * 2 + 1] = audioData.samples[i * 2]
        }

        return audioData.copy(samples = swapped)
    }

    /**
     * Applies constant-power stereo panning (-1.0 full left, 0.0 center, +1.0 full right).
     */
    fun pan(audioData: AudioData, panPosition: Float): AudioData {
        val stereo = if (audioData.header.channels == 1) {
            AudioMerger.monoToStereo(audioData)
        } else {
            audioData
        }

        val clampedPan = panPosition.coerceIn(-1.0f, 1.0f)
        // Angle in radians between 0 (left) and PI/2 (right)
        val angle = ((clampedPan + 1.0f) / 2.0f) * (PI / 2.0)
        val leftGain = cos(angle).toFloat()
        val rightGain = sin(angle).toFloat()

        val frames = stereo.samples.size / 2
        val newSamples = ShortArray(stereo.samples.size)

        for (i in 0 until frames) {
            val left = (stereo.samples[i * 2] * leftGain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val right = (stereo.samples[i * 2 + 1] * rightGain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            newSamples[i * 2] = left
            newSamples[i * 2 + 1] = right
        }

        return stereo.copy(samples = newSamples)
    }
}
