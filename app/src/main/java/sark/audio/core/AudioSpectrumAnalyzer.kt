package sark.audio.core

import kotlin.math.*

/**
 * 22. Audio Spectrum Analyzer (FFT)
 * Computes Fast Fourier Transform (Cooley-Tukey radix-2) to compute decibel frequency spectrum,
 * peak frequency detection, and RMS levels.
 */
object AudioSpectrumAnalyzer {

    data class SpectrumResult(
        val frequencies: DoubleArray, // center frequency of each bin
        val magnitudesDb: FloatArray, // magnitude in dBFS
        val peakFrequencyHz: Double,  // dominant detected frequency
        val rmsDb: Double             // overall RMS level
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SpectrumResult

            if (!frequencies.contentEquals(other.frequencies)) return false
            if (!magnitudesDb.contentEquals(other.magnitudesDb)) return false
            if (peakFrequencyHz != other.peakFrequencyHz) return false
            if (rmsDb != other.rmsDb) return false

            return true
        }

        override fun hashCode(): Int {
            var result = frequencies.contentHashCode()
            result = 31 * result + magnitudesDb.contentHashCode()
            result = 31 * result + peakFrequencyHz.hashCode()
            result = 31 * result + rmsDb.hashCode()
            return result
        }
    }

    /**
     * Analyzes an [AudioData] buffer using an FFT window of size [fftSize] (must be power of 2, e.g. 512, 1024, 2048).
     */
    fun analyze(audioData: AudioData, fftSize: Int = 1024, offsetMs: Long = 0L): SpectrumResult {
        val sampleRate = audioData.header.sampleRate
        val channels = audioData.header.channels
        val totalFrames = audioData.samples.size / channels

        val n = Integer.highestOneBit(fftSize).coerceIn(256, 4096)
        val startFrame = ((offsetMs * sampleRate) / 1000L).toInt().coerceIn(0, (totalFrames - n).coerceAtLeast(0))

        val real = DoubleArray(n)
        val imag = DoubleArray(n)

        var sumSquare = 0.0
        // Window function: Hann window to reduce spectral leakage
        for (i in 0 until n) {
            val frame = startFrame + i
            if (frame < totalFrames) {
                // Mix down to mono for spectrum analysis
                var sampleSum = 0.0
                for (c in 0 until channels) {
                    sampleSum += audioData.samples[frame * channels + c].toDouble() / Short.MAX_VALUE
                }
                val sampleMono = sampleSum / channels
                sumSquare += sampleMono * sampleMono

                val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
                real[i] = sampleMono * hann
            } else {
                real[i] = 0.0
            }
            imag[i] = 0.0
        }

        val rms = sqrt(sumSquare / n)
        val rmsDb = if (rms > 1e-6) 20.0 * log10(rms) else -96.0

        fftRadix2(real, imag)

        // Half spectrum (positive frequencies)
        val numBins = n / 2
        val freqs = DoubleArray(numBins)
        val magsDb = FloatArray(numBins)

        var maxMag = -1e9
        var peakFreq = 0.0

        for (k in 0 until numBins) {
            freqs[k] = (k * sampleRate.toDouble()) / n
            val mag = sqrt(real[k] * real[k] + imag[k] * imag[k]) / (n / 2.0)
            val db = if (mag > 1e-6) (20.0 * log10(mag)).toFloat().coerceIn(-96f, 0f) else -96f
            magsDb[k] = db

            // Ignore 0 Hz DC offset for peak frequency
            if (k > 0 && mag > maxMag) {
                maxMag = mag
                peakFreq = freqs[k]
            }
        }

        return SpectrumResult(
            frequencies = freqs,
            magnitudesDb = magsDb,
            peakFrequencyHz = peakFreq,
            rmsDb = rmsDb
        )
    }

    /**
     * In-place Cooley-Tukey Radix-2 FFT.
     */
    private fun fftRadix2(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey decimation-in-time
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (m in 0 until halfLen) {
                    val uR = real[i + m]
                    val uI = imag[i + m]
                    val vR = real[i + m + halfLen] * wR - imag[i + m + halfLen] * wI
                    val vI = real[i + m + halfLen] * wI + imag[i + m + halfLen] * wR

                    real[i + m] = uR + vR
                    imag[i + m] = uI + vI
                    real[i + m + halfLen] = uR - vR
                    imag[i + m + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
