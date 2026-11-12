package com.hardtekk.electribe.audio.DSP

/**
 * High-performance Biquad Filter implementation
 * Supports Low-Pass, High-Pass, Band-Pass, and Notch filters
 * Optimized for real-time audio processing with minimal allocations
 */
class BiquadFilter {
    // Filter coefficients
    private var a0: Double = 1.0
    private var a1: Double = 0.0
    private var a2: Double = 0.0
    private var b1: Double = 0.0
    private var b2: Double = 0.0

    // State variables (per channel for stereo)
    private var z1L: Double = 0.0
    private var z2L: Double = 0.0
    private var z1R: Double = 0.0
    private var z2R: Double = 0.0

    // Current settings
    var filterType: FilterType = FilterType.LOW_PASS
    var frequency: Double = 1000.0 // Hz
    var resonance: Double = 0.707 // Q factor
    var sampleRate: Double = 44100.0

    // Smoothing for parameter changes
    private var targetA0: Double = 1.0
    private var targetA1: Double = 0.0
    private var targetA2: Double = 0.0
    private var targetB1: Double = 0.0
    private var targetB2: Double = 0.0
    private val smoothingFactor: Double = 0.001 // Coefficient interpolation speed

    init {
        updateCoefficients()
    }

    enum class FilterType {
        LOW_PASS, HIGH_PASS, BAND_PASS, NOTCH, PEAK, LOW_SHELF, HIGH_SHELF
    }

    fun setSampleRate(sr: Double) {
        sampleRate = sr
        updateCoefficients()
    }

    fun setFrequency(freq: Double) {
        frequency = freq.coerceIn(20.0, sampleRate * 0.45)
        updateCoefficients()
    }

    fun setResonance(q: Double) {
        resonance = q.coerceIn(0.1, 20.0)
        updateCoefficients()
    }

    fun setType(type: FilterType) {
        filterType = type
        updateCoefficients()
    }

    fun setParameters(type: FilterType, freq: Double, q: Double) {
        filterType = type
        frequency = freq.coerceIn(20.0, sampleRate * 0.45)
        resonance = q.coerceIn(0.1, 20.0)
        updateCoefficients()
    }

