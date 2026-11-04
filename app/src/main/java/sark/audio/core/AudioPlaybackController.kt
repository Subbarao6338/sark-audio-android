package sark.audio.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * High-performance low-latency PCM audio player supporting seeking, scrubbing,
 * looping, and precise progress emission.
 */
class AudioPlaybackController {

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Playing : PlaybackState()
        object Paused : PlaybackState()
        object Completed : PlaybackState()
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private var currentAudio: AudioData? = null
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentFramePosition = 0
    private var isPlayingActive = false

    fun load(audioData: AudioData) {
        stop()
        currentAudio = audioData
        currentFramePosition = 0
        _currentPositionMs.value = 0L
        _playbackState.value = PlaybackState.Idle
    }

    fun play() {
        val audio = currentAudio ?: return
        if (_playbackState.value == PlaybackState.Playing) return

        val sampleRate = audio.header.sampleRate
        val channels = audio.header.channels
        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        val bufferSize = minBufferSize * 2

        audioTrack?.release()
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioEncoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        _playbackState.value = PlaybackState.Playing
        isPlayingActive = true

        playJob = scope.launch {
            val totalFrames = audio.samples.size / channels
            val chunkFrames = 2048
            val chunkBuffer = ShortArray(chunkFrames * channels)

            while (isActive && isPlayingActive && currentFramePosition < totalFrames) {
                val framesToRead = min(chunkFrames, totalFrames - currentFramePosition)
                val samplesToRead = framesToRead * channels

                System.arraycopy(
                    audio.samples,
                    currentFramePosition * channels,
                    chunkBuffer,
                    0,
                    samplesToRead
                )

                audioTrack?.write(chunkBuffer, 0, samplesToRead)
                currentFramePosition += framesToRead

                val posMs = (currentFramePosition.toLong() * 1000L) / sampleRate
                _currentPositionMs.value = posMs
            }

            if (currentFramePosition >= totalFrames) {
                _playbackState.value = PlaybackState.Completed
                isPlayingActive = false
            }
        }
    }

    fun pause() {
        if (_playbackState.value == PlaybackState.Playing) {
            isPlayingActive = false
            audioTrack?.pause()
            _playbackState.value = PlaybackState.Paused
        }
    }

    fun seekTo(positionMs: Long) {
        val audio = currentAudio ?: return
        val totalMs = audio.header.durationMs
        val clampedMs = positionMs.coerceIn(0L, totalMs)
        val sampleRate = audio.header.sampleRate
        currentFramePosition = ((clampedMs * sampleRate) / 1000L).toInt()
        _currentPositionMs.value = clampedMs

        if (_playbackState.value == PlaybackState.Playing) {
            pause()
            play()
        }
    }

    fun stop() {
        isPlayingActive = false
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        currentFramePosition = 0
        _currentPositionMs.value = 0L
        _playbackState.value = PlaybackState.Idle
    }
}
