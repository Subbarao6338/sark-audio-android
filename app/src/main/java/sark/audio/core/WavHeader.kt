package sark.audio.core

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * RIFF WAV header representation and binary serializer/deserializer.
 * Standard PCM 16-bit or 8-bit, mono or stereo format.
 */
data class WavHeader(
    val channels: Int = 1,
    val sampleRate: Int = 44100,
    val bitsPerSample: Int = 16,
    val dataSize: Int = 0
) {
    val byteRate: Int get() = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign: Int get() = channels * (bitsPerSample / 8)
    val totalFileSize: Int get() = dataSize + 36 // 44 - 8 bytes of RIFF header

    val durationMs: Long
        get() {
            if (byteRate <= 0) return 0L
            return (dataSize.toLong() * 1000L) / byteRate
        }

    fun write(out: OutputStream) {
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // ChunkID "RIFF"
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        // ChunkSize (total size - 8)
        buffer.putInt(36 + dataSize)
        // Format "WAVE"
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // Subchunk1ID "fmt "
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        // Subchunk1Size (16 for PCM)
        buffer.putInt(16)
        // AudioFormat (1 = PCM)
        buffer.putShort(1.toShort())
        // NumChannels
        buffer.putShort(channels.toShort())
        // SampleRate
        buffer.putInt(sampleRate)
        // ByteRate
        buffer.putInt(byteRate)
        // BlockAlign
        buffer.putShort(blockAlign.toShort())
        // BitsPerSample
        buffer.putShort(bitsPerSample.toShort())

        // Subchunk2ID "data"
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        // Subchunk2Size
        buffer.putInt(dataSize)

        out.write(buffer.array())
    }

    companion object {
        fun read(input: InputStream): WavHeader {
            val headerBytes = ByteArray(44)
            var bytesRead = 0
            while (bytesRead < 44) {
                val read = input.read(headerBytes, bytesRead, 44 - bytesRead)
                if (read == -1) throw IllegalArgumentException("Unexpected EOF while reading WAV header")
                bytesRead += read
            }

            val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            val riff = String(headerBytes, 0, 4)
            if (riff != "RIFF") throw IllegalArgumentException("Invalid WAV: Missing 'RIFF' chunk header")

            val wave = String(headerBytes, 8, 4)
            if (wave != "WAVE") throw IllegalArgumentException("Invalid WAV: Missing 'WAVE' chunk format")

            val fmt = String(headerBytes, 12, 4)
            if (fmt != "fmt ") throw IllegalArgumentException("Invalid WAV: Missing 'fmt ' subchunk")

            buffer.position(20)
            val audioFormat = buffer.short
            if (audioFormat.toInt() != 1) {
                throw UnsupportedOperationException("Only PCM format WAV is supported. Found format: $audioFormat")
            }

            val channels = buffer.short.toInt()
            val sampleRate = buffer.int
            val byteRate = buffer.int
            val blockAlign = buffer.short
            val bitsPerSample = buffer.short.toInt()

            val dataTag = String(headerBytes, 36, 4)
            if (dataTag != "data") {
                throw IllegalArgumentException("Expected 'data' tag, found $dataTag")
            }
            buffer.position(40)
            val dataSize = buffer.int

            return WavHeader(
                channels = channels,
                sampleRate = sampleRate,
                bitsPerSample = bitsPerSample,
                dataSize = dataSize
            )
        }
    }
}
