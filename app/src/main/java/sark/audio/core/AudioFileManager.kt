package sark.audio.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 25. Audio File Manager
 * Real file management: list, inspect, save, rename, delete audio files and projects.
 */
data class SavedAudioFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedFormatted: String,
    val durationMs: Long,
    val sampleRate: Int,
    val channels: Int
)

class AudioFileManager(private val baseDir: File) {

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    /**
     * Lists all WAV audio files in the managed directory.
     */
    fun listFiles(): List<SavedAudioFile> {
        val files = baseDir.listFiles { file ->
            file.isFile && file.extension.equals("wav", ignoreCase = true)
        } ?: emptyArray()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        return files.sortedByDescending { it.lastModified() }.map { file ->
            var durationMs = 0L
            var sampleRate = 44100
            var channels = 1
            try {
                val data = WavFileProcessor.decodeWav(file)
                durationMs = data.header.durationMs
                sampleRate = data.header.sampleRate
                channels = data.header.channels
            } catch (_: Exception) {
                // If decoding fails, fallback to estimated size
                val pcmBytes = (file.length() - 44).coerceAtLeast(0)
                durationMs = (pcmBytes / (44100 * 2)) * 1000
            }

            SavedAudioFile(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                durationMs = durationMs,
                sampleRate = sampleRate,
                channels = channels
            )
        }
    }

    /**
     * Saves audio data as a new file in the directory.
     */
    fun saveAudio(audioData: AudioData, preferredName: String): File {
        val sanitized = preferredName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val fileName = if (sanitized.endsWith(".wav", ignoreCase = true)) sanitized else "$sanitized.wav"
        val destFile = File(baseDir, fileName)
        WavFileProcessor.encodeToWavFile(audioData, destFile)
        return destFile
    }

    /**
     * Renames an audio file.
     */
    fun renameFile(targetFile: File, newName: String): File? {
        val sanitized = newName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val finalName = if (sanitized.endsWith(".wav", ignoreCase = true)) sanitized else "$sanitized.wav"
        val newFile = File(baseDir, finalName)
        return if (targetFile.renameTo(newFile)) newFile else null
    }

    /**
     * Deletes an audio file.
     */
    fun deleteFile(targetFile: File): Boolean {
        return targetFile.delete()
    }
}
