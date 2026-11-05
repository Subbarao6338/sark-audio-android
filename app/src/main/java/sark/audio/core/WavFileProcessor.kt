package sark.audio.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Representation of decoded PCM audio data with audio metadata.
 */
data class AudioData(
    val header: WavHeader,
    val samples: ShortArray // 16-bit signed PCM samples interleaved (e.g. L, R, L, R)
) {
    val durationSeconds: Double get() = samples.size.toDouble() / (header.sampleRate * header.channels)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData
        if (header != other.header) return false
        if (!samples.contentEquals(other.samples)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + samples.contentHashCode()
        return result
    }
}

/**
 * WAV file encoder, decoder, and sample converter.
 */
object WavFileProcessor {

    /**
     * Decode a WAV file from disk into [AudioData].
     */
    fun decodeWav(file: File): AudioData {
        FileInputStream(file).use { fis ->
            return decodeWav(fis.readBytes())
        }
    }

    /**
     * Decode WAV byte array into [AudioData].
     */
    fun decodeWav(bytes: ByteArray): AudioData {
        val bais = ByteArrayInputStream(bytes)
        val header = WavHeader.read(bais)
        val pcmBytes = ByteArray(header.dataSize)
        var totalRead = 0
        while (totalRead < header.dataSize) {
            val r = bais.read(pcmBytes, totalRead, header.dataSize - totalRead)
            if (r == -1) break
            totalRead += r
        }

        val shortSamples: ShortArray
        if (header.bitsPerSample == 16) {
            val sampleCount = totalRead / 2
            shortSamples = ShortArray(sampleCount)
            val buffer = ByteBuffer.wrap(pcmBytes, 0, totalRead).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until sampleCount) {
                shortSamples[i] = buffer.short
            }
        } else if (header.bitsPerSample == 8) {
            // 8-bit unsigned PCM to 16-bit signed PCM
            shortSamples = ShortArray(totalRead)
            for (i in 0 until totalRead) {
                val unsignedVal = pcmBytes[i].toInt() and 0xFF
                shortSamples[i] = ((unsignedVal - 128) * 256).toShort()
            }
        } else {
            throw UnsupportedOperationException("Bits per sample ${header.bitsPerSample} not supported. Use 8 or 16.")
        }

        return AudioData(
            header = header.copy(dataSize = shortSamples.size * 2, bitsPerSample = 16),
            samples = shortSamples
        )
    }

    /**
     * Encode [AudioData] into a standard 16-bit PCM RIFF WAV byte array.
     */
    fun encodeToWavBytes(audioData: AudioData): ByteArray {
        val pcmByteSize = audioData.samples.size * 2
        val header = audioData.header.copy(
            dataSize = pcmByteSize,
            bitsPerSample = 16
        )

        val baos = ByteArrayOutputStream(44 + pcmByteSize)
        header.write(baos)

        val sampleBuffer = ByteBuffer.allocate(pcmByteSize).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in audioData.samples) {
            sampleBuffer.putShort(sample)
        }
        baos.write(sampleBuffer.array())
        return baos.toByteArray()
    }

    /**
     * Save [AudioData] directly to a file on disk.
     */
    fun encodeToWavFile(audioData: AudioData, destination: File) {
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { fos ->
            fos.write(encodeToWavBytes(audioData))
        }
    }

    /**
     * Create synthetic test audio tone for testing and verification.
     */
    fun generateSineWave(
        sampleRate: Int = 44100,
        frequencyHz: Double = 440.0,
        durationSeconds: Double = 1.0,
        channels: Int = 1,
        amplitude: Float = 0.8f
    ): AudioData {
        val totalFrames = (sampleRate * durationSeconds).toInt()
        val totalSamples = totalFrames * channels
        val samples = ShortArray(totalSamples)
        val maxAmp = (Short.MAX_VALUE * amplitude).coerceIn(0f, Short.MAX_VALUE.toFloat())

        for (i in 0 until totalFrames) {
            val angle = 2.0 * Math.PI * i * frequencyHz / sampleRate
            val sampleVal = (Math.sin(angle) * maxAmp).toInt().toShort()
            for (c in 0 until channels) {
                samples[i * channels + c] = sampleVal
            }
        }

        val header = WavHeader(
            channels = channels,
            sampleRate = sampleRate,
            bitsPerSample = 16,
            dataSize = totalSamples * 2
        )
        return AudioData(header, samples)
    }

    /**
     * Extract normalized waveform peaks (e.g., 50-200 bars) for visual representation.
     */
    fun extractWaveformAmplitudes(samples: ShortArray, channels: Int, binCount: Int): FloatArray {
        if (samples.isEmpty() || binCount <= 0) return FloatArray(0)
        val frames = samples.size / channels
        val result = FloatArray(binCount)
        val framesPerBin = (frames.toFloat() / binCount).coerceAtLeast(1f)

        for (b in 0 until binCount) {
            val startFrame = (b * framesPerBin).toInt().coerceIn(0, frames - 1)
            val endFrame = ((b + 1) * framesPerBin).toInt().coerceIn(startFrame + 1, frames)

            var peak = 0
            for (f in startFrame until endFrame) {
                for (c in 0 until channels) {
                    val s = Math.abs(samples[f * channels + c].toInt())
                    if (s > peak) peak = s
                }
            }
            result[b] = (peak.toFloat() / Short.MAX_VALUE).coerceIn(0f, 1f)
        }
        return result
    }
}
