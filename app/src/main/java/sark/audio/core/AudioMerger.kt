package sark.audio.core

import kotlin.math.min

/**
 * Audio merging, concatenation, and crossfading processor.
 */
object AudioMerger {

    /**
     * Sequentially concatenate multiple [AudioData] tracks into a single unified track.
     * Automatically handles channel and sample rate alignment.
     */
    fun mergeTracks(tracks: List<AudioData>, crossfadeMs: Int = 0): AudioData {
        if (tracks.isEmpty()) {
            return AudioData(WavHeader(), ShortArray(0))
        }
        if (tracks.size == 1) {
            return tracks.first()
        }

        // Determine target specs (use highest sample rate and stereo if any track is stereo)
        val targetSampleRate = tracks.maxOf { it.header.sampleRate }
        val targetChannels = tracks.maxOf { it.header.channels }

        // Normalize all tracks to target format
        val normalizedTracks = tracks.map { track ->
            alignFormat(track, targetSampleRate, targetChannels)
        }

        if (crossfadeMs <= 0) {
            // Direct concatenation
            val totalSamples = normalizedTracks.sumOf { it.samples.size }
            val mergedSamples = ShortArray(totalSamples)
            var offset = 0
            for (t in normalizedTracks) {
                System.arraycopy(t.samples, 0, mergedSamples, offset, t.samples.size)
                offset += t.samples.size
            }
            val header = WavHeader(
                channels = targetChannels,
                sampleRate = targetSampleRate,
                bitsPerSample = 16,
                dataSize = mergedSamples.size * 2
            )
            return AudioData(header, mergedSamples)
        }

        // Crossfade concatenation
        var current = normalizedTracks[0]
        for (i in 1 until normalizedTracks.size) {
            current = crossfadePair(current, normalizedTracks[i], crossfadeMs)
        }
        return current
    }

    private fun crossfadePair(trackA: AudioData, trackB: AudioData, crossfadeMs: Int): AudioData {
        val sampleRate = trackA.header.sampleRate
        val channels = trackA.header.channels
        val crossfadeFrames = (crossfadeMs * sampleRate) / 1000
        val crossfadeSamples = crossfadeFrames * channels

        val actualCrossfade = min(crossfadeSamples, min(trackA.samples.size, trackB.samples.size))
        val nonOverlapA = trackA.samples.size - actualCrossfade
        val nonOverlapB = trackB.samples.size - actualCrossfade

        val outputSize = nonOverlapA + actualCrossfade + nonOverlapB
        val outSamples = ShortArray(outputSize)

        // Copy non-overlapped part of A
        System.arraycopy(trackA.samples, 0, outSamples, 0, nonOverlapA)

        // Crossfade region
        for (i in 0 until actualCrossfade) {
            val progress = i.toFloat() / actualCrossfade
            val valA = trackA.samples[nonOverlapA + i].toInt()
            val valB = trackB.samples[i].toInt()
            val blended = ((1f - progress) * valA + progress * valB).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outSamples[nonOverlapA + i] = blended.toShort()
        }

        // Copy remainder of B
        System.arraycopy(trackB.samples, actualCrossfade, outSamples, nonOverlapA + actualCrossfade, nonOverlapB)

        return AudioData(
            header = trackA.header.copy(dataSize = outSamples.size * 2),
            samples = outSamples
        )
    }

    /**
     * Resamples and expands channels if needed.
     */
    fun alignFormat(track: AudioData, targetSampleRate: Int, targetChannels: Int): AudioData {
        var current = track
        if (current.header.sampleRate != targetSampleRate) {
            current = AudioTimePitchProcessor.resample(current, targetSampleRate)
        }
        if (current.header.channels != targetChannels) {
            current = if (targetChannels == 2 && current.header.channels == 1) {
                monoToStereo(current)
            } else if (targetChannels == 1 && current.header.channels == 2) {
                stereoToMono(current)
            } else {
                current
            }
        }
        return current
    }

    fun monoToStereo(audio: AudioData): AudioData {
        val newSamples = ShortArray(audio.samples.size * 2)
        for (i in audio.samples.indices) {
            val s = audio.samples[i]
            newSamples[i * 2] = s
            newSamples[i * 2 + 1] = s
        }
        return AudioData(
            header = audio.header.copy(channels = 2, dataSize = newSamples.size * 2),
            samples = newSamples
        )
    }

    fun stereoToMono(audio: AudioData): AudioData {
        val frameCount = audio.samples.size / 2
        val newSamples = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            val left = audio.samples[i * 2].toInt()
            val right = audio.samples[i * 2 + 1].toInt()
            newSamples[i] = ((left + right) / 2).toShort()
        }
        return AudioData(
            header = audio.header.copy(channels = 1, dataSize = newSamples.size * 2),
            samples = newSamples
        )
    }
}
