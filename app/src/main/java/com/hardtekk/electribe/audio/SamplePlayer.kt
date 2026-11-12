package com.hardtekk.electribe.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInt

/**
 * Sample Player with real-time DSP effects
 * Supports polyphonic playback, pitch shifting, filtering, distortion, delay
 */
class SamplePlayer(
    private val context: Context,
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 256
) {
    private val audioTrack: AudioTrack
    private val writeThread: Thread
    private val isRunning = AtomicBoolean(true)
    private val currentBuffer = FloatArray(bufferSize * 2) // Stereo
    private val outputBuffer = ShortArray(bufferSize * 2)

    // Voice management for polyphony
    private val voices = MutableList(32) { Voice() }
    private val voiceLock = Any()

    // Master effects
    private val masterFilter = BiquadFilter()
    private val masterDistortion = Distortion()
    private val masterDelay = Delay()

    // Audio format
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO

    init {
        // Initialize AudioTrack with low latency
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        val trackBufferSize = maxOf(minBufferSize, bufferSize * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setFlags(android.media.AudioAttributes.FLAG_LOW_LATENCY)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferSize * 2) // bytes
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        // Initialize master effects
        masterFilter.setSampleRate(sampleRate.toDouble())
        masterDistortion.setSampleRate(sampleRate.toDouble())
        masterDelay.setSampleRate(sampleRate.toDouble())

        // Start audio thread
        writeThread = Thread(priority = Thread.MAX_PRIORITY) {
            audioLoop()
        }
        writeThread.start()

        audioTrack.play()
    }

    private fun audioLoop() {
        val frameTimeNs = (1_000_000_000L * bufferSize / sampleRate)

        while (isRunning.get()) {
            val startTime = System.nanoTime()

            // Clear buffer
            currentBuffer.fill(0.0f)

            // Mix all active voices
            mixVoices()

            // Apply master effects
            applyMasterEffects()

            // Convert to PCM16 and write
            convertAndWrite()

            // Sleep to maintain buffer rate
            val elapsed = System.nanoTime() - startTime
            val sleepTime = frameTimeNs - elapsed
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1_000_000, (sleepTime % 1_000_000).toInt())
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun mixVoices() {
        synchronized(voiceLock) {
            for (voice in voices) {
                if (voice.isActive) {
                    voice.processBuffer(currentBuffer, bufferSize)
                }
            }
        }
    }

    private fun applyMasterEffects() {
        // Split stereo for processing
        val left = FloatArray(bufferSize)
        val right = FloatArray(bufferSize)
        for (i in 0 until bufferSize) {
            left[i] = currentBuffer[i * 2]
            right[i] = currentBuffer[i * 2 + 1]
        }

        // Master filter
        masterFilter.processBufferStereo(left, right, left, right, 0, bufferSize)

        // Master distortion
        masterDistortion.processBuffer(left, left, 0, bufferSize)
        masterDistortion.processBuffer(right, right, 0, bufferSize)

        // Master delay
        masterDelay.processBuffer(left, right, left, right, 0, bufferSize)

        // Recombine
        for (i in 0 until bufferSize) {
            currentBuffer[i * 2] = left[i]
            currentBuffer[i * 2 + 1] = right[i]
        }
    }

    private fun convertAndWrite() {
        for (i in currentBuffer.indices) {
            val sample = (currentBuffer[i] * 32767.0).toInt().coerceIn(-32768, 32767)
            outputBuffer[i] = sample.toShort()
        }
        audioTrack.write(outputBuffer, 0, outputBuffer.size, AudioTrack.WRITE_BLOCKING)
    }

    /**
     * Trigger a sample on a pad
     */
    fun triggerPad(
        padIndex: Int,
        samplePath: String,
        config: PadConfig,
        velocity: Float = 1.0f
    ): Int {
        return synchronized(voiceLock) {
            // Find free voice (or steal oldest)
            var voiceIndex = -1
            var oldestTime = Long.MAX_VALUE

            for (i in voices.indices) {
                if (!voices[i].isActive) {
                    voiceIndex = i
                    break
                }
                if (voices[i].startTime < oldestTime) {
                    oldestTime = voices[i].startTime
                    voiceIndex = i
                }
            }

            // Check choke group
            if (config.chokeGroup > 0) {
                for (i in voices.indices) {
                    if (voices[i].isActive && voices[i].chokeGroup == config.chokeGroup) {
                        voices[i].stop()
                    }
                }
            }

            val voice = voices[voiceIndex]
            voice.start(
                samplePath = samplePath,
                config = config,
                velocity = velocity,
                chokeGroup = config.chokeGroup
            )

            voiceIndex
        }
    }

    /**
     * Stop a specific voice
     */
    fun stopVoice(voiceIndex: Int) {
        synchronized(voiceLock) {
            if (voiceIndex in voices.indices) {
                voices[voiceIndex].stop()
            }
        }
    }

    /**
     * Stop all voices for a pad (choke)
     */
    fun stopPad(padIndex: Int) {
        synchronized(voiceLock) {
            for (voice in voices) {
                if (voice.isActive && voice.padIndex == padIndex) {
                    voice.stop()
                }
            }
        }
    }

    /**
     * Stop all voices
     */
    fun stopAll() {
        synchronized(voiceLock) {
            for (voice in voices) {
                voice.stop()
            }
        }
    }

    // Master effect setters
    fun setMasterVolume(volume: Float) {
        // Applied in voice processing
    }

    fun setMasterFilter(type: BiquadFilter.FilterType, freq: Float, resonance: Float) {
        masterFilter.setType(type)
        masterFilter.setFrequency(freq.toDouble())
        masterFilter.setResonance(resonance.toDouble())
    }

    fun setMasterDistortion(type: Distortion.DistortionType, drive: Float, mix: Float) {
        masterDistortion.type = type
        masterDistortion.setDrive(drive)
        masterDistortion.setMix(mix)
    }

    fun setMasterDelay(timeMs: Float, feedback: Float, mix: Float) {
        masterDelay.setDelayTimeMs(timeMs)
        masterDelay.setFeedback(feedback)
        masterDelay.setMix(mix)
    }

    fun shutdown() {
        isRunning.set(false)
        try {
            writeThread.join(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        audioTrack.stop()
        audioTrack.release()
    }

    // ============================================================
    // VOICE CLASS - Handles individual sample playback
    // ============================================================

    private inner class Voice {
        var isActive = false
        var padIndex = -1
        var chokeGroup = 0
        var startTime = 0L
        var sampleData: FloatArray? = null
        var sampleRate = 44100
        var channels = 1
        var readPos = 0.0
        var readInc = 1.0
        var gain = 1.0f
        var pan = 0.0f
        var filter: BiquadFilter? = null
        var distortion: Distortion? = null
        var delay: Delay? = null
        var loopEnabled = false
        var loopStart = 0
        var loopEnd = 0
        var fadeOut = false
        var fadeOutPos = 0
        var fadeOutLen = 0

        fun start(
            samplePath: String,
            config: PadConfig,
            velocity: Float,
            chokeGroup: Int
        ) {
            this.chokeGroup = chokeGroup
            this.startTime = System.currentTimeMillis()
            this.padIndex = config.padIndex
            this.gain = config.volume * velocity * config.velocitySensitivity
            this.pan = config.pan
            this.readInc = Math.pow(2.0, config.pitch / 12.0)
            this.loopEnabled = !config.oneShot
            this.loopStart = (config.startOffset * sampleData?.size?.toDouble().orDefault(0.0)).toInt()
            this.loopEnd = (config.endOffset * sampleData?.size?.toDouble().orDefault(0.0)).toInt()

            // Load sample data
            loadSample(samplePath, config)

            // Setup per-voice effects
            if (config.filterCutoff < 1.0f || config.filterResonance > 0.0f) {
                filter = BiquadFilter().apply {
                    setSampleRate(sampleRate.toDouble())
                    setType(config.filterType.toBiquadType())
                    setFrequency((config.filterCutoff * 20000.0).toDouble())
                    setResonance((config.filterResonance * 20.0 + 0.5).toDouble())
                }
            }

            if (config.distortionAmount > 0.0f) {
                distortion = Distortion().apply {
                    setSampleRate(sampleRate.toDouble())
                    type = Distortion.DistortionType.SOFT_CLIP
                    setDrive(1.0f + config.distortionAmount * 9.0f)
                    setMix(config.distortionAmount)
                }
            }

            if (config.delayAmount > 0.0f) {
                delay = Delay().apply {
                    setSampleRate(sampleRate.toDouble())
                    setDelayTimeMs(config.delayTime * 1000.0f)
                    setFeedback(config.delayFeedback)
                    setMix(config.delayAmount)
                }
            }

            isActive = true
            readPos = 0.0
            fadeOut = false
        }

        private fun loadSample(path: String, config: PadConfig) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    isActive = false
                    return
                }

                val raf = RandomAccessFile(file, "r")
                val header = ByteArray(44)
                raf.readFully(header)

                // Parse WAV header
                val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                bb.position(22)
                channels = bb.getShort().toInt()
                sampleRate = bb.getInt()
                bb.position(34)
                val bitsPerSample = bb.getShort().toInt()

                val dataPos = findDataChunk(raf)
                raf.seek(dataPos)

                val dataSize = file.length() - dataPos
                val frameCount = (dataSize / (channels * bitsPerSample / 8)).toInt()

                // Apply start/end offsets
                val startFrame = (config.startOffset * frameCount).toInt()
                val endFrame = (config.endOffset * frameCount).toInt()
                val loadFrames = endFrame - startFrame

                raf.seek(dataPos + startFrame * channels * bitsPerSample / 8)

                sampleData = FloatArray(loadFrames)

                if (bitsPerSample == 16) {
                    val byteBuffer = ByteArray(loadFrames * channels * 2)
                    raf.readFully(byteBuffer)
                    val sb = ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN)

                    for (i in 0 until loadFrames) {
                        var sum = 0
                        for (c in 0 until channels) {
                            sum += sb.getShort().toInt()
                        }
                        sampleData!![i] = (sum / channels.toDouble() / 32768.0).toFloat()
                    }
                } else if (bitsPerSample == 24) {
                    val byteBuffer = ByteArray(loadFrames * channels * 3)
                    raf.readFully(byteBuffer)
                    for (i in 0 until loadFrames) {
                        var sum = 0
                        for (c in 0 until channels) {
                            val idx = (i * channels + c) * 3
                            val sample = (byteBuffer[idx + 2].toInt() shl 16) |
                                ((byteBuffer[idx + 1].toInt() and 0xFF) shl 8) |
                                (byteBuffer[idx].toInt() and 0xFF)
                            sum += sample
                        }
                        sampleData!![i] = (sum / channels.toDouble() / 8388608.0).toFloat()
                    }
                } else if (bitsPerSample == 32) {
                    val byteBuffer = ByteArray(loadFrames * channels * 4)
                    raf.readFully(byteBuffer)
                    val ib = ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until loadFrames) {
                        var sum = 0
                        for (c in 0 until channels) {
                            sum += ib.getInt()
                        }
                        sampleData!![i] = (sum / channels.toDouble() / 2147483648.0).toFloat()
                    }
                }

                // Reverse if needed
                if (config.reverse) {
                    for (i in 0 until sampleData!!.size / 2) {
                        val tmp = sampleData!![i]
                        sampleData!![i] = sampleData!![sampleData!!.size - 1 - i]
                        sampleData!![sampleData!!.size - 1 - i] = tmp
                    }
                }

                // Normalize
                var maxVal = 0.0f
                for (s in sampleData!!) {
                    maxVal = maxOf(maxVal, Math.abs(s))
                }
                if (maxVal > 0.0f) {
                    val gain = 0.95f / maxVal
                    for (i in sampleData!!.indices) {
                        sampleData!![i] *= gain
                    }
                }

                raf.close()
            } catch (e: Exception) {
                Log.e("SamplePlayer", "Error loading sample: $path", e)
                isActive = false
            }
        }

        private fun findDataChunk(raf: RandomAccessFile): Long {
            raf.seek(12)
            while (raf.getFilePointer() < raf.length()) {
                val chunkId = ByteArray(4)
                raf.readFully(chunkId)
                val chunkSize = raf.readInt()
                if (String(chunkId) == "data") {
                    return raf.getFilePointer()
                }
                raf.seek(raf.getFilePointer() + chunkSize)
            }
            return 44 // Default
        }

        fun processBuffer(output: FloatArray, frames: Int) {
            if (!isActive || sampleData == null) return

            val data = sampleData!!
            val dataLen = data.size

            for (i in 0 until frames) {
                if (!isActive) break

                var sample = 0.0f

                // Read with linear interpolation
                val idx = readPos.toInt()
                val frac = readPos - idx

                if (idx < dataLen - 1) {
                    sample = data[idx] + (data[idx + 1] - data[idx]) * frac.toFloat()
                } else if (idx < dataLen) {
                    sample = data[idx]
                } else {
                    // End of sample
                    if (loopEnabled && loopEnd > loopStart) {
                        readPos = loopStart.toDouble()
                        continue
                    } else {
                        // Fade out
                        if (!fadeOut) {
                            fadeOut = true
                            fadeOutPos = 0
                            fadeOutLen = sampleRate / 50 // 20ms fade
                        }
                        if (fadeOutPos < fadeOutLen) {
                            val fade = 1.0f - fadeOutPos.toFloat() / fadeOutLen
                            sample *= fade
                            fadeOutPos++
                        } else {
                            isActive = false
                            break
                        }
                    }
                }

                // Apply per-voice effects
                if (filter != null) {
                    sample = filter!!.process(sample.toDouble()).toFloat()
                }
                if (distortion != null) {
                    sample = distortion!!.process(sample)
                }
                if (delay != null) {
                    // Delay is applied at master level, skip per-voice
                }

                // Apply gain and pan
                val gainL = gain * (1.0f - pan) * 0.707f
                val gainR = gain * (1.0f + pan) * 0.707f

                output[i * 2] += sample * gainL
                output[i * 2 + 1] += sample * gainR

                // Advance read position
                readPos += readInc
            }
        }

        fun stop() {
            if (isActive && !fadeOut) {
                fadeOut = true
                fadeOutPos = 0
                fadeOutLen = sampleRate / 50
            }
        }

        fun setPitch(pitch: Float) {
            readInc = Math.pow(2.0, pitch / 12.0)
        }

        fun setVolume(volume: Float) {
            gain = volume
        }
    }

    // Extension for PadConfig filter type conversion
    private fun FilterType.toBiquadType(): BiquadFilter.FilterType {
        return when (this) {
            FilterType.LOW_PASS -> BiquadFilter.FilterType.LOW_PASS
            FilterType.HIGH_PASS -> BiquadFilter.FilterType.HIGH_PASS
            FilterType.BAND_PASS -> BiquadFilter.FilterType.BAND_PASS
            FilterType.NOTCH -> BiquadFilter.FilterType.NOTCH
        }
    }
}