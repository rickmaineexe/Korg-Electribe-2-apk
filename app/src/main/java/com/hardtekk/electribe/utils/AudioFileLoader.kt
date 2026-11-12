package com.hardtekk.electribe.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.hardtekk.electribe.model.SampleData
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio file loader - reads WAV/MP3 metadata and waveform data
 */
object AudioFileLoader {
    private const val TAG = "AudioFileLoader"

    /**
     * Load sample metadata from file
     */
    fun loadSampleInfo(file: File): SampleData? {
        return try {
            val raf = RandomAccessFile(file, "r")
            val header = ByteArray(44)
            val bytesRead = raf.read(header)

            if (bytesRead < 44) {
                raf.close()
                return loadWithMediaMetadataRetriever(file)
            }

            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            // Check RIFF/WAVE header
            val riffId = String(header, 0, 4)
            val waveId = String(header, 8, 4)
            if (riffId != "RIFF" || waveId != "WAVE") {
                raf.close()
                return loadWithMediaMetadataRetriever(file)
            }

            // Find fmt chunk
            var fmtFound = false
            var dataPos = 44L
            raf.seek(12)

            while (raf.getFilePointer() < raf.length()) {
                val chunkId = ByteArray(4)
                raf.readFully(chunkId)
                val chunkSize = raf.readInt()

                if (String(chunkId) == "fmt ") {
                    fmtFound = true
                    val fmtData = ByteArray(chunkSize)
                    raf.readFully(fmtData)
                    val fmtBB = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN)

                    val audioFormat = fmtBB.getShort().toInt()
                    val channels = fmtBB.getShort().toInt()
                    val sampleRate = fmtBB.getInt()
                    val byteRate = fmtBB.getInt()
                    val blockAlign = fmtBB.getShort().toInt()
                    val bitsPerSample = fmtBB.getShort().toInt()

                    // Skip to data chunk
                    raf.seek(raf.getFilePointer() + (chunkSize - fmtData.size))
                } else if (String(chunkId) == "data") {
                    dataPos = raf.getFilePointer()
                    val dataSize = chunkSize.toLong()
                    val frameCount = if (blockAlign > 0) dataSize / blockAlign else 0

                    val sample = SampleData(
                        name = FileUtils.getNameWithoutExtension(file),
                        filePath = file.absolutePath,
                        durationFrames = frameCount.toInt(),
                        sampleRate = sampleRate,
                        channels = channels,
                        bitDepth = bitsPerSample,
                        isBuiltIn = false,
                        category = com.hardtekk.electribe.model.SampleData.SampleCategory.CUSTOM
                    )

                    raf.close()
                    return sample
                } else {
                    // Skip unknown chunk
                    raf.seek(raf.getFilePointer() + chunkSize)
                }
            }

            raf.close()
            loadWithMediaMetadataRetriever(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sample info: ${file.name}", e)
            loadWithMediaMetadataRetriever(file)
        }
    }

