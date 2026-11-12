package com.hardtekk.electribe.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class SampleData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val filePath: String = "",
    val durationFrames: Int = 0,
    val sampleRate: Int = 44100,
    val channels: Int = 1, // 1 = mono, 2 = stereo
    val bitDepth: Int = 16,
    val isBuiltIn: Boolean = false,
    val category: SampleCategory = SampleCategory.CUSTOM,
    val waveformData: FloatArray? = null, // Downsampled waveform for display
    val peakAmplitude: Float = 0.0f,
    val rmsAmplitude: Float = 0.0f,
    val dateAdded: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
) {
    fun getDurationSeconds(): Float = durationFrames.toFloat() / sampleRate

    fun getFormattedDuration(): String {
        val totalSecs = getDurationSeconds()
        val mins = (totalSecs / 60).toInt()
        val secs = (totalSecs % 60).toInt()
        val millis = ((totalSecs * 1000) % 1000).toInt()
        return if (mins > 0) {
            String.format("%d:%02d.%02d", mins, secs, millis / 10)
        } else {
            String.format("%d.%02d", secs, millis / 10)
        }
    }
}

enum class SampleCategory(@Serializable(with = SampleCategorySerializer::class)) {
    KICK,
    SNARE,
    CLAP,
    HI_HAT_CLOSED,
    HI_HAT_OPEN,
    BASS,
    SYNTH,
    PERCUSSION,
    FX,
    CUSTOM,
    SLICED
}

class SampleCategorySerializer : kotlinx.serialization.KSerializer<SampleCategory> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("SampleCategory", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.Encoding, value: SampleCategory) {
        encoder.encodeStringValue(value.name)
    }

    override fun deserialize(decoder: kotlinx.serialization.Decoding): SampleCategory {
        return SampleCategory.valueOf(decoder.decodeString())
    }
}

// Slice data for the sample slicer
@Serializable
@Keep
data class SliceData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sampleId: String,
    val name: String,
    val startFrame: Int,
    val endFrame: Int,
    val assignedPad: Int = -1, // -1 = not assigned
    val gain: Float = 1.0f,
    val pitch: Float = 0.0f,
    val loopEnabled: Boolean = false,
    val loopStartFrame: Int = 0,
    val loopEndFrame: Int = 0,
    val fadeInFrames: Int = 0,
    val fadeOutFrames: Int = 0
) {
    val durationFrames: Int
        get() = endFrame - startFrame

    fun getDurationSeconds(sampleRate: Int): Float = durationFrames.toFloat() / sampleRate
}