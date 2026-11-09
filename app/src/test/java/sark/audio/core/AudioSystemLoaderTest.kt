package sark.audio.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AudioSystemLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDecodeStandardWavFile() {
        val testAudio = WavFileProcessor.generateSineWave(
            sampleRate = 44100,
            frequencyHz = 440.0,
            durationSeconds = 1.0,
            channels = 2,
            amplitude = 0.7f
        )
        val file = tempFolder.newFile("sample_audio.wav")
        WavFileProcessor.encodeToWavFile(testAudio, file)

        assertTrue(file.exists())
        assertTrue(file.length() > 44)

        val decoded = AudioSystemLoader.decodeAudioFile(file)
        assertNotNull(decoded)
        assertEquals(44100, decoded.header.sampleRate)
        assertEquals(2, decoded.header.channels)
        assertEquals(16, decoded.header.bitsPerSample)
        assertEquals(testAudio.samples.size, decoded.samples.size)
        // Check sample contents match
        assertArrayEquals(testAudio.samples, decoded.samples)
    }

    @Test
    fun testDecodeNonExistentFileThrows() {
        val nonExistent = File(tempFolder.root, "does_not_exist.wav")
        try {
            AudioSystemLoader.decodeAudioFile(nonExistent)
            fail("Expected IllegalArgumentException for non-existent file")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("does not exist") == true)
        }
    }

    @Test
    fun testDecode8BitWavThroughLoader() {
        val testAudio = WavFileProcessor.generateSineWave(
            sampleRate = 22050,
            frequencyHz = 300.0,
            durationSeconds = 0.5,
            channels = 1
        )
        val bytes8 = AudioBitDepthConverter.convertTo8Bit(testAudio)
        val header8 = testAudio.header.copy(
            sampleRate = 22050,
            channels = 1,
            bitsPerSample = 8,
            dataSize = bytes8.size
        )
        val file8 = tempFolder.newFile("audio_8bit.wav")
        val baos = java.io.ByteArrayOutputStream()
        header8.write(baos)
        baos.write(bytes8)
        file8.writeBytes(baos.toByteArray())

        val decoded = AudioSystemLoader.decodeAudioFile(file8)
        assertEquals(22050, decoded.header.sampleRate)
        assertEquals(1, decoded.header.channels)
        assertEquals(16, decoded.header.bitsPerSample) // 8-bit converts to 16-bit PCM
        assertEquals(testAudio.samples.size, decoded.samples.size)
    }

    @Test
    fun testSaveAndManageExportFlow() {
        val managerDir = tempFolder.newFolder("managed_lib")
        val manager = AudioFileManager(managerDir)

        val sample = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 500.0, durationSeconds = 1.0)
        val savedFile = manager.saveAudio(sample, "imported_from_phone")
        assertTrue(savedFile.exists())
        assertEquals("imported_from_phone.wav", savedFile.name)

        val list = manager.listFiles()
        assertEquals(1, list.size)
        assertEquals("imported_from_phone.wav", list[0].name)

        val loadedAgain = AudioSystemLoader.decodeAudioFile(savedFile)
        assertEquals(sample.samples.size, loadedAgain.samples.size)
    }
}