    /**
     * Fallback using MediaMetadataRetriever for MP3 and other formats
     */
    private fun loadWithMediaMetadataRetriever(file: File): SampleData? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLong() ?: 0
            val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLE_RATE)
            val sampleRate = sampleRateStr?.toInt() ?: 44100

            val sample = SampleData(
                name = FileUtils.getNameWithoutExtension(file),
                filePath = file.absolutePath,
                durationFrames = (durationMs * sampleRate / 1000).toInt(),
                sampleRate = sampleRate,
                channels = 2, // Assume stereo for compressed formats
                bitDepth = 16,
                isBuiltIn = false,
                category = com.hardtekk.electribe.model.SampleData.SampleCategory.CUSTOM
            )

            sample
        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever failed", e)
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Load waveform data for display (downsampled)
     */
    fun loadWaveformData(file: File, targetPoints: Int = 512): FloatArray? {
        return try {
            val raf = RandomAccessFile(file, "r")
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

            if (dataSize == 0) {
                raf.close()
                return null
            }

            val frameCount = dataSize / (channels * bitsPerSample / 8)
            val framesPerPoint = maxOf(1, frameCount / targetPoints)
            val waveform = FloatArray(targetPoints)

            raf.seek(dataPos)

            val bytesPerFrame = channels * bitsPerSample / 8
            val bufferSize = minOf(8192, (framesPerPoint * bytesPerFrame).toInt())
            val buffer = ByteArray(bufferSize)

            var pointIndex = 0
            var frameIndex = 0L

            while (pointIndex < targetPoints && raf.getFilePointer() < dataPos + dataSize) {
                var maxVal = 0.0
                var framesRead = 0

                while (framesRead < framesPerPoint && raf.getFilePointer() < dataPos + dataSize) {
                    val read = raf.read(buffer, 0, minOf(bytesPerFrame, buffer.size))
                    if (read < bytesPerFrame) break

                    var sampleVal = 0.0
                    if (bitsPerSample == 16) {
                        val sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                        for (c in 0 until channels) {
                            sampleVal += (sb.getShort().toInt() / 32768.0).absoluteValue
                        }
                        sampleVal /= channels
                    } else if (bitsPerSample == 24) {
                        // 24-bit handling
                        for (c in 0 until channels) {
                            val idx = c * 3
                            val sample = (buffer[idx + 2].toInt() shl 16) |
                                ((buffer[idx + 1].toInt() and 0xFF) shl 8) |
                                (buffer[idx].toInt() and 0xFF)
                            sampleVal += (sample / 8388608.0).absoluteValue
                        }
                        sampleVal /= channels
                    }

                    maxVal = maxOf(maxVal, sampleVal)
                    framesRead++
                    frameIndex++
                }

                waveform[pointIndex] = maxVal.toFloat()
                pointIndex++
            }

            raf.close()
            waveform
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load waveform: ${file.name}", e)
            null
        }
    }

    /**
     * Load full audio data as FloatArray (for processing)
     */
    fun loadAudioData(file: File, startFrame: Int = 0, maxFrames: Int = Int.MAX_VALUE): FloatArray? {
        return try {
            val raf = RandomAccessFile(file, "r")
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
            val endFrame = minOf(startFrame + maxFrames, totalFrames)
            val frameCount = endFrame - startFrame

            if (frameCount <= 0) {
                raf.close()
                return FloatArray(0)
            }

            raf.seek(dataPos + startFrame * channels * bitsPerSample / 8)

            val audioData = FloatArray(frameCount)
            val bytesPerFrame = channels * bitsPerSample / 8
            val buffer = ByteArray(bytesPerFrame)

            for (i in 0 until frameCount) {
                val read = raf.read(buffer)
                if (read < bytesPerFrame) break

                var sum = 0.0
                if (bitsPerSample == 16) {
                    val sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                    for (c in 0 until channels) {
                        sum += sb.getShort().toInt() / 32768.0
                    }
                } else if (bitsPerSample == 24) {
                    for (c in 0 until channels) {
                        val idx = c * 3
                        val sample = (buffer[idx + 2].toInt() shl 16) |
                            ((buffer[idx + 1].toInt() and 0xFF) shl 8) |
                            (buffer[idx].toInt() and 0xFF)
                        // Sign extend 24-bit
                        val signed = if (sample and 0x800000 != 0) sample | 0xFF000000 else sample
                        sum += signed / 8388608.0
                    }
                } else if (bitsPerSample == 32) {
                    val ib = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                    for (c in 0 until channels) {
                        sum += ib.getInt() / 2147483648.0
                    }
                }

                audioData[i] = (sum / channels).toFloat()
            }

            raf.close()
            audioData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio data: ${file.name}", e)
            null
        }
    }

    /**
     * Check if file is a valid audio file
     */
    fun isValidAudioFile(file: File): Boolean {
        return try {
            val raf = RandomAccessFile(file, "r")
            val header = ByteArray(12)
            raf.readFully(header)
            raf.close()

            val riffId = String(header, 0, 4)
            val waveId = String(header, 8, 4)
            riffId == "RIFF" && waveId == "WAVE"
        } catch (e: Exception) {
            false
        }
    }
}