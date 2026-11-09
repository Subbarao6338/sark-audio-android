package sark.audio.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.abs

class FullSuiteAudioDspTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAudioVocalProcessor() {
        val stereoTone = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.25, channels = 2, amplitude = 0.8f
        )
        val vocalRemoved = AudioVocalProcessor.removeVocals(stereoTone, vocalReductionStrength = 1.0f, bassPreservationHz = 0.0)
        assertEquals(stereoTone.samples.size, vocalRemoved.samples.size)

        // Center channel cancellation on identical stereo tone results in near-zero output
        var maxAmp = 0
        for (s in vocalRemoved.samples) {
            val a = abs(s.toInt())
            if (a > maxAmp) maxAmp = a
        }
        assertTrue("Expected vocal cancellation to drop peak level significantly, got $maxAmp", maxAmp < 50)

        val isolated = AudioVocalProcessor.isolateVocals(stereoTone)
        assertEquals(stereoTone.samples.size, isolated.samples.size)
        assertTrue(isolated.samples.any { it != 0.toShort() })
    }

    @Test
    fun testAudioAutotuneAndPitchDetection() {
        val tone440 = AudioToneGenerator.generateTone(
            AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2, sampleRate = 44100
        )
        val midiNote = AudioAutotuneProcessor.frequencyToMidi(440.0)
        assertEquals(69.0, midiNote, 0.01)

        val snapped = AudioAutotuneProcessor.snapToScale(69.3, AudioAutotuneProcessor.MusicalScale.MAJOR)
        assertEquals(69.0, snapped, 0.01)

        val tuned = AudioAutotuneProcessor.applyAutotune(tone440, AudioAutotuneProcessor.MusicalScale.CHROMATIC)
        assertEquals(tone440.samples.size, tuned.samples.size)
    }

    @Test
    fun testAudioVoiceChangerPresets() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 300.0, durationSeconds = 0.1)

        for (preset in AudioVoiceChanger.VoicePreset.values()) {
            val changed = AudioVoiceChanger.applyPreset(tone, preset)
            assertTrue("Preset $preset failed to produce samples", changed.samples.isNotEmpty())
        }
    }

    @Test
    fun testAudio8DProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.5)
        val eightD = Audio8DProcessor.process8D(tone, rotationSpeedHz = 0.5, reverbDepth = 0.2f)

        assertEquals(2, eightD.header.channels)
        assertTrue(eightD.samples.isNotEmpty())
    }

    @Test
    fun testVoiceCloneSimulator() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.25)
        val profile = AudioVoiceCloneSimulator.extractFormants(tone)
        assertTrue(profile.f1Hz > 0)

        val modeled = AudioVoiceCloneSimulator.applyVocalCloneProfile(tone, profile)
        assertEquals(tone.samples.size, modeled.samples.size)
    }

    @Test
    fun testMasteringEngine() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.25, amplitude = 0.4f)
        val mastered = AudioMasteringEngine.masterAudio(tone, AudioMasteringEngine.MasteringConfig(targetCeilingDb = -0.5))

        assertEquals(2, mastered.header.channels)
        assertTrue(mastered.samples.isNotEmpty())
    }

    @Test
    fun testEqualizerProcessor() {
        val noise = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.WHITE_NOISE, durationSeconds = 0.2)
        val eqApplied = AudioEqualizerProcessor.apply3Band(noise, bassGainDb = 4.0, midGainDb = -2.0, trebleGainDb = 3.0)

        assertEquals(noise.samples.size, eqApplied.samples.size)
    }

    @Test
    fun testStemsSplitter() {
        val mix = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SAWTOOTH, 220.0, durationSeconds = 0.25, channels = 2)
        val stems = AudioStemsSplitter.splitStems(mix)

        assertEquals(mix.samples.size, stems.vocalsStem.samples.size)
        assertEquals(mix.samples.size, stems.bassStem.samples.size)
        assertEquals(mix.samples.size, stems.drumsStem.samples.size)
        assertEquals(mix.samples.size, stems.melodyStem.samples.size)
    }

    @Test
    fun testAudioTrackSplitter() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 1.0)
        val chunks = AudioTrackSplitter.splitByDuration(tone, segmentDurationMs = 250L)
        assertEquals(4, chunks.size)

        val markerChunks = AudioTrackSplitter.splitAtMarkers(tone, listOf(300L, 700L))
        assertEquals(3, markerChunks.size)
    }

    @Test
    fun testAudioTagEditor() {
        val folder = tempFolder.newFolder("tags_test")
        val file = File(folder, "test_tagged.wav")

        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.1)
        val tags = AudioTagMetadata(
            title = "Awesome Beat",
            artist = "Sark Producer",
            album = "Masterpiece",
            year = "2026",
            genre = "Electronic",
            trackNumber = "1"
        )

        AudioTagEditor.writeTags(tone, tags, file)
        assertTrue(file.exists())

        val readBack = AudioTagEditor.readTags(file)
        assertEquals("Awesome Beat", readBack.title)
        assertEquals("Sark Producer", readBack.artist)
        assertEquals("Masterpiece", readBack.album)
        assertEquals("2026", readBack.year)
        assertEquals("Electronic", readBack.genre)
        assertEquals("1", readBack.trackNumber)
    }

    @Test
    fun testDenoiseAndReverbRemover() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.25)
        val dereverbed = AudioDenoiseProcessor.removeReverbAndEcho(tone, reductionStrength = 0.5f)
        assertEquals(tone.samples.size, dereverbed.samples.size)
    }

    @Test
    fun testBulkConverter() {
        val inFolder = tempFolder.newFolder("bulk_in")
        val outFolder = tempFolder.newFolder("bulk_out")

        val f1 = File(inFolder, "t1.wav")
        val f2 = File(inFolder, "t2.wav")
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.1)
        WavFileProcessor.encodeToWavFile(tone, f1)
        WavFileProcessor.encodeToWavFile(tone, f2)

        val jobs = listOf(
            AudioBulkConverter.BatchJob(f1, targetSampleRate = 22050, targetChannels = 2),
            AudioBulkConverter.BatchJob(f2, targetBitDepth = 8, normalizePeak = true)
        )

        val res = AudioBulkConverter.processBulk(jobs, outFolder)
        assertEquals(2, res.successfulJobs)
        assertEquals(0, res.failedJobs)
        assertEquals(2, res.outputFiles.size)
    }

    @Test
    fun testSfxManager() {
        val beep = AudioSfxManager.generateSfx(AudioSfxManager.SfxType.CENSOR_BEEP)
        assertTrue(beep.samples.isNotEmpty())

        val baseTone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 1.0)
        val overlaid = AudioSfxManager.overlaySfx(baseTone, beep, timestampMs = 200L)
        assertTrue(overlaid.samples.isNotEmpty())
    }

    @Test
    fun testChorusProcessor() {
        val tone = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.2)
        val chorus = AudioChorusProcessor.applyChorus(tone, rateHz = 1.5, depthMs = 3.0)
        assertEquals(2, chorus.header.channels)
        assertEquals(tone.samples.size * 2, chorus.samples.size)
    }

    @Test
    fun testAudioTuner() {
        val toneA4 = AudioToneGenerator.generateTone(AudioToneGenerator.WaveformType.SINE, 440.0, durationSeconds = 0.25, sampleRate = 44100)
        val res = AudioTunerEngine.analyzePitch(toneA4)
        assertEquals("A", res.noteName)
        assertEquals(4, res.octave)
        assertTrue(res.isInTune)
    }
}
