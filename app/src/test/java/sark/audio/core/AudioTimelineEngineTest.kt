package sark.audio.core

import org.junit.Assert.*
import org.junit.Test

class AudioTimelineEngineTest {

    @Test
    fun testTimelineClipSplitting() {
        val audio = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.SINE,
            frequencyHz = 440.0,
            durationSeconds = 2.0,
            sampleRate = 44100
        )
        val clip = AudioTimelineEngine.TimelineClip(
            id = "clip_original",
            name = "Test Clip",
            audioData = audio,
            timelineStartMs = 0L,
            durationMs = 2000L
        )

        val (part1, part2) = AudioTimelineEngine.splitClip(clip, 800L)
        assertEquals(800L, part1.durationMs)
        assertEquals(0L, part1.timelineStartMs)
        assertEquals(1200L, part2.durationMs)
        assertEquals(800L, part2.timelineStartMs)
        assertEquals(800L, part2.clipStartOffsetMs)
    }

    @Test
    fun testTimelineProjectRendering() {
        val tone1 = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.SINE,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            sampleRate = 44100,
            channels = 2
        )
        val tone2 = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.TRIANGLE,
            frequencyHz = 880.0,
            durationSeconds = 1.0,
            sampleRate = 44100,
            channels = 2
        )

        val clip1 = AudioTimelineEngine.TimelineClip(
            id = "clip_1",
            name = "Tone 1",
            audioData = tone1,
            timelineStartMs = 0L,
            durationMs = 1000L,
            volume = 0.8f,
            pan = -0.5f
        )
        val clip2 = AudioTimelineEngine.TimelineClip(
            id = "clip_2",
            name = "Tone 2",
            audioData = tone2,
            timelineStartMs = 500L,
            durationMs = 1000L,
            volume = 0.8f,
            pan = 0.5f
        )

        val track1 = AudioTimelineEngine.TimelineTrack(id = "track_1", name = "Track 1", clips = listOf(clip1))
        val track2 = AudioTimelineEngine.TimelineTrack(id = "track_2", name = "Track 2", clips = listOf(clip2))

        val project = AudioTimelineEngine.TimelineProject(
            tracks = listOf(track1, track2),
            masterVolume = 1.0f
        )

        val rendered = AudioTimelineEngine.renderProject(project)
        assertNotNull(rendered)
        assertEquals(2, rendered.header.channels)
        assertEquals(44100, rendered.header.sampleRate)
        assertTrue(rendered.samples.isNotEmpty())
        assertEquals(1500L, project.totalDurationMs)
    }

    @Test
    fun testAdsrEnvelopeGeneration() {
        val adsr = AudioToneGenerator.AdsrEnvelope(
            attackSec = 0.1,
            decaySec = 0.1,
            sustainLevel = 0.5,
            releaseSec = 0.2
        )
        val audio = AudioToneGenerator.generateTone(
            type = AudioToneGenerator.WaveformType.SINE,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            sampleRate = 44100,
            channels = 1,
            envelope = adsr
        )
        assertNotNull(audio)
        assertTrue(audio.samples.isNotEmpty())
    }

    @Test
    fun testHarmonicsAndBrownNoiseGeneration() {
        val brown = AudioToneGenerator.generateTone(
            type = AudioToneGenerator.WaveformType.BROWN_NOISE,
            durationSeconds = 0.5,
            sampleRate = 44100
        )
        assertTrue(brown.samples.isNotEmpty())

        val harmonics = AudioToneGenerator.generateTone(
            type = AudioToneGenerator.WaveformType.HARMONICS,
            frequencyHz = 220.0,
            durationSeconds = 0.5,
            sampleRate = 44100,
            harmonicsWeights = doubleArrayOf(1.0, 0.5, 0.25)
        )
        assertTrue(harmonics.samples.isNotEmpty())
    }

    @Test
    fun testMixerSoloMutePan() {
        val tone1 = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 1.0)
        val tone2 = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SQUARE, 880.0, durationSeconds = 1.0)

        val mixed = AudioMixer.mixTracks(
            listOf(
                AudioMixer.MixTrack(tone1, volume = 0.8f, pan = -0.5f, isMuted = false, isSolo = true),
                AudioMixer.MixTrack(tone2, volume = 0.8f, pan = 0.5f, isMuted = false, isSolo = false)
            )
        )
        assertNotNull(mixed)
        assertTrue(mixed.samples.isNotEmpty())
    }
}