    private fun updateCoefficients() {
        val w0 = 2.0 * Math.PI * frequency / sampleRate
        val cosW0 = Math.cos(w0)
        val sinW0 = Math.sin(w0)
        val alpha = sinW0 / (2.0 * resonance)

        when (filterType) {
            FilterType.LOW_PASS -> {
                val b0 = (1.0 - cosW0) * 0.5
                val b1_ = 1.0 - cosW0
                val b2_ = (1.0 - cosW0) * 0.5
                val a0_ = 1.0 + alpha
                val a1_ = -2.0 * cosW0
                val a2_ = 1.0 - alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.HIGH_PASS -> {
                val b0 = (1.0 + cosW0) * 0.5
                val b1_ = -(1.0 + cosW0)
                val b2_ = (1.0 + cosW0) * 0.5
                val a0_ = 1.0 + alpha
                val a1_ = -2.0 * cosW0
                val a2_ = 1.0 - alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.BAND_PASS -> {
                val b0 = alpha
                val b1_ = 0.0
                val b2_ = -alpha
                val a0_ = 1.0 + alpha
                val a1_ = -2.0 * cosW0
                val a2_ = 1.0 - alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.NOTCH -> {
                val b0 = 1.0
                val b1_ = -2.0 * cosW0
                val b2_ = 1.0
                val a0_ = 1.0 + alpha
                val a1_ = -2.0 * cosW0
                val a2_ = 1.0 - alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.PEAK -> {
                val A = Math.pow(10.0, resonance / 40.0) // resonance as dB gain
                val b0 = 1.0 + alpha * A
                val b1_ = -2.0 * cosW0
                val b2_ = 1.0 - alpha * A
                val a0_ = 1.0 + alpha / A
                val a1_ = -2.0 * cosW0
                val a2_ = 1.0 - alpha / A

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.LOW_SHELF -> {
                val A = Math.pow(10.0, resonance / 40.0)
                val sqrtA = Math.sqrt(A)
                val b0 = A * ((A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                val b1_ = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW0)
                val b2_ = A * ((A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                val a0_ = (A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                val a1_ = -2.0 * ((A - 1.0) + (A + 1.0) * cosW0)
                val a2_ = (A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
            FilterType.HIGH_SHELF -> {
                val A = Math.pow(10.0, resonance / 40.0)
                val sqrtA = Math.sqrt(A)
                val b0 = A * ((A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                val b1_ = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosW0)
                val b2_ = A * ((A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                val a0_ = (A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                val a1_ = 2.0 * ((A - 1.0) - (A + 1.0) * cosW0)
                val a2_ = (A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha

                targetA0 = b0 / a0_
                targetA1 = b1_ / a0_
                targetA2 = b2_ / a0_
                targetB1 = a1_ / a0_
                targetB2 = a2_ / a0_
            }
        }
    }

    /**
     * Process a single sample (mono)
     * Uses coefficient smoothing to avoid zipper noise
     */
    @Suppress("UNUSED_PARAMETER")
    fun process(input: Double): Double {
        // Smooth coefficients
        a0 += (targetA0 - a0) * smoothingFactor
        a1 += (targetA1 - a1) * smoothingFactor
        a2 += (targetA2 - a2) * smoothingFactor
        b1 += (targetB1 - b1) * smoothingFactor
        b2 += (targetB2 - b2) * smoothingFactor

        val output = a0 * input + z1L
        z1L = a1 * input - b1 * output + z2L
        z2L = a2 * input - b2 * output

        return output
    }

    /**
     * Process stereo sample pair
     */
    fun processStereo(inputL: Double, inputR: Double): Pair<Double, Double> {
        // Smooth coefficients
        a0 += (targetA0 - a0) * smoothingFactor
        a1 += (targetA1 - a1) * smoothingFactor
        a2 += (targetA2 - a2) * smoothingFactor
        b1 += (targetB1 - b1) * smoothingFactor
        b2 += (targetB2 - b2) * smoothingFactor

        // Left channel
        val outputL = a0 * inputL + z1L
        z1L = a1 * inputL - b1 * outputL + z2L
        z2L = a2 * inputL - b2 * outputL

        // Right channel
        val outputR = a0 * inputR + z1R
        z1R = a1 * inputR - b1 * outputR + z2R
        z2R = a2 * inputR - b2 * outputR

        return outputL to outputR
    }

    /**
     * Process a buffer of samples (mono) - optimized for batch processing
     */
    fun processBuffer(input: FloatArray, output: FloatArray, offset: Int = 0, length: Int = -1) {
        val len = if (length == -1) input.size - offset else length.coerceAtMost(input.size - offset)
        var z1 = z1L
        var z2 = z2L
        var currA0 = a0
        var currA1 = a1
        var currA2 = a2
        var currB1 = b1
        var currB2 = b2

        for (i in 0 until len) {
            // Smooth coefficients
            currA0 += (targetA0 - currA0) * smoothingFactor
            currA1 += (targetA1 - currA1) * smoothingFactor
            currA2 += (targetA2 - currA2) * smoothingFactor
            currB1 += (targetB1 - currB1) * smoothingFactor
            currB2 += (targetB2 - currB2) * smoothingFactor

            val inputSample = input[offset + i].toDouble()
            val outputSample = currA0 * inputSample + z1
            z1 = currA1 * inputSample - currB1 * outputSample + z2
            z2 = currA2 * inputSample - currB2 * outputSample

            output[offset + i] = outputSample.toFloat()
        }

        // Update state
        a0 = currA0
        a1 = currA1
        a2 = currA2
        b1 = currB1
        b2 = currB2
        z1L = z1
        z2L = z2
    }

    /**
     * Process stereo buffer
     */
    fun processBufferStereo(inputL: FloatArray, inputR: FloatArray, outputL: FloatArray, outputR: FloatArray, offset: Int = 0, length: Int = -1) {
        val len = if (length == -1) inputL.size - offset else length.coerceAtMost(inputL.size - offset)
        var z1L_ = z1L
        var z2L_ = z2L
        var z1R_ = z1R
        var z2R_ = z2R
        var currA0 = a0
        var currA1 = a1
        var currA2 = a2
        var currB1 = b1
        var currB2 = b2

        for (i in 0 until len) {
            // Smooth coefficients
            currA0 += (targetA0 - currA0) * smoothingFactor
            currA1 += (targetA1 - currA1) * smoothingFactor
            currA2 += (targetA2 - currA2) * smoothingFactor
            currB1 += (targetB1 - currB1) * smoothingFactor
            currB2 += (targetB2 - currB2) * smoothingFactor

            // Left
            val inL = inputL[offset + i].toDouble()
            val outL = currA0 * inL + z1L_
            z1L_ = currA1 * inL - currB1 * outL + z2L_
            z2L_ = currA2 * inL - currB2 * outL
            outputL[offset + i] = outL.toFloat()

            // Right
            val inR = inputR[offset + i].toDouble()
            val outR = currA0 * inR + z1R_
            z1R_ = currA1 * inR - currB1 * outR + z2R_
            z2R_ = currA2 * inR - currB2 * outR
            outputR[offset + i] = outR.toFloat()
        }

        a0 = currA0
        a1 = currA1
        a2 = currA2
        b1 = currB1
        b2 = currB2
        z1L = z1L_
        z2L = z2L_
        z1R = z1R_
        z2R = z2R_
    }

    fun reset() {
        z1L = 0.0
        z2L = 0.0
        z1R = 0.0
        z2R = 0.0
    }

    fun getFrequencyResponse(frequencies: DoubleArray): DoubleArray {
        val response = DoubleArray(frequencies.size)
        for (i in frequencies.indices) {
            val w = 2.0 * Math.PI * frequencies[i] / sampleRate
            val cosW = Math.cos(w)
            val sinW = Math.sin(w)

            // Evaluate transfer function at this frequency
            val numReal = a0 + a1 * cosW + a2 * Math.cos(2.0 * w)
            val numImag = a1 * sinW + a2 * Math.sin(2.0 * w)
            val denReal = 1.0 + b1 * cosW + b2 * Math.cos(2.0 * w)
            val denImag = b1 * sinW + b2 * Math.sin(2.0 * w)

            val magSq = (numReal * numReal + numImag * numImag) / (denReal * denReal + denImag * denImag)
            response[i] = Math.sqrt(magSq)
        }
        return response
    }
}