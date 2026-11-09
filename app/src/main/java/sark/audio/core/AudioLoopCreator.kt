package sark.audio.core

/**
 * 21. Audio Loop Creator
 * Seamlessly loops audio N times with optional crossfading at cycle seams to avoid clicks and pops.
 */
object AudioLoopCreator {

    /**
     * Loops [audioData] [repeatCount] times.
     * @param crossfadeDurationMs Crossfade blend duration at seam transitions.
     */
    fun loop(
        audioData: AudioData,
        repeatCount: Int = 2,
        crossfadeDurationMs: Long = 50L
    ): AudioData {
        if (repeatCount <= 1) return audioData

        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val originalFrames = audioData.samples.size / channels
        val crossfadeFrames = ((crossfadeDurationMs * sampleRate) / 1000L).toInt().coerceIn(0, originalFrames / 4)

        if (crossfadeFrames == 0) {
            // Simple concatenation
            val totalSamples = audioData.samples.size * repeatCount
            val newSamples = ShortArray(totalSamples)
            for (r in 0 until repeatCount) {
                System.arraycopy(audioData.samples, 0, newSamples, r * audioData.samples.size, audioData.samples.size)
            }
            return AudioData(
                header = audioData.header.copy(dataSize = totalSamples * 2),
                samples = newSamples
            )
        }

        // Loop with crossfade:
        // Total frames = originalFrames + (repeatCount - 1) * (originalFrames - crossfadeFrames)
        val framesPerSubsequentLoop = originalFrames - crossfadeFrames
        val totalFrames = originalFrames + (repeatCount - 1) * framesPerSubsequentLoop
        val newSamples = ShortArray(totalFrames * channels)

        // Copy first instance
        System.arraycopy(audioData.samples, 0, newSamples, 0, originalFrames * channels)

        var currentOffsetFrames = originalFrames - crossfadeFrames
        for (r in 1 until repeatCount) {
            // Apply crossfade over crossfadeFrames
            for (f in 0 until crossfadeFrames) {
                val progress = f.toFloat() / crossfadeFrames
                val destFrame = currentOffsetFrames + f
                for (c in 0 until channels) {
                    val destIdx = destFrame * channels + c
                    val existingSample = newSamples[destIdx].toFloat()
                    val newSample = audioData.samples[f * channels + c].toFloat()
                    val blended = existingSample * (1.0f - progress) + newSample * progress
                    newSamples[destIdx] = blended.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            // Copy remainder of the loop
            val remainingFrames = originalFrames - crossfadeFrames
            val copyStartDest = (currentOffsetFrames + crossfadeFrames) * channels
            val copyStartSrc = crossfadeFrames * channels
            val copyLength = (remainingFrames) * channels
            System.arraycopy(audioData.samples, copyStartSrc, newSamples, copyStartDest, copyLength)

            currentOffsetFrames += framesPerSubsequentLoop
        }

        return AudioData(
            header = audioData.header.copy(dataSize = newSamples.size * 2),
            samples = newSamples
        )
    }
}
