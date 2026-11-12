package com.hardtekk.electribe.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Procedural Sound Generator for Hardtekk/Hardtechno sounds
 * Generates authentic 909/808-style kicks, gabber kicks, acid bass, etc.
 * Saves as WAV files to internal storage on first launch
 */
class SoundGenerator(private val context: Context) {
    private val sampleRate = 44100
    private val bitDepth = 16
    private val channels = 1 // Mono for all drum sounds

    // Sound definitions matching the 16 pads
    private val soundDefinitions = listOf(
        SoundDef("kick_hardtekk", "Hardtekk Kick", SampleCategory.KICK, 0.8, ::generateHardtekkKick),
        SoundDef("kick_gabber", "Gabber Kick", SampleCategory.KICK, 0.6, ::generateGabberKick),
        SoundDef("bass_offbeat", "Offbeat Bass", SampleCategory.BASS, 1.2, ::generateOffbeatBass),
        SoundDef("snare", "Snare", SampleCategory.SNARE, 0.4, ::generateSnare),
        SoundDef("clap", "Clap", SampleCategory.CLAP, 0.3, ::generateClap),
        SoundDef("hat_closed", "Closed HH", SampleCategory.HI_HAT_CLOSED, 0.15, ::generateClosedHat),
        SoundDef("hat_open", "Open HH", SampleCategory.HI_HAT_OPEN, 0.4, ::generateOpenHat),
        SoundDef("acid_stab", "Acid Stab", SampleCategory.SYNTH, 0.6, ::generateAcidStab),
        SoundDef("ride", "Ride", SampleCategory.PERCUSSION, 0.8, ::generateRide),
        SoundDef("kick_909", "909 Kick", SampleCategory.KICK, 0.9, ::generate909Kick),
        SoundDef("kick_808", "808 Kick", SampleCategory.KICK, 1.0, ::generate808Kick),
        SoundDef("perc_1", "Perc 1", SampleCategory.PERCUSSION, 0.2, ::generatePerc1),
        SoundDef("perc_2", "Perc 2", SampleCategory.PERCUSSION, 0.25, ::generatePerc2),
        SoundDef("fx_1", "FX 1", SampleCategory.FX, 0.5, ::generateFx1),
        SoundDef("fx_2", "FX 2", SampleCategory.FX, 0.5, ::generateFx2),
        SoundDef("silence", "Empty", SampleCategory.CUSTOM, 0.1, ::generateSilence)
    )

    data class SoundDef(
        val fileName: String,
        val displayName: String,
        val category: SampleCategory,
        val durationSeconds: Double,
        val generator: (Double) -> FloatArray
    )

    /**
     * Generate all built-in sounds and save as WAV files
     * Returns list of generated SampleData
     */
    suspend fun generateAllSounds(): List<SampleData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SampleData>()
        val soundsDir = File(context.filesDir, "sounds")
        soundsDir.mkdirs()

        for (def in soundDefinitions) {
            val file = File(soundsDir, "${def.fileName}.wav")
            if (!file.exists()) {
                Log.d("SoundGenerator", "Generating ${def.displayName}...")
                val audioData = def.generator(def.durationSeconds)
                writeWavFile(file, audioData, sampleRate, channels, bitDepth)
            }

            val sampleData = SampleData(
                name = def.displayName,
                filePath = file.absolutePath,
                durationFrames = (def.durationSeconds * sampleRate).toInt(),
                sampleRate = sampleRate,
                channels = channels,
                bitDepth = bitDepth,
                isBuiltIn = true,
                category = def.category
            )
            results.add(sampleData)
        }

