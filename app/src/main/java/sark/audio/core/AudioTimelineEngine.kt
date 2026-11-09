package sark.audio.core

import kotlin.math.*

/**
 * Advanced multi-track timeline editing engine.
 * Supports non-destructive clip models, splitting, splicing, volume/pan automation,
 * offset shifting, rate & semitone pitch adjustments, and bounded summation rendering.
 */
object AudioTimelineEngine {

    /**
     * Represents a single audio clip placed on a timeline track.
     */
    data class TimelineClip(
        val id: String,
        val name: String,
        val audioData: AudioData,
        val timelineStartMs: Long = 0L,
        val clipStartOffsetMs: Long = 0L,
        val durationMs: Long = audioData.header.durationMs,
        val volume: Float = 1.0f,
        val pan: Float = 0.0f, // -1.0 (Left) to +1.0 (Right)
        val speed: Float = 1.0f,
        val pitchSemitones: Int = 0,
        val fadeInMs: Long = 0L,
        val fadeOutMs: Long = 0L,
        val isMuted: Boolean = false,
        val isReversed: Boolean = false
    ) {
        val timelineEndMs: Long get() = timelineStartMs + durationMs
    }

    /**
     * Represents a track containing one or more sequential or concurrent clips.
     */
    data class TimelineTrack(
        val id: String,
        val name: String,
        val clips: List<TimelineClip> = emptyList(),
        val volume: Float = 1.0f,
        val pan: Float = 0.0f,
        val isMuted: Boolean = false,
        val isSolo: Boolean = false
    )

    /**
     * Complete timeline project state.
     */
    data class TimelineProject(
        val id: String = "project_1",
        val name: String = "Timeline Project",
        val sampleRate: Int = 44100,
        val channels: Int = 2,
        val tracks: List<TimelineTrack> = emptyList(),
        val masterVolume: Float = 1.0f
    ) {
        val totalDurationMs: Long
            get() = tracks.flatMap { it.clips }.maxOfOrNull { it.timelineEndMs } ?: 0L
    }

    /**
     * Splits a clip at [splitTimeMs] relative to the timeline start of the clip.
     * Returns a pair of resulting clips, or single clip if split point is invalid.
     */
    fun splitClip(clip: TimelineClip, splitTimeMs: Long): Pair<TimelineClip, TimelineClip> {
        val localOffset = (splitTimeMs - clip.timelineStartMs).coerceIn(1L, (clip.durationMs - 1).coerceAtLeast(1L))

        val firstClip = clip.copy(
            id = "${clip.id}_part1",
            name = "${clip.name} (Part 1)",
            durationMs = localOffset,
            fadeOutMs = min(clip.fadeOutMs, localOffset / 2)
        )

        val secondClip = clip.copy(
            id = "${clip.id}_part2",
            name = "${clip.name} (Part 2)",
            timelineStartMs = clip.timelineStartMs + localOffset,
            clipStartOffsetMs = clip.clipStartOffsetMs + localOffset,
            durationMs = clip.durationMs - localOffset,
            fadeInMs = min(clip.fadeInMs, (clip.durationMs - localOffset) / 2)
        )

        return Pair(firstClip, secondClip)
    }

