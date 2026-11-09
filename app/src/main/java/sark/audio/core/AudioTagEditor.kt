package sark.audio.core

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio Tag & Metadata Editor
 * Reads and writes standard RIFF INFO chunk metadata tags in WAV audio files:
 * INAM (Title), IART (Artist), IPRD (Album), ICRD (Creation Year), IGNR (Genre), ITRK (Track).
 */
data class AudioTagMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val comment: String = ""
)

object AudioTagEditor {

    /**
     * Reads metadata tags from a WAV file.
     */
    fun readTags(wavFile: File): AudioTagMetadata {
        if (!wavFile.exists() || wavFile.length() < 44) return AudioTagMetadata()

        var title = ""
        var artist = ""
        var album = ""
        var year = ""
        var genre = ""
        var trackNumber = ""
        var comment = ""

        try {
            val bytes = FileInputStream(wavFile).use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // Search for "LIST" chunk
            var offset = 12
            while (offset + 8 <= bytes.size) {
                buffer.position(offset)
                val chunkId = String(bytes, offset, 4)
                val chunkSize = buffer.getInt(offset + 4)

                if (chunkId == "LIST" && offset + 12 <= bytes.size) {
                    val listType = String(bytes, offset + 8, 4)
                    if (listType == "INFO") {
                        var subOffset = offset + 12
                        val endSub = offset + 8 + chunkSize
                        while (subOffset + 8 <= endSub && subOffset + 8 <= bytes.size) {
                            val tagId = String(bytes, subOffset, 4)
                            val tagSize = buffer.getInt(subOffset + 4)
                            val tagDataOffset = subOffset + 8
                            if (tagDataOffset + tagSize <= bytes.size) {
                                val tagVal = String(bytes, tagDataOffset, tagSize).trim('\u0000', ' ')
                                when (tagId) {
                                    "INAM" -> title = tagVal
                                    "IART" -> artist = tagVal
                                    "IPRD" -> album = tagVal
                                    "ICRD" -> year = tagVal
                                    "IGNR" -> genre = tagVal
                                    "ITRK" -> trackNumber = tagVal
                                    "ICMT" -> comment = tagVal
                                }
                            }
                            subOffset += 8 + ((tagSize + 1) and -2) // Word align
                        }
                    }
                }
                offset += 8 + ((chunkSize + 1) and -2)
            }
        } catch (_: Exception) {}

        return AudioTagMetadata(
            title = title,
            artist = artist,
            album = album,
            year = year,
            genre = genre,
            trackNumber = trackNumber,
            comment = comment
        )
    }

    /**
     * Embeds RIFF INFO chunk metadata tags into audio WAV bytes.
     */
    fun writeTags(audioData: AudioData, tags: AudioTagMetadata, destination: File) {
        val baseWavBytes = WavFileProcessor.encodeToWavBytes(audioData)
        val infoBytes = buildInfoChunk(tags)

        // If info chunk is non-empty, inject into WAV and update RIFF size
        val finalBytes: ByteArray
        if (infoBytes.isNotEmpty()) {
            val totalSize = baseWavBytes.size + infoBytes.size
            val updatedRiffSize = totalSize - 8

            val buffer = ByteBuffer.wrap(baseWavBytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(4, updatedRiffSize)

            val out = ByteArrayOutputStream(totalSize)
            out.write(baseWavBytes)
            out.write(infoBytes)
            finalBytes = out.toByteArray()
        } else {
            finalBytes = baseWavBytes
        }

        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { it.write(finalBytes) }
    }

    private fun buildInfoChunk(tags: AudioTagMetadata): ByteArray {
        val subChunks = ByteArrayOutputStream()

        fun writeSubChunk(id: String, value: String) {
            if (value.isBlank()) return
            val valBytes = (value + "\u0000").toByteArray(Charsets.US_ASCII)
            val subHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            subHeader.put(id.toByteArray(Charsets.US_ASCII), 0, 4)
            subHeader.putInt(valBytes.size)
            subChunks.write(subHeader.array())
            subChunks.write(valBytes)
            if (valBytes.size % 2 != 0) {
                subChunks.write(0) // Word padding
            }
        }

        writeSubChunk("INAM", tags.title)
        writeSubChunk("IART", tags.artist)
        writeSubChunk("IPRD", tags.album)
        writeSubChunk("ICRD", tags.year)
        writeSubChunk("IGNR", tags.genre)
        writeSubChunk("ITRK", tags.trackNumber)
        writeSubChunk("ICMT", tags.comment)

        val subData = subChunks.toByteArray()
        if (subData.isEmpty()) return ByteArray(0)

        val listChunk = ByteArrayOutputStream()
        val listHeader = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        listHeader.put("LIST".toByteArray(Charsets.US_ASCII))
        listHeader.putInt(4 + subData.size)
        listHeader.put("INFO".toByteArray(Charsets.US_ASCII))
        listChunk.write(listHeader.array())
        listChunk.write(subData)

        return listChunk.toByteArray()
    }
}
