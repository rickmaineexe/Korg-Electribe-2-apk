package com.hardtekk.electribe.utils

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV file writer for exporting samples, slices, and recordings
 */
object WavWriter {
    private const val TAG = "WavWriter"

    /**
     * Write FloatArray audio data to WAV file
     */
    fun writeWavFile(
        file: File,
        audioData: FloatArray,
        sampleRate: Int = 44100,
        channels: Int = 1,
        bitDepth: Int = 16
    ): Boolean {
        return try {
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
            if (bitDepth == 16) {
                for (sample in audioData) {
                    val clamped = (sample * 32767.0).toInt().coerceIn(-32768, 32767)
                    buffer.putShort(clamped.toShort())
                }
            } else if (bitDepth == 24) {
                for (sample in audioData) {
                    val clamped = (sample * 8388607.0).toInt().coerceIn(-8388608, 8388607)
                    buffer.put((clamped and 0xFF).toByte())
                    buffer.put((clamped shr 8 and 0xFF).toByte())
                    buffer.put((clamped shr 16 and 0xFF).toByte())
                }
            } else if (bitDepth == 32) {
                for (sample in audioData) {
                    val clamped = (sample * 2147483647.0).toLong().coerceIn(-2147483648L, 2147483647L)
                    buffer.putInt(clamped.toInt())
                }
            }

            FileOutputStream(file).use { fos ->
                fos.write(buffer.array())
            }

            Log.d(TAG, "Wrote WAV file: ${file.name} (${audioData.size} frames, $sampleRate Hz, $channels ch, $bitDepth bit)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write WAV file: ${file.name}", e)
            false
        }
    }

    /**
     * Write stereo FloatArray to WAV file
     */
    fun writeStereoWavFile(
        file: File,
        leftData: FloatArray,
        rightData: FloatArray,
        sampleRate: Int = 44100,
        bitDepth: Int = 16
    ): Boolean {
        val frameCount = maxOf(leftData.size, rightData.size)
        val interleaved = FloatArray(frameCount * 2)

        for (i in 0 until frameCount) {
            interleaved[i * 2] = if (i < leftData.size) leftData[i] else 0.0f
            interleaved[i * 2 + 1] = if (i < rightData.size) rightData[i] else 0.0f
        }

        return writeWavFile(file, interleaved, sampleRate, 2, bitDepth)
    }

    /**
     * Append audio data to existing WAV file (for recording)
     */
    fun appendToWavFile(file: File, audioData: FloatArray): Boolean {
        return try {
            if (!file.exists()) {
                return writeWavFile(file, audioData)
            }

            // Read existing file
            val raf = java.io.RandomAccessFile(file, "rw")
            val header = ByteArray(44)
            raf.readFully(header)

            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(22)
            val channels = bb.getShort().toInt()
            val sampleRate = bb.getInt()
            bb.position(34)
            val bitsPerSample = bb.getShort().toInt()

            // Find data chunk size
            raf.seek(40)
            val oldDataSize = raf.readInt()
            val newDataSize = oldDataSize + audioData.size * channels * bitsPerSample / 8
            val newFileSize = 36 + newDataSize

            // Update header
            raf.seek(4)
            raf.writeInt(newFileSize)
            raf.seek(40)
            raf.writeInt(newDataSize.toInt())

            // Seek to end of data and append
            raf.seek(raf.length())

            val bytesPerSample = bitsPerSample / 8
            if (bitsPerSample == 16) {
                for (sample in audioData) {
                    val clamped = (sample * 32767.0).toInt().coerceIn(-32768, 32767)
                    raf.writeShort(clamped.toShort())
                }
            }

            raf.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append to WAV file", e)
            false
        }
    }

    /**
     * Create a WAV file from a slice of another WAV file
     */
    fun extractSlice(
        sourceFile: File,
        destFile: File,
        startFrame: Int,
        endFrame: Int,
        fadeInFrames: Int = 0,
        fadeOutFrames: Int = 0
    ): Boolean {
        return try {
            val raf = java.io.RandomAccessFile(sourceFile, "r")
            val header = ByteArray(44)
            raf.readFully(header)

            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(22)
            val channels = bb.getShort().toInt()
            val sampleRate = bb.getInt()
            bb.position(34)
            val bitsPerSample = bb.getShort().toInt()

            // Find data chunk
            raf.seek(12)
            var dataPos = 44L
            var dataSize = 0L

            while (raf.getFilePointer() < raf.length()) {
                val chunkId = ByteArray(4)
                raf.readFully(chunkId)
                val chunkSize = raf.readInt().toLong()

                if (String(chunkId) == "data") {
                    dataPos = raf.getFilePointer()
                    dataSize = chunkSize
                    break
                } else {
                    raf.seek(raf.getFilePointer() + chunkSize)
                }
            }

            val totalFrames = (dataSize / (channels * bitsPerSample / 8)).toInt()
            val actualStart = startFrame.coerceIn(0, totalFrames - 1)
            val actualEnd = endFrame.coerceIn(actualStart + 1, totalFrames)
            val frameCount = actualEnd - actualStart

            raf.seek(dataPos + actualStart * channels * bitsPerSample / 8)

            val audioData = FloatArray(frameCount * channels)
            val bytesPerFrame = channels * bitsPerSample / 8
            val buffer = ByteArray(bytesPerFrame)

            for (i in 0 until frameCount) {
                val read = raf.read(buffer)
                if (read < bytesPerFrame) break

                var sum = 0.0
                if (bitsPerSample == 16) {
                    val sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                    for (c in 0 until channels) {
                        val sample = sb.getShort().toInt() / 32768.0
                        audioData[i * channels + c] = sample.toFloat()
                    }
                }

                // Apply fade in/out
                var gain = 1.0f
                if (i < fadeInFrames && fadeInFrames > 0) {
                    gain = i.toFloat() / fadeInFrames
                } else if (i >= frameCount - fadeOutFrames && fadeOutFrames > 0) {
                    gain = (frameCount - i).toFloat() / fadeOutFrames
                }

                for (c in 0 until channels) {
                    audioData[i * channels + c] *= gain
                }
            }

            raf.close()

            // Write as mono if stereo source
            val outputChannels = if (channels == 2) 1 else channels
            val outputData = if (channels == 2) {
                val mono = FloatArray(frameCount)
                for (i in 0 until frameCount) {
                    mono[i] = (audioData[i * 2] + audioData[i * 2 + 1]) * 0.5f
                }
                mono
            } else {
                audioData
            }

            writeWavFile(destFile, outputData, sampleRate, outputChannels, bitsPerSample)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract slice", e)
            false
        }
    }
}