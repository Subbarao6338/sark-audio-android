package sark.audio.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.math.abs

class ExtendedAudioToolsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAudioToneGeneratorAllWaveforms() {
        val waveforms = listOf(
            AudioToneGenerator.WaveformType.SINE,
            AudioToneGenerator.WaveformType.SQUARE,
            AudioToneGenerator.WaveformType.TRIANGLE,
            AudioToneGenerator.WaveformType.SAWTOOTH,
            AudioToneGenerator.WaveformType.WHITE_NOISE,
            AudioToneGenerator.WaveformType.PINK_NOISE,
            AudioToneGenerator.WaveformType.CHIRP
        )

        for (wf in waveforms) {
            val audio = AudioToneGenerator.generateTone(
                type = wf,
                frequencyHz = 440.0,
                durationSeconds = 0.25,
                sampleRate = 44100,
                channels = 1
            )
            assertEquals(44100 / 4, audio.samples.size)
            assertTrue(audio.samples.any { it != 0.toShort() })
        }
    }

    @Test
    fun testAudioDtmfAndMorse() {
        val dtmfKey = AudioDtmfGenerator.generateKeyTone('5', durationMs = 100L)
        assertTrue(dtmfKey.samples.isNotEmpty())

        val sequence = AudioDtmfGenerator.generateDialSequence("123", toneDurationMs = 50L, pauseDurationMs = 20L)
        assertTrue(sequence.samples.isNotEmpty())

        val morse = AudioDtmfGenerator.generateMorseCode("SOS", unitDurationMs = 30L)
        assertTrue(morse.samples.isNotEmpty())
    }

    @Test
    fun testSilenceGeneratorAndInserter() {
        val silence = AudioSilenceGenerator.generateSilence(durationMs = 200L, sampleRate = 44100, channels = 1)
        assertEquals(8820, silence.samples.size)
        assertTrue(silence.samples.all { it == 0.toShort() })

        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5)
        val inserted = AudioSilenceGenerator.insertSilence(tone, offsetMs = 250L, silenceDurationMs = 200L)
        assertEquals(tone.samples.size + 8820, inserted.samples.size)
    }

    @Test
    fun testAudioFadeProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 1.0, amplitude = 0.9f)
        val fadedIn = AudioFadeProcessor.fadeIn(tone, 200L, AudioFadeProcessor.FadeCurve.LINEAR)
        assertEquals(0.0, fadedIn.samples[0].toDouble(), 50.0) // very close to 0 at start

        val fadedOut = AudioFadeProcessor.fadeOut(tone, 200L, AudioFadeProcessor.FadeCurve.LINEAR)
        assertEquals(0.0, fadedOut.samples.last().toDouble(), 50.0) // very close to 0 at end
    }

    @Test
    fun testSilenceTrimmer() {
        val silence = AudioSilenceGenerator.generateSilence(300L, 44100, 1)
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5, amplitude = 0.8f)
        val combined = AudioMerger.mergeTracks(listOf(silence, tone, silence))

        val trimmed = AudioSilenceTrimmer.trimSilence(combined, thresholdDb = -30.0)
        assertTrue(trimmed.samples.size < combined.samples.size)
        assertTrue(trimmed.samples.size >= tone.samples.size * 0.9)
    }

    @Test
    fun testInvertProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.1, amplitude = 0.5f)
        val inverted = AudioInvertProcessor.invertPhase(tone)
        for (i in tone.samples.indices) {
            assertEquals(-tone.samples[i].toInt(), inverted.samples[i].toInt())
        }
    }

    @Test
    fun testAudioMixer() {
        val track1 = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5, amplitude = 0.3f)
        val track2 = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 880.0, durationSeconds = 0.5, amplitude = 0.3f)

        val mixed = AudioMixer.mixTracks(
            listOf(
                AudioMixer.MixTrack(track1, volume = 0.8f),
                AudioMixer.MixTrack(track2, volume = 0.8f, delayOffsetMs = 100L)
            ),
            targetSampleRate = 44100,
            targetChannels = 2
        )
        assertEquals(2, mixed.header.channels)
        assertTrue(mixed.samples.isNotEmpty())
    }

    @Test
    fun testAudioChannelProcessor() {
        val stereoTone = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2, channels = 2
        )
        val (left, right) = AudioChannelProcessor.splitStereo(stereoTone)
        assertEquals(1, left.header.channels)
        assertEquals(1, right.header.channels)
        assertEquals(stereoTone.samples.size / 2, left.samples.size)

        val pannedLeft = AudioChannelProcessor.pan(left, -1.0f)
        assertEquals(2, pannedLeft.header.channels)
        // Right channel should be approximately zero
        assertTrue(abs(pannedLeft.samples[1].toInt()) < 50)
    }

    @Test
    fun testAudioFilterProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.WHITE_NOISE, durationSeconds = 0.2)
        val lowPass = AudioFilterProcessor.applyFilter(tone, AudioFilterProcessor.FilterType.LOW_PASS, cutoffHz = 1000.0)
        assertEquals(tone.samples.size, lowPass.samples.size)

        val highPass = AudioFilterProcessor.applyFilter(tone, AudioFilterProcessor.FilterType.HIGH_PASS, cutoffHz = 3000.0)
        assertEquals(tone.samples.size, highPass.samples.size)
    }

    @Test
    fun testAudioDynamicsCompressor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5, amplitude = 0.95f)
        val compressed = AudioDynamicsCompressor.compress(
            tone,
            thresholdDb = -12.0,
            ratio = 4.0,
            attackMs = 1.0,
            makeupGainDb = 0.0
        )
        assertEquals(tone.samples.size, compressed.samples.size)

        // Compare steady-state audio after attack phase (> 500 samples)
        var peakOrig = 0
        for (i in 500 until tone.samples.size) {
            val s = abs(tone.samples[i].toInt())
            if (s > peakOrig) peakOrig = s
        }
        var peakComp = 0
        for (i in 500 until compressed.samples.size) {
            val s = abs(compressed.samples[i].toInt())
            if (s > peakComp) peakComp = s
        }

        assertTrue("Expected steady-state compressed peak ($peakComp) to be lower than original ($peakOrig)", peakComp < peakOrig)
    }

    @Test
    fun testAudioDelayProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5)
        val delayed = AudioDelayProcessor.applyDelay(tone, delayMs = 100L, feedback = 0.3f)
        assertEquals(tone.samples.size, delayed.samples.size)
    }

    @Test
    fun testAudioBitcrusherAndDistortion() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2)
        val crushed = AudioBitcrusherProcessor.process(tone, bitDepth = 4, downsampleFactor = 4)
        assertEquals(tone.samples.size, crushed.samples.size)

        val distorted = AudioDistortionProcessor.distort(tone, drive = 4.0f, mode = AudioDistortionProcessor.DistortionMode.SOFT_CLIP)
        assertEquals(tone.samples.size, distorted.samples.size)
    }

    @Test
    fun testBitDepthConverter() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.1)
        val bytes8 = AudioBitDepthConverter.convertTo8Bit(tone, useDither = false)
        assertEquals(tone.samples.size, bytes8.size)

        val restored16 = AudioBitDepthConverter.convertFrom8Bit(bytes8, tone.header.sampleRate, 1)
        assertEquals(tone.samples.size, restored16.samples.size)
    }

    @Test
    fun testLoopCreator() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2)
        val looped = AudioLoopCreator.loop(tone, repeatCount = 3, crossfadeDurationMs = 20L)
        assertTrue(looped.samples.size > tone.samples.size * 2)
    }

    @Test
    fun testSpectrumAnalyzer() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 1000.0, durationSeconds = 0.25, sampleRate = 44100)
        val result = AudioSpectrumAnalyzer.analyze(tone, fftSize = 1024)

        assertEquals(512, result.frequencies.size)
        assertEquals(512, result.magnitudesDb.size)
        // Dominant frequency should be around 1000 Hz
        assertTrue(abs(result.peakFrequencyHz - 1000.0) < 50.0)
    }

    @Test
    fun testAudioFileManager() {
        val dir = tempFolder.newFolder("audio_manager_test")
        val manager = AudioFileManager(dir)

        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2)
        val savedFile = manager.saveAudio(tone, "sample_track")
        assertTrue(savedFile.exists())

        var list = manager.listFiles()
        assertEquals(1, list.size)
        assertEquals("sample_track.wav", list[0].name)

        val renamed = manager.renameFile(savedFile, "renamed_track")
        assertNotNull(renamed)
        assertTrue(renamed!!.exists())

        list = manager.listFiles()
        assertEquals(1, list.size)
        assertEquals("renamed_track.wav", list[0].name)

        val deleted = manager.deleteFile(renamed)
        assertTrue(deleted)
        assertEquals(0, manager.listFiles().size)
    }
}
