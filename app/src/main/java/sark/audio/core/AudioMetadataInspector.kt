package sark.audio.core

import java.io.File

/**
 * Detailed audio file metadata representation.
 */
data class AudioMetadata(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val bitRateKbps: Int,
    val format: String
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (durationMs % 1000) / 100
            return "%02d:%02d.%d".format(minutes, seconds, millis)
        }

    val fileSizeFormatted: String
        get() {
            val kb = fileSizeBytes / 1024.0
            if (kb < 1024) return "%.2f KB".format(kb)
            val mb = kb / 1024.0
            return "%.2f MB".format(mb)
        }
}

/**
 * Inspector parsing audio properties and tags.
 */
object AudioMetadataInspector {

    /**
     * Inspect a file directly from disk.
     */
    fun inspectFile(file: File): AudioMetadata {
        val audioData = WavFileProcessor.decodeWav(file)
        return inspectAudioData(audioData, file.name, file.absolutePath, file.length())
    }

    /**
     * Inspect [AudioData] instance.
     */
    fun inspectAudioData(
        audioData: AudioData,
        fileName: String = "audio.wav",
        filePath: String = "",
        fileSizeBytes: Long = (audioData.samples.size * 2 + 44).toLong()
    ): AudioMetadata {
        val header = audioData.header
        val bitRateKbps = (header.byteRate * 8) / 1000

        return AudioMetadata(
            fileName = fileName,
            filePath = filePath,
            fileSizeBytes = fileSizeBytes,
            durationMs = header.durationMs,
            sampleRate = header.sampleRate,
            channels = header.channels,
            bitsPerSample = header.bitsPerSample,
            bitRateKbps = bitRateKbps,
            format = "PCM / RIFF WAV"
        )
    }
}
