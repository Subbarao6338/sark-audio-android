package sark.audio.core

import java.io.File

/**
 * Bulk Audio Converter
 * Handles batch processing queues for multi-file format, sample rate, bit depth, and channel conversion.
 */
object AudioBulkConverter {

    data class BatchJob(
        val inputFile: File,
        val targetSampleRate: Int? = null,
        val targetChannels: Int? = null,
        val targetBitDepth: Int? = null, // 8 or 16
        val normalizePeak: Boolean = false
    )

    data class BatchResult(
        val totalJobs: Int,
        val successfulJobs: Int,
        val failedJobs: Int,
        val outputFiles: List<File>
    )

    /**
     * Executes bulk conversion on a list of audio files.
     */
    fun processBulk(jobs: List<BatchJob>, outputDirectory: File): BatchResult {
        outputDirectory.mkdirs()
        val outputs = mutableListOf<File>()
        var successCount = 0
        var failCount = 0

        for (job in jobs) {
            try {
                if (!job.inputFile.exists()) {
                    failCount++
                    continue
                }

                var audio = WavFileProcessor.decodeWav(job.inputFile)

                // Sample rate conversion
                job.targetSampleRate?.let { sr ->
                    if (sr != audio.header.sampleRate) {
                        audio = AudioTimePitchProcessor.resample(audio, sr)
                    }
                }

                // Channel conversion
                job.targetChannels?.let { ch ->
                    if (ch != audio.header.channels) {
                        audio = if (ch == 2) AudioMerger.monoToStereo(audio) else AudioMerger.stereoToMono(audio)
                    }
                }

                // Peak normalization
                if (job.normalizePeak) {
                    audio = AudioGainProcessor.normalize(audio, 0.98f)
                }

                val outName = "conv_" + job.inputFile.nameWithoutExtension + ".wav"
                val outFile = File(outputDirectory, outName)

                if (job.targetBitDepth == 8) {
                    val bytes8 = AudioBitDepthConverter.convertTo8Bit(audio)
                    // Encode as 8-bit WAV
                    val header8 = audio.header.copy(bitsPerSample = 8, dataSize = bytes8.size)
                    val outBytes = java.io.ByteArrayOutputStream()
                    header8.write(outBytes)
                    outBytes.write(bytes8)
                    outFile.writeBytes(outBytes.toByteArray())
                } else {
                    WavFileProcessor.encodeToWavFile(audio, outFile)
                }

                outputs.add(outFile)
                successCount++
            } catch (_: Exception) {
                failCount++
            }
        }

        return BatchResult(
            totalJobs = jobs.size,
            successfulJobs = successCount,
            failedJobs = failCount,
            outputFiles = outputs
        )
    }
}
