package com.hardtekk.electribe.audio.DSP

/**
 * Stereo Delay with feedback, filtering, and modulation
 * Supports ping-pong, stereo spread, and tempo-sync modes
 */
class Delay {
    enum class DelayMode {
        MONO,           // Single delay line
        STEREO,         // Independent L/R delays
        PING_PONG,      // Alternating L/R
        STEREO_SPREAD   // Wide stereo with cross-feedback
    }

    var mode: DelayMode = DelayMode.STEREO
    var delayTimeMs: Float = 250.0f      // 1ms - 2000ms
    var feedback: Float = 0.3f           // 0.0 - 0.95
    var mix: Float = 0.3f                // 0.0 - 1.0
    var lowCut: Float = 200.0f           // Hz
    var highCut: Float = 8000.0f         // Hz
    var stereoWidth: Float = 0.5f        // 0.0 - 1.0
    var pingPongOffset: Float = 0.5f     // 0.0 - 1.0 (for ping-pong)
    var tempoSync: Boolean = false
    var tempoSyncDivision: TempoDivision = TempoDivision.EIGHTH

    private var sampleRate: Double = 44100.0
    private var maxDelaySamples: Int = 88200 // 2 seconds at 44.1kHz

    // Delay buffers (circular)
    private var bufferL: FloatArray = FloatArray(0)
    private var bufferR: FloatArray = FloatArray(0)
    private var writeIndex: Int = 0
    private var bufferSize: Int = 0

    // Filters for feedback path
    private val lowCutFilterL = BiquadFilter()
    private val lowCutFilterR = BiquadFilter()
    private val highCutFilterL = BiquadFilter()
    private val highCutFilterR = BiquadFilter()

    // Modulation for chorus-like effect
    private var modPhase: Float = 0.0f
    private var modDepth: Float = 0.0f
    private var modRate: Float = 0.5f // Hz

    init {
        initializeBuffers()
    }

    fun setSampleRate(sr: Double) {
        sampleRate = sr
        maxDelaySamples = (sr * 2.0).toInt() // 2 seconds max
        initializeBuffers()

        lowCutFilterL.setSampleRate(sr)
        lowCutFilterR.setSampleRate(sr)
        highCutFilterL.setSampleRate(sr)
        highCutFilterR.setSampleRate(sr)

        updateFilters()
    }

    fun setDelayTimeMs(timeMs: Float) {
        delayTimeMs = timeMs.coerceIn(1.0f, 2000.0f)
    }

    fun setFeedback(fb: Float) {
        feedback = fb.coerceIn(0.0f, 0.98f)
    }

    fun setMix(m: Float) {
        mix = m.coerceIn(0.0f, 1.0f)
    }

    fun setLowCut(freq: Float) {
        lowCut = freq.coerceIn(20.0f, 1000.0f)
        updateFilters()
    }

    fun setHighCut(freq: Float) {
        highCut = freq.coerceIn(1000.0f, 20000.0f)
        updateFilters()
    }

    fun setStereoWidth(width: Float) {
        stereoWidth = width.coerceIn(0.0f, 1.0f)
    }

    fun setModulation(depth: Float, rateHz: Float) {
        modDepth = depth.coerceIn(0.0f, 1.0f)
        modRate = rateHz.coerceIn(0.01f, 10.0f)
    }

    fun setTempoSync(enabled: Boolean, bpm: Int, division: TempoDivision) {
        tempoSync = enabled
        tempoSyncDivision = division
        if (enabled) {
            updateTempoSyncDelay(bpm)
        }
    }

    private fun updateTempoSyncDelay(bpm: Int) {
        val beatMs = 60000.0 / bpm
        val divisionMs = when (tempoSyncDivision) {
            TempoDivision.WHOLE -> beatMs * 4.0
            TempoDivision.HALF -> beatMs * 2.0
            TempoDivision.QUARTER -> beatMs
            TempoDivision.EIGHTH -> beatMs / 2.0
            TempoDivision.SIXTEENTH -> beatMs / 4.0
            TempoDivision.THIRTY_SECOND -> beatMs / 8.0
            TempoDivision.DOTTED_EIGHTH -> beatMs * 0.75
            TempoDivision.DOTTED_QUARTER -> beatMs * 1.5
            TempoDivision.TRIPLET_QUARTER -> beatMs * 2.0 / 3.0
            TempoDivision.TRIPLET_EIGHTH -> beatMs / 3.0
            TempoDivision.TRIPLET_SIXTEENTH -> beatMs / 6.0
        }
        delayTimeMs = divisionMs.toFloat().coerceIn(1.0f, 2000.0f)
    }

    private fun initializeBuffers() {
        bufferSize = maxDelaySamples
        bufferL = FloatArray(bufferSize)
        bufferR = FloatArray(bufferSize)
        writeIndex = 0
    }

