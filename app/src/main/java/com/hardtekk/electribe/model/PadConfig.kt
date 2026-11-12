package com.hardtekk.electribe.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class PadConfig(
    val padIndex: Int = 0,           // 0-15
    val sampleName: String = "",     // Name of the loaded sample
    val samplePath: String = "",     // Path to the sample file (internal or custom)
    val volume: Float = 1.0f,        // 0.0 - 1.0
    val pitch: Float = 0.0f,         // Semitones: -24 to +24
    val filterCutoff: Float = 1.0f,  // 0.0 - 1.0 (normalized)
    val filterResonance: Float = 0.0f, // 0.0 - 1.0
    val filterType: FilterType = FilterType.LOW_PASS,
    val distortionAmount: Float = 0.0f, // 0.0 - 1.0
    val delayAmount: Float = 0.0f,   // 0.0 - 1.0
    val delayTime: Float = 0.5f,     // 0.0 - 1.0 (normalized to max delay)
    val delayFeedback: Float = 0.3f, // 0.0 - 0.95
    val pan: Float = 0.0f,           // -1.0 (left) to 1.0 (right)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val chokeGroup: Int = 0,         // 0 = no choke, 1-16 = choke group
    val oneShot: Boolean = true,     // true = one-shot, false = loop
    val startOffset: Float = 0.0f,   // Sample start offset 0.0 - 1.0
    val endOffset: Float = 1.0f,     // Sample end offset 0.0 - 1.0
    val reverse: Boolean = false,
    val velocitySensitivity: Float = 1.0f // 0.0 - 1.0
) {
    companion object {
        fun defaultForPad(index: Int, sampleName: String = "", samplePath: String = ""): PadConfig {
            return PadConfig(
                padIndex = index,
                sampleName = sampleName,
                samplePath = samplePath,
                volume = 1.0f,
                pitch = 0.0f,
                filterCutoff = 1.0f,
                filterResonance = 0.0f,
                filterType = FilterType.LOW_PASS,
                distortionAmount = 0.0f,
                delayAmount = 0.0f,
                delayTime = 0.5f,
                delayFeedback = 0.3f,
                pan = 0.0f
            )
        }
    }
}

enum class FilterType(@Serializable(with = FilterTypeSerializer::class)) {
    LOW_PASS,
    HIGH_PASS,
    BAND_PASS,
    NOTCH
}

class FilterTypeSerializer : kotlinx.serialization.KSerializer<FilterType> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("FilterType", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.Encoding, value: FilterType) {
        encoder.encodeStringValue(value.name)
    }

    override fun deserialize(decoder: kotlinx.serialization.Decoding): FilterType {
        return FilterType.valueOf(decoder.decodeString())
    }
}