    /**
     * Renders a clip into rendered PCM samples respecting offset, trim duration,
     * speed, pitch, volume, pan, and fade envelopes.
     */
    fun renderClip(clip: TimelineClip, targetSampleRate: Int, targetChannels: Int): AudioData {
        var audio = clip.audioData

        // 1. Invert / Reverse if enabled
        if (clip.isReversed) {
            audio = AudioTimePitchProcessor.reverse(audio)
        }

        // 2. Pitch shifting if semitones != 0
        if (clip.pitchSemitones != 0) {
            val factor = 2.0.pow(clip.pitchSemitones / 12.0).toFloat()
            audio = AudioTimePitchProcessor.changePitch(audio, factor)
        }

        // 3. Speed / Tempo change if speed != 1.0
        if (clip.speed > 0f && clip.speed != 1.0f) {
            audio = AudioTimePitchProcessor.changeSpeed(audio, clip.speed)
        }

        // 4. Sample accurate slice (clipStartOffsetMs to durationMs)
        val endMs = min(audio.header.durationMs, clip.clipStartOffsetMs + clip.durationMs)
        audio = AudioTrimmer.trim(audio, clip.clipStartOffsetMs, endMs)

        // 5. Fade In & Fade Out
        if (clip.fadeInMs > 0L) {
            audio = AudioFadeProcessor.fadeIn(audio, clip.fadeInMs, AudioFadeProcessor.FadeCurve.S_CURVE)
        }
        if (clip.fadeOutMs > 0L) {
            audio = AudioFadeProcessor.fadeOut(audio, clip.fadeOutMs, AudioFadeProcessor.FadeCurve.S_CURVE)
        }

        // 6. Resample to timeline target sample rate if needed
        if (audio.header.sampleRate != targetSampleRate) {
            audio = AudioTimePitchProcessor.resample(audio, targetSampleRate)
        }

        // 7. Ensure target channel count (Mono / Stereo)
        if (audio.header.channels != targetChannels) {
            audio = if (targetChannels == 2 && audio.header.channels == 1) {
                AudioMerger.monoToStereo(audio)
            } else if (targetChannels == 1 && audio.header.channels == 2) {
                AudioMerger.stereoToMono(audio)
            } else {
                audio
            }
        }

        // 8. Volume and Pan application
        val finalSamples = audio.samples.clone()
        val totalFrames = finalSamples.size / targetChannels
        val effectiveVol = if (clip.isMuted) 0f else clip.volume

        if (targetChannels == 2) {
            // Constant power or linear pan
            val panClamped = clip.pan.coerceIn(-1.0f, 1.0f)
            val leftGain = (effectiveVol * (1f - panClamped) / 2f).coerceIn(0f, 2f)
            val rightGain = (effectiveVol * (1f + panClamped) / 2f).coerceIn(0f, 2f)

            for (f in 0 until totalFrames) {
                val lIdx = f * 2
                val rIdx = f * 2 + 1
                finalSamples[lIdx] = (finalSamples[lIdx] * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                finalSamples[rIdx] = (finalSamples[rIdx] * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        } else {
            for (i in finalSamples.indices) {
                finalSamples[i] = (finalSamples[i] * effectiveVol).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        return AudioData(
            header = audio.header.copy(
                sampleRate = targetSampleRate,
                channels = targetChannels,
                dataSize = finalSamples.size * 2
            ),
            samples = finalSamples
        )
    }

    /**
     * Renders an entire TimelineProject down to a unified master AudioData file
     * with multi-channel summing and tanh soft-saturation limiting.
     */
    fun renderProject(project: TimelineProject): AudioData {
        val totalMs = project.totalDurationMs.coerceAtLeast(100L)
        val sampleRate = project.sampleRate
        val channels = project.channels
        val totalFrames = ((totalMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
        val totalSamples = totalFrames * channels

        val sumBuffer = DoubleArray(totalSamples)

        val hasSolo = project.tracks.any { it.isSolo }

        for (track in project.tracks) {
            if (track.isMuted) continue
            if (hasSolo && !track.isSolo) continue

            val trackGain = track.volume
            val trackPan = track.pan.coerceIn(-1f, 1f)

            for (clip in track.clips) {
                if (clip.isMuted) continue

                val renderedClip = renderClip(clip, sampleRate, channels)
                val clipStartFrame = ((clip.timelineStartMs * sampleRate) / 1000L).toInt()
                val clipFrames = renderedClip.samples.size / channels

                for (cf in 0 until clipFrames) {
                    val targetFrame = clipStartFrame + cf
                    if (targetFrame in 0 until totalFrames) {
                        for (c in 0 until channels) {
                            val inSample = renderedClip.samples[cf * channels + c].toDouble()
                            val panMult = if (channels == 2) {
                                if (c == 0) (1.0 - trackPan) / 2.0 else (1.0 + trackPan) / 2.0
                            } else 1.0

                            val sumIdx = targetFrame * channels + c
                            sumBuffer[sumIdx] += inSample * trackGain * panMult
                        }
                    }
                }
            }
        }

        // Apply Master Volume and soft saturation limiter
        val outSamples = ShortArray(totalSamples)
        val masterGain = project.masterVolume.toDouble()

        for (i in 0 until totalSamples) {
            val multiplied = (sumBuffer[i] * masterGain) / 32768.0
            // Tanh soft limiting saturation
            val limited = tanh(multiplied) * 32767.0
            outSamples[i] = limited.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return AudioData(
            header = WavHeader(
                channels = channels,
                sampleRate = sampleRate,
                bitsPerSample = 16,
                dataSize = totalSamples * 2
            ),
            samples = outSamples
        )
    }
}
