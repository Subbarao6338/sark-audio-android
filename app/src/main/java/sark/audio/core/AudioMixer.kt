package sark.audio.core

import kotlin.math.abs
import kotlin.math.tanh

/**
 * 11. Audio Mixer
 * Mixes multiple audio tracks simultaneously (parallel multi-track summing)
 * with individual track gain scaling and soft-knee hyperbolic tangent (tanh) limiter to prevent clipping.
 */
object AudioMixer {

    data class MixTrack(
        val audioData: AudioData,
        val volume: Float = 1.0f,
        val delayOffsetMs: Long = 0L
    )

    /**
     * Mixes multiple tracks in parallel into a single summed output.
     */
    fun mixTracks(
        tracks: List<MixTrack>,
        targetSampleRate: Int = 44100,
        targetChannels: Int = 2
    ): AudioData {
        if (tracks.isEmpty()) {
            return AudioSilenceGenerator.generateSilence(500L, targetSampleRate, targetChannels)
        }

        // Standardize all tracks to target sample rate and channels
        val processedTracks = tracks.map { track ->
            var data = track.audioData
            if (data.header.sampleRate != targetSampleRate) {
                data = AudioTimePitchProcessor.resample(data, targetSampleRate)
            }
            if (data.header.channels != targetChannels) {
                data = if (targetChannels == 2) {
                    AudioMerger.monoToStereo(data)
                } else {
                    AudioMerger.stereoToMono(data)
                }
            }
            track.copy(audioData = data)
        }

        // Determine total duration
        var maxTotalFrames = 0
        for (track in processedTracks) {
            val offsetFrames = ((track.delayOffsetMs * targetSampleRate) / 1000L).toInt()
            val trackFrames = track.audioData.samples.size / targetChannels
            val endFrames = offsetFrames + trackFrames
            if (endFrames > maxTotalFrames) {
                maxTotalFrames = endFrames
            }
        }

        // Floating point summing buffer
        val sumBuffer = FloatArray(maxTotalFrames * targetChannels)

        for (track in processedTracks) {
            val offsetFrames = ((track.delayOffsetMs * targetSampleRate) / 1000L).toInt()
            val samples = track.audioData.samples
            val vol = track.volume
            val trackFrames = samples.size / targetChannels

            for (f in 0 until trackFrames) {
                val destFrame = offsetFrames + f
                for (c in 0 until targetChannels) {
                    val sampleFloat = samples[f * targetChannels + c].toFloat() / Short.MAX_VALUE
                    sumBuffer[destFrame * targetChannels + c] += sampleFloat * vol
                }
            }
        }

        // Render to ShortArray with soft saturation limiting
        val finalSamples = ShortArray(sumBuffer.size)
        for (i in sumBuffer.indices) {
            val raw = sumBuffer[i]
            // Soft clipping using tanh
            val limited = if (abs(raw) > 1.0f) {
                tanh(raw.toDouble()).toFloat()
            } else {
                raw
            }
            finalSamples[i] = (limited * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val header = WavHeader(
            channels = targetChannels,
            sampleRate = targetSampleRate,
            bitsPerSample = 16,
            dataSize = finalSamples.size * 2
        )
        return AudioData(header, finalSamples)
    }
}
