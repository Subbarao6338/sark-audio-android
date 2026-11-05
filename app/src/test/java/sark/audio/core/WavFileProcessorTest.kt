package sark.audio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WavFileProcessorTest {

    @Test
    fun testWavHeaderSerializationAndParsing() {
        val originalHeader = WavHeader(
            channels = 2,
            sampleRate = 48000,
            bitsPerSample = 16,
            dataSize = 192000
        )

        val output = ByteArrayOutputStream()
        originalHeader.write(output)
        val bytes = output.toByteArray()

        assertEquals(44, bytes.size)

        val parsedHeader = WavHeader.read(ByteArrayInputStream(bytes))
        assertEquals(originalHeader.channels, parsedHeader.channels)
        assertEquals(originalHeader.sampleRate, parsedHeader.sampleRate)
        assertEquals(originalHeader.bitsPerSample, parsedHeader.bitsPerSample)
        assertEquals(originalHeader.dataSize, parsedHeader.dataSize)
        assertEquals(originalHeader.byteRate, parsedHeader.byteRate)
        assertEquals(originalHeader.blockAlign, parsedHeader.blockAlign)
        assertEquals(1000L, parsedHeader.durationMs)
    }

    @Test
    fun testGenerateAndDecodeWav() {
        val audioData = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 0.5,
            channels = 1,
            amplitude = 0.5f
        )

        val wavBytes = WavFileProcessor.encodeToWavBytes(audioData)
        assertTrue(wavBytes.size > 44)

        val decoded = WavFileProcessor.decodeWav(wavBytes)
        assertEquals(1, decoded.header.channels)
        assertEquals(44100, decoded.header.sampleRate)
        assertEquals(audioData.samples.size, decoded.samples.size)

        // Compare sample values
        for (i in audioData.samples.indices) {
            assertEquals(audioData.samples[i], decoded.samples[i])
        }
    }

    @Test
    fun testExtractWaveformAmplitudes() {
        val audioData = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            channels = 1,
            amplitude = 0.8f
        )

        val amplitudes = WavFileProcessor.extractWaveformAmplitudes(audioData.samples, audioData.header.channels, 50)
        assertEquals(50, amplitudes.size)
        // Check that amplitude is non-zero and roughly around 0.8
        var maxAmp = 0f
        for (a in amplitudes) {
            if (a > maxAmp) maxAmp = a
        }
        assertTrue("Expected amplitude around 0.8, found $maxAmp", maxAmp in 0.7f..0.9f)
    }
}
