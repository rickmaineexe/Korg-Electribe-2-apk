package com.hardtekk.electribe.audio.DSP

/**
 * Distortion/Overdrive effects for Hardtekk sounds
 * Multiple algorithms: Soft Clip, Hard Clip, Tube, Fold, Bitcrush
 */
class Distortion {
    enum class DistortionType {
        SOFT_CLIP,      // Tanh soft clipping - warm, analog-like
        HARD_CLIP,      // Hard clipping - aggressive, digital
        TUBE,           // Tube emulation - asymmetric, warm
        FOLD,           // Wave folding - metallic, complex
        BITCRUSH,       // Bit reduction + sample rate reduction
        OVERDRIVE,      // Guitar-style overdrive
        FUZZ            // Heavy fuzz - extreme saturation
    }

    var type: DistortionType = DistortionType.SOFT_CLIP
    var drive: Float = 1.0f        // 0.0 - 10.0 (input gain)
    var mix: Float = 1.0f          // 0.0 - 1.0 (dry/wet)
    var tone: Float = 0.5f         // 0.0 - 1.0 (post-filter)
    var outputGain: Float = 1.0f   // Output compensation

    // Tone filter
    private val toneFilter = BiquadFilter()
    private var sampleRate: Double = 44100.0

    // Bitcrush state
    private var bitcrushPhase: Float = 0.0f
    private var bitcrushLastSample: Float = 0.0f

    init {
        toneFilter.setSampleRate(sampleRate)
        toneFilter.setType(BiquadFilter.FilterType.LOW_PASS)
        updateToneFilter()
    }

    fun setSampleRate(sr: Double) {
        sampleRate = sr
        toneFilter.setSampleRate(sr)
    }

    fun setDrive(d: Float) {
        drive = d.coerceIn(0.0f, 20.0f)
    }

    fun setMix(m: Float) {
        mix = m.coerceIn(0.0f, 1.0f)
    }

    fun setTone(t: Float) {
        tone = t.coerceIn(0.0f, 1.0f)
        updateToneFilter()
    }

    fun setOutputGain(g: Float) {
        outputGain = g.coerceIn(0.0f, 10.0f)
    }

    private fun updateToneFilter() {
        // Map tone 0-1 to frequency range 200Hz - 20kHz
        val freq = 200.0 * Math.pow(100.0, tone.toDouble()) // 200 to 20000
        toneFilter.setFrequency(freq)
        toneFilter.setResonance(0.707)
    }

    @Suppress("UNUSED_PARAMETER")
    fun process(input: Float): Float {
        var x = input * drive

        // Apply distortion based on type
        val distorted = when (type) {
            DistortionType.SOFT_CLIP -> tanh(x)
            DistortionType.HARD_CLIP -> x.coerceIn(-1.0f, 1.0f)
            DistortionType.TUBE -> tubeDistortion(x)
            DistortionType.FOLD -> waveFold(x)
            DistortionType.BITCRUSH -> bitcrush(x)
            DistortionType.OVERDRIVE -> overdrive(x)
            DistortionType.FUZZ -> fuzz(x)
        }

        // Apply tone filter
        val filtered = toneFilter.process(distorted.toDouble()).toFloat()

        // Mix dry/wet
        val mixed = input + (filtered - input) * mix

        return mixed * outputGain
    }

    fun processBuffer(input: FloatArray, output: FloatArray, offset: Int = 0, length: Int = -1) {
        val len = if (length == -1) input.size - offset else length.coerceAtMost(input.size - offset)

        for (i in 0 until len) {
            output[offset + i] = process(input[offset + i])
        }
    }

    fun processStereo(inputL: Float, inputR: Float): Pair<Float, Float> {
        return process(inputL) to process(inputR)
    }

    private fun tanh(x: Float): Float {
        // Fast tanh approximation
        val x2 = x * x
        val x4 = x2 * x2
        return x * (1.0f - x2 * (1.0f / 3.0f - x2 * (2.0f / 15.0f - x4 * (17.0f / 315.0f))))
    }

    private fun tubeDistortion(x: Float): Float {
        // Asymmetric tube-like distortion
        val pos = when {
            x > 0 -> (1.0 - Math.exp(-x * 2.0)).toFloat()
            else -> (Math.exp(x * 2.0) - 1.0).toFloat()
        }
        return pos * 0.5f + x * 0.5f // Blend with clean for warmth
    }

    private fun waveFold(x: Float): Float {
        // Wave folding - creates complex harmonics
        var folded = x * 3.0f // Increase gain for folding
        val threshold = 1.0f

        while (folded > threshold || folded < -threshold) {
            if (folded > threshold) {
                folded = threshold - (folded - threshold)
            } else if (folded < -threshold) {
                folded = -threshold - (folded + threshold)
            }
        }
        return folded * 0.5f
    }

    private fun bitcrush(x: Float): Float {
        // Bit depth reduction (simulate 4-16 bits)
        val bits = (2.0 + drive * 14.0).toInt() // 2 to 16 bits
        val levels = (1 shl bits) - 1
        val quantized = Math.round(x * levels) / levels.toFloat()

        // Sample rate reduction
        val downsample = (1 + drive * 30).toInt() // 1 to 31
        bitcrushPhase += 1.0f
        if (bitcrushPhase >= downsample) {
            bitcrushPhase = 0.0f
            bitcrushLastSample = quantized
        }
        return bitcrushLastSample
    }

    private fun overdrive(x: Float): Float {
        // Guitar overdrive style - soft asymmetric
        val gain = 1.0 + drive * 9.0
        val driven = x * gain
        // Soft asymmetric clipping
        return when {
            driven > 0.5 -> 0.5 + (driven - 0.5) * 0.3
            driven < -0.5 -> -0.5 + (driven + 0.5) * 0.5
            else -> driven
        } / gain * 2.0f
    }

    private fun fuzz(x: Float): Float {
        // Extreme fuzz - square wave-ish
        val gain = 1.0 + drive * 50.0
        val driven = x * gain
        return Math.signum(driven) * 0.9f
    }

    fun reset() {
        toneFilter.reset()
        bitcrushPhase = 0.0f
        bitcrushLastSample = 0.0f
    }
}