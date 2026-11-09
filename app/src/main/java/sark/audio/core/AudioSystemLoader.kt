package sark.audio.core

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Result of loading an audio or video file from Android device storage.
 */
data class LoadedAudioFile(
    val fileName: String,
    val sizeBytes: Long,
    val audioData: AudioData,
    val sourceUri: Uri? = null
)

/**
 * Universal System Audio & Video File Loader.
 * Interacts with Android system ContentResolver to read files chosen by the user
 * from system file pickers, external storage, downloads, etc.
 * Supports native WAV decoding as well as decoding compressed formats (MP3, AAC, M4A, FLAC, OGG)
 * and extracting audio from video containers (MP4, MKV, 3GP, WEBM) via MediaExtractor & MediaCodec.
 */
object AudioSystemLoader {

    /**
     * Resolves the display name of a Uri using ContentResolver.
     */
    fun queryFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.lastPathSegment ?: "audio_${System.currentTimeMillis()}.wav"
        }
        return name
    }

    /**
     * Resolves the size in bytes of a Uri.
     */
    fun queryFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        return 0L
    }

    /**
     * Loads and decodes an audio or video file from a given Android URI.
     */
    fun loadFromUri(context: Context, uri: Uri): LoadedAudioFile {
        val fileName = queryFileName(context, uri)
        val reportedSize = queryFileSize(context, uri)

        // Copy content stream to a temporary cache file for robust random access by MediaExtractor
        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Could not open input stream from URI: $uri")

            val finalSize = if (reportedSize > 0) reportedSize else tempFile.length()
            val audioData = decodeAudioFile(tempFile)

            return LoadedAudioFile(
                fileName = fileName,
                sizeBytes = finalSize,
                audioData = audioData,
                sourceUri = uri
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Decodes any supported audio or video file into 16-bit PCM [AudioData].
     * First attempts fast standard WAV decoding; if that fails or format is compressed (MP3, AAC, FLAC, MP4),
     * utilizes Android's hardware/software MediaCodec pipeline.
     */
    fun decodeAudioFile(file: File): AudioData {
        if (!file.exists()) {
            throw IllegalArgumentException("Audio file does not exist: ${file.absolutePath}")
        }

        // Try standard WAV decoding first
        try {
            return WavFileProcessor.decodeWav(file)
        } catch (_: Exception) {
            // Not a standard PCM WAV, proceed to MediaCodec decoding
        }

        return decodeViaMediaCodec(file)
    }

    /**
     * Decodes audio streams from arbitrary media containers (MP3, AAC, M4A, OGG, FLAC, MP4, MKV)
     * using MediaExtractor and MediaCodec into 16-bit PCM [AudioData].
     */
    private fun decodeViaMediaCodec(file: File): AudioData {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("No supported audio track found in file.")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val pcmData = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(10000L)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                while (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(chunk)
                        pcmData.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            codec.stop()
            codec.release()

            val rawBytes = pcmData.toByteArray()
            val shortCount = rawBytes.size / 2
            val shortSamples = ShortArray(shortCount)
            val bb = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until shortCount) {
                shortSamples[i] = bb.short
            }

            val header = WavHeader(
                channels = channelCount,
                sampleRate = sampleRate,
                bitsPerSample = 16,
                dataSize = shortSamples.size * 2
            )

            return AudioData(header, shortSamples)
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
