package sark.audio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioManipulationTest {

    @Test
    fun testAudioTrimmer() {
        val tone = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 2.0, // 2000 ms
            channels = 1
        )
        assertEquals(88200, tone.samples.size)

        val trimmed = AudioTrimmer.trim(tone, 500, 1500)
        // 1000 ms = 44100 samples
        assertEquals(44100, trimmed.samples.size)
        assertEquals(1000L, trimmed.header.durationMs)

        val cut = AudioTrimmer.cutOut(tone, 500, 1500)
        // 2000 ms - 1000 ms = 1000 ms = 44100 samples
        assertEquals(44100, cut.samples.size)
    }

    @Test
    fun testAudioMerger() {
        val tone1 = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            channels = 1
        )
        val tone2 = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 880.0,
            durationSeconds = 1.0,
            channels = 1
        )

        val merged = AudioMerger.mergeTracks(listOf(tone1, tone2))
        assertEquals(tone1.samples.size + tone2.samples.size, merged.samples.size)
        assertEquals(2000L, merged.header.durationMs)
    }

    @Test
    fun testAudioGainProcessor() {
        val tone = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 0.5,
            channels = 1,
            amplitude = 0.2f
        )

        val boosted = AudioGainProcessor.applyGain(tone, 2.0f)
        val normalized = AudioGainProcessor.normalize(tone, 0.95f)

        var peakOriginal = 0
        for (s in tone.samples) if (abs(s.toInt()) > peakOriginal) peakOriginal = abs(s.toInt())

        var peakBoosted = 0
        for (s in boosted.samples) if (abs(s.toInt()) > peakBoosted) peakBoosted = abs(s.toInt())

        var peakNorm = 0
        for (s in normalized.samples) if (abs(s.toInt()) > peakNorm) peakNorm = abs(s.toInt())

        assertEquals(peakOriginal * 2, peakBoosted)
        assertTrue(peakNorm >= (Short.MAX_VALUE * 0.94f).toInt())
    }

    @Test
    fun testAudioSpeedAndResample() {
        val tone = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            channels = 1
        )

        val speed2x = AudioTimePitchProcessor.changeSpeed(tone, 2.0f)
        assertEquals(tone.samples.size / 2, speed2x.samples.size)

        val resampled = AudioTimePitchProcessor.resample(tone, 22050)
        assertEquals(22050, resampled.header.sampleRate)
        assertEquals(tone.samples.size / 2, resampled.samples.size)

        val reversed = AudioTimePitchProcessor.reverse(tone)
        assertEquals(tone.samples.size, reversed.samples.size)
        assertEquals(tone.samples.first(), reversed.samples.last())
    }
}
