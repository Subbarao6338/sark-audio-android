package sark.audio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioMetadataInspectorTest {

    @Test
    fun testMetadataInspection() {
        val audioData = WavFileProcessor.generateSineWave(
            sampleRate = 48000,
            frequencyHz = 440.0,
            durationSeconds = 2.5,
            channels = 2,
            amplitude = 0.5f
        )

        val meta = AudioMetadataInspector.inspectAudioData(audioData, "test_track.wav")

        assertEquals("test_track.wav", meta.fileName)
        assertEquals(48000, meta.sampleRate)
        assertEquals(2, meta.channels)
        assertEquals(16, meta.bitsPerSample)
        assertEquals(2500L, meta.durationMs)
        assertEquals("00:02.5", meta.durationFormatted)
        assertEquals("PCM / RIFF WAV", meta.format)
        assertNotNull(meta.fileSizeFormatted)
    }
}