        Log.d("SoundGenerator", "Generated ${results.size} sounds")
        results
    }

    // ============================================================
    // SOUND GENERATORS - Each creates authentic Hardtekk sounds
    // ============================================================

    private fun generateHardtekkKick(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 55.0 // A1

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 30.0) // Fast decay

            // Pitch sweep: starts high, drops fast
            val pitchEnv = Math.exp(-t * 120.0)
            val freq = fundamental * (1.0 + pitchEnv * 3.0) // 55Hz to ~220Hz

            // Main oscillator - sine with harmonic content
            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * 0.7

            // Add odd harmonics for "hard" character
            sample += Math.sin(phase * 3.0) * 0.2 * env
            sample += Math.sin(phase * 5.0) * 0.1 * env
            sample += Math.sin(phase * 7.0) * 0.05 * env

            // Click/transient at start
            if (t < 0.005) {
                val clickEnv = 1.0 - t / 0.005
                sample += (Math.random() * 2.0 - 1.0) * clickEnv * 0.3
            }

            // Distortion for hardness
            sample = Math.tanh(sample * 2.5) * 0.8

            buffer[i] = (sample * env).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateGabberKick(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 45.0 // Lower, more sub

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 20.0) // Longer tail

            // Extreme pitch drop
            val pitchEnv = Math.exp(-t * 200.0)
            val freq = fundamental * (1.0 + pitchEnv * 8.0) // 45Hz to ~360Hz

            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * 0.9

            // Heavy harmonics
            sample += Math.sin(phase * 2.0) * 0.3 * env
            sample += Math.sin(phase * 3.0) * 0.25 * env
            sample += Math.sin(phase * 4.0) * 0.2 * env
            sample += Math.sin(phase * 5.0) * 0.15 * env
            sample += Math.sin(phase * 6.0) * 0.1 * env
            sample += Math.sin(phase * 7.0) * 0.08 * env
            sample += Math.sin(phase * 8.0) * 0.05 * env

            // Hard clip distortion - THE gabber sound
            sample = Math.max(-1.0, Math.min(1.0, sample * 4.0))
            sample = Math.signum(sample) * Math.pow(Math.abs(sample), 0.6)

            // Sub oscillator
            val subPhase = 2.0 * Math.PI * (fundamental * 0.5) * t
            sample += Math.sin(subPhase) * 0.3 * Math.exp(-t * 10.0)

            buffer[i] = (sample * env * 0.9).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateOffbeatBass(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 55.0 // A1

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 8.0) // Medium decay

            // Acid-style pitch envelope
            val pitchEnv = Math.exp(-t * 30.0)
            val freq = fundamental * (1.0 + pitchEnv * 0.5)

            val phase = 2.0 * Math.PI * freq * t

            // Sawtooth for acid character
            var sample = 0.0
            for (h in 1..10) {
                sample += Math.sin(phase * h) / h
            }
            sample *= 0.3

            // Filter envelope (simulated with harmonic decay)
            val filterEnv = Math.exp(-t * 15.0)
            var filteredSample = sample * (0.2 + 0.8 * filterEnv)

            // Resonance emphasis
            filteredSample += Math.sin(phase * 2.0) * 0.2 * filterEnv

            // Distortion
            filteredSample = Math.tanh(filteredSample * 3.0) * 0.7

            buffer[i] = (filteredSample * env).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateSnare(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val bodyFreq = 180.0 // Snare body frequency

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 60.0)

            // Tonal body
            val bodyPhase = 2.0 * Math.PI * bodyFreq * t
            val body = Math.sin(bodyPhase) * Math.exp(-t * 40.0) * 0.4

            // Noise component
            val noiseEnv = Math.exp(-t * 80.0)
            val noise = (Math.random() * 2.0 - 1.0) * noiseEnv * 0.6

            // High-pass filtered noise (simulate with less low freq)
            var filteredNoise = noise
            // Simple one-pole HPF
            // (in practice, use the BiquadFilter)

            val sample = body + filteredNoise

            buffer[i] = (sample * env * 1.2).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateClap(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        // Multiple early reflections for clap character
        val clapTimes = doubleArrayOf(0.0, 0.005, 0.012, 0.022, 0.035, 0.05)
        val clapGains = doubleArrayOf(1.0, 0.8, 0.6, 0.4, 0.3, 0.2)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            for (j in clapTimes.indices) {
                val delay = clapTimes[j]
                if (t >= delay) {
                    val relT = t - delay
                    val env = Math.exp(-relT * 200.0)
                    val noise = (Math.random() * 2.0 - 1.0) * env * clapGains[j]
                    sample += noise
                }
            }

            buffer[i] = (sample * 0.7).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateClosedHat(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 400.0) // Very fast decay

            // Metallic noise - bandpass filtered
            val noise = (Math.random() * 2.0 - 1.0)

            // Simulate bandpass with multiple sine waves (metallic)
            var metallic = 0.0
            val freqs = doubleArrayOf(8000.0, 10000.0, 12000.0, 14000.0, 16000.0)
            for (f in freqs) {
                metallic += Math.sin(2.0 * Math.PI * f * t) * (Math.random() * 0.5 + 0.5)
            }
            metallic /= freqs.size

            val sample = (noise * 0.3 + metallic * 0.7) * env

            buffer[i] = (sample * 0.5).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateOpenHat(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 80.0) // Longer decay

            val noise = (Math.random() * 2.0 - 1.0)

            // More low-frequency content for "open" sound
            var metallic = 0.0
            val freqs = doubleArrayOf(2000.0, 4000.0, 6000.0, 8000.0, 10000.0, 12000.0)
            for (f in freqs) {
                metallic += Math.sin(2.0 * Math.PI * f * t) * (Math.random() * 0.5 + 0.5)
            }
            metallic /= freqs.size

            val sample = (noise * 0.5 + metallic * 0.5) * env

            buffer[i] = (sample * 0.6).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateAcidStab(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 110.0 // A2

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val ampEnv = Math.exp(-t * 12.0)

            // Classic 303-style pitch envelope
            val pitchEnv = Math.exp(-t * 25.0)
            val freq = fundamental * (1.0 + pitchEnv * 1.5)

            val phase = 2.0 * Math.PI * freq * t

            // Sawtooth (square for hollower sound)
            var sample = 0.0
            for (h in 1..12 step 2) { // Odd harmonics only = square-ish
                sample += Math.sin(phase * h) / h
            }
            sample *= 0.25

            // Filter envelope - key to acid sound
            val filterEnv = Math.exp(-t * 18.0)
            val resonance = 0.8

            // Simulate filter with harmonic shaping
            var filtered = sample * (0.1 + 0.9 * filterEnv)
            // Boost around cutoff
            filtered += Math.sin(phase * 3.0) * 0.3 * filterEnv * resonance
            filtered += Math.sin(phase * 5.0) * 0.2 * filterEnv * resonance

            // Distortion
            filtered = Math.tanh(filtered * 4.0) * 0.6

            buffer[i] = (filtered * ampEnv).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateRide(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 15.0)

            // Ride = multiple inharmonic partials (bell-like)
            var sample = 0.0
            val partials = doubleArrayOf(
                500.0, 800.0, 1200.0, 1800.0, 2600.0, 3500.0,
                4600.0, 5800.0, 7200.0, 8800.0, 10500.0, 12500.0
            )
            val partialGains = doubleArrayOf(
                0.3, 0.4, 0.5, 0.6, 0.5, 0.4,
                0.35, 0.3, 0.25, 0.2, 0.15, 0.1
            )

            for (j in partials.indices) {
                val partialEnv = Math.exp(-t * (10.0 + j * 2.0))
                sample += Math.sin(2.0 * Math.PI * partials[j] * t) * partialGains[j] * partialEnv
            }

            // Add noise for "shimmer"
            val noise = (Math.random() * 2.0 - 1.0) * Math.exp(-t * 30.0) * 0.1
            sample += noise

            buffer[i] = (sample * env * 0.7).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generate909Kick(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 55.0

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 25.0)

            // 909 kick: sine with fast pitch drop, less distortion than gabber
            val pitchEnv = Math.exp(-t * 80.0)
            val freq = fundamental * (1.0 + pitchEnv * 2.5)

            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * 0.8

            // Click transient
            if (t < 0.003) {
                val clickEnv = 1.0 - t / 0.003
                sample += (Math.random() * 2.0 - 1.0) * clickEnv * 0.2
            }

            // Soft saturation
            sample = Math.tanh(sample * 1.5) * 0.9

            buffer[i] = (sample * env).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generate808Kick(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)
        val fundamental = 40.0 // Deeper sub

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 12.0) // Long decay

            // 808 kick: pure sine with very long pitch decay
            val pitchEnv = Math.exp(-t * 15.0)
            val freq = fundamental * (1.0 + pitchEnv * 4.0)

            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * 0.9

            // No distortion, pure tone
            // Add slight second harmonic for body
            sample += Math.sin(phase * 2.0) * 0.1 * Math.exp(-t * 8.0)

            buffer[i] = (sample * env * 1.0).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generatePerc1(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 100.0)

            // Short metallic perc
            val freq = 800.0 + Math.exp(-t * 200.0) * 2000.0
            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * env

            // Add noise burst
            sample += (Math.random() * 2.0 - 1.0) * Math.exp(-t * 300.0) * 0.3

            buffer[i] = (sample * 0.5).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generatePerc2(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 80.0)

            // Woodblock-like
            val freq = 1200.0
            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * env * 0.6

            // Harmonic
            sample += Math.sin(phase * 2.5) * env * 0.3
            sample += Math.sin(phase * 3.7) * env * 0.15

            buffer[i] = (sample * 0.6).toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateFx1(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 5.0)

            // Downward sweep
            val freq = 2000.0 * Math.exp(-t * 10.0) + 200.0
            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * env * 0.3

            // Add noise
            sample += (Math.random() * 2.0 - 1.0) * env * 0.2

            buffer[i] = sample.toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateFx2(duration: Double): FloatArray {
        val len = (duration * sampleRate).toInt()
        val buffer = FloatArray(len)

        for (i in 0 until len) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 8.0)

            // Upward sweep + noise
            val freq = 200.0 + t * 3000.0
            val phase = 2.0 * Math.PI * freq * t
            var sample = Math.sin(phase) * env * 0.25

            sample += (Math.random() * 2.0 - 1.0) * Math.exp(-t * 20.0) * 0.3

            buffer[i] = sample.toFloat()
        }

        normalize(buffer)
        return buffer
    }

    private fun generateSilence(duration: Double): FloatArray {
        return FloatArray((duration * sampleRate).toInt())
    }

    // ============================================================
    // UTILITY FUNCTIONS
    // ============================================================

    private fun normalize(buffer: FloatArray) {
        var maxVal = 0.0f
        for (sample in buffer) {
            maxVal = maxOf(maxVal, Math.abs(sample))
        }
        if (maxVal > 0.0f && maxVal < 1.0f) {
            val gain = 0.95f / maxVal
            for (i in buffer.indices) {
                buffer[i] *= gain
            }
        }
    }

    private fun writeWavFile(
        file: File,
        audioData: FloatArray,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        val bytesPerSample = bitDepth / 8
        val dataSize = audioData.size * channels * bytesPerSample
        val fileSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(fileSize)
        buffer.put("WAVE".toByteArray())

        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // PCM format chunk size
        buffer.putShort(1) // Audio format (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * bytesPerSample) // Byte rate
        buffer.putShort((channels * bytesPerSample).toShort()) // Block align
        buffer.putShort(bitDepth.toShort()) // Bits per sample

        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        // Audio data
        for (sample in audioData) {
            val clamped = (sample * 32767.0).toInt().coerceIn(-32768, 32767)
            buffer.putShort(clamped.toShort())
        }

        FileOutputStream(file).use { fos ->
            fos.write(buffer.array())
        }
    }
}