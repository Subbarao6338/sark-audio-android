package sark.audio.core

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/**
 * Controller capturing real microphone PCM audio using Android's AudioRecord API.
 * Streams live amplitude levels for visualizer and serializes straight to WAV file.
 */
class AudioRecorderController(
    val sampleRate: Int = 44100,
    val channels: Int = 1
) {
    sealed class State {
        object Idle : State()
        object Recording : State()
        object Paused : State()
        data class Finished(val file: File, val durationMs: Long) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var outputFile: File? = null

    private val recordedBuffer = ByteArrayOutputStream()
    private var isPaused = false

    @SuppressLint("MissingPermission")
    fun startRecording(destinationFile: File) {
        if (_state.value is State.Recording) return
        outputFile = destinationFile
        recordedBuffer.reset()
        isPaused = false

        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        val bufferSize = max(minBufferSize, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioEncoding,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = State.Error("AudioRecord initialization failed.")
                return
            }

            audioRecord?.startRecording()
            _state.value = State.Recording

            recordingJob = scope.launch {
                val tempBuffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _state.value is State.Recording) {
                    if (isPaused) {
                        Thread.sleep(50)
                        continue
                    }

                    val readCount = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: 0
                    if (readCount > 0) {
                        byteBuffer.clear()
                        var maxPeak = 0
                        for (i in 0 until readCount) {
                            val sample = tempBuffer[i]
                            val sAbs = abs(sample.toInt())
                            if (sAbs > maxPeak) maxPeak = sAbs
                            byteBuffer.putShort(sample)
                        }

                        synchronized(recordedBuffer) {
                            recordedBuffer.write(byteBuffer.array(), 0, readCount * 2)
                        }

                        _amplitude.value = (maxPeak.toFloat() / Short.MAX_VALUE).coerceIn(0f, 1f)
                        val totalBytes = recordedBuffer.size()
                        val bytesPerSec = sampleRate * channels * 2
                        _elapsedTimeMs.value = if (bytesPerSec > 0) (totalBytes.toLong() * 1000L) / bytesPerSec else 0L
                    }
                }
            }
        } catch (e: Exception) {
            _state.value = State.Error("Failed to start recording: ${e.message}")
        }
    }

    fun pauseRecording() {
        if (_state.value is State.Recording) {
            isPaused = true
            _state.value = State.Paused
        }
    }

    fun resumeRecording() {
        if (_state.value is State.Paused) {
            isPaused = false
            _state.value = State.Recording
        }
    }

    fun stopRecording(): File? {
        val dest = outputFile ?: return null
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingJob?.cancel()
            recordingJob = null

            val pcmBytes = synchronized(recordedBuffer) { recordedBuffer.toByteArray() }
            val header = WavHeader(
                channels = channels,
                sampleRate = sampleRate,
                bitsPerSample = 16,
                dataSize = pcmBytes.size
            )

            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { fos ->
                header.write(fos)
                fos.write(pcmBytes)
            }

            val finalDuration = header.durationMs
            _state.value = State.Finished(dest, finalDuration)
            _amplitude.value = 0f
            return dest
        } catch (e: Exception) {
            _state.value = State.Error("Failed to save recording: ${e.message}")
            return null
        }
    }

    fun reset() {
        audioRecord?.release()
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        _state.value = State.Idle
        _amplitude.value = 0f
        _elapsedTimeMs.value = 0L
        recordedBuffer.reset()
    }
}
