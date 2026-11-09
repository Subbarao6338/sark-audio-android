package sark.audio.core

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bulk Video to Audio Extractor
 * Extracts native audio tracks from MP4, MKV, 3GP, WEBM video containers using
 * Android MediaExtractor and hardware/software MediaCodec decoders, converting them to 16-bit PCM WAV.
 */
object AudioVideoExtractor {

    /**
     * Extracts PCM audio from a video file.
     */
    fun extractAudioFromVideo(videoFile: File, outputFile: File): Boolean {
        if (!videoFile.exists()) return false

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(videoFile.absolutePath)
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
                extractor.release()
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val pcmData = java.io.ByteArrayOutputStream()
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
            extractor.release()

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

            WavFileProcessor.encodeToWavFile(AudioData(header, shortSamples), outputFile)
            return true
        } catch (_: Exception) {
            try { extractor.release() } catch (_: Exception) {}
            return false
        }
    }
}