    private fun updateFilters() {
        lowCutFilterL.setType(BiquadFilter.FilterType.HIGH_PASS)
        lowCutFilterL.setFrequency(lowCut.toDouble())
        lowCutFilterL.setResonance(0.707)
        lowCutFilterR.setType(BiquadFilter.FilterType.HIGH_PASS)
        lowCutFilterR.setFrequency(lowCut.toDouble())
        lowCutFilterR.setResonance(0.707)

        highCutFilterL.setType(BiquadFilter.FilterType.LOW_PASS)
        highCutFilterL.setFrequency(highCut.toDouble())
        highCutFilterL.setResonance(0.707)
        highCutFilterR.setType(BiquadFilter.FilterType.LOW_PASS)
        highCutFilterR.setFrequency(highCut.toDouble())
        highCutFilterR.setResonance(0.707)
    }

    /**
     * Process stereo input, return stereo output
     */
    fun process(inputL: Float, inputR: Float): Pair<Float, Float> {
        // Calculate delay time in samples with modulation
        val baseDelaySamples = (delayTimeMs * sampleRate / 1000.0).toInt()
        modPhase += (modRate * 2.0 * Math.PI / sampleRate).toFloat()
        val modOffset = (Math.sin(modPhase.toDouble()) * modDepth * baseDelaySamples * 0.1).toInt()

        val delaySamplesL = (baseDelaySamples + modOffset).coerceIn(1, bufferSize - 1)
        val delaySamplesR = when (mode) {
            DelayMode.MONO -> delaySamplesL
            DelayMode.STEREO -> {
                val spread = (stereoWidth * baseDelaySamples * 0.5).toInt()
                (baseDelaySamples + spread + modOffset).coerceIn(1, bufferSize - 1)
            }
            DelayMode.PING_PONG -> {
                val offset = (pingPongOffset * baseDelaySamples).toInt()
                (baseDelaySamples + offset + modOffset).coerceIn(1, bufferSize - 1)
            }
            DelayMode.STEREO_SPREAD -> {
                val spread = (stereoWidth * baseDelaySamples * 0.3).toInt()
                (baseDelaySamples + spread + modOffset).coerceIn(1, bufferSize - 1)
            }
        }

        // Read from delay buffers (circular)
        val readIndexL = (writeIndex - delaySamplesL + bufferSize) % bufferSize
        val readIndexR = (writeIndex - delaySamplesR + bufferSize) % bufferSize

        var delayedL = bufferL[readIndexL]
        var delayedR = bufferR[readIndexR]

        // Apply feedback filters
        delayedL = lowCutFilterL.process(delayedL.toDouble()).toFloat()
        delayedL = highCutFilterL.process(delayedL.toDouble()).toFloat()
        delayedR = lowCutFilterR.process(delayedR.toDouble()).toFloat()
        delayedR = highCutFilterR.process(delayedR.toDouble()).toFloat()

        // Cross-feedback for stereo spread and ping-pong
        val feedbackL = when (mode) {
            DelayMode.PING_PONG -> delayedR * feedback
            DelayMode.STEREO_SPREAD -> (delayedL + delayedR * stereoWidth * 0.5) * feedback
            else -> delayedL * feedback
        }
        val feedbackR = when (mode) {
            DelayMode.PING_PONG -> delayedL * feedback
            DelayMode.STEREO_SPREAD -> (delayedR + delayedL * stereoWidth * 0.5) * feedback
            else -> delayedR * feedback
        }

        // Write to buffers (input + feedback)
        bufferL[writeIndex] = inputL + feedbackL
        bufferR[writeIndex] = inputR + feedbackR

        // Advance write index
        writeIndex = (writeIndex + 1) % bufferSize

        // Mix dry/wet
        val outL = inputL + (delayedL - inputL) * mix
        val outR = inputR + (delayedR - inputR) * mix

        return outL to outR
    }

    fun processBuffer(inputL: FloatArray, inputR: FloatArray, outputL: FloatArray, outputR: FloatArray, offset: Int = 0, length: Int = -1) {
        val len = if (length == -1) inputL.size - offset else length.coerceAtMost(inputL.size - offset)

        for (i in 0 until len) {
            val (outL, outR) = process(inputL[offset + i], inputR[offset + i])
            outputL[offset + i] = outL
            outputR[offset + i] = outR
        }
    }

    fun processMono(input: Float): Float {
        val (outL, _) = process(input, input)
        return outL
    }

    fun clear() {
        bufferL.fill(0.0f)
        bufferR.fill(0.0f)
        lowCutFilterL.reset()
        lowCutFilterR.reset()
        highCutFilterL.reset()
        highCutFilterR.reset()
    }

    fun getDelayTimeSamples(): Int {
        return (delayTimeMs * sampleRate / 1000.0).toInt()
    }

    fun getMaxDelayTimeMs(): Float {
        return (bufferSize.toDouble() / sampleRate * 1000.0).toFloat()
    }
}

enum class TempoDivision {
    WHOLE,              // 4 beats
    HALF,               // 2 beats
    QUARTER,            // 1 beat
    EIGHTH,             // 1/2 beat
    SIXTEENTH,          // 1/4 beat
    THIRTY_SECOND,      // 1/8 beat
    DOTTED_EIGHTH,      // 3/4 beat
    DOTTED_QUARTER,     // 1.5 beats
    TRIPLET_QUARTER,    // 2/3 beat
    TRIPLET_EIGHTH,     // 1/3 beat
    TRIPLET_SIXTEENTH   // 1/6 beat
}