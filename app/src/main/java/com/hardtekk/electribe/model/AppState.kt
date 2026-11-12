package com.hardtekk.electribe.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class AppState(
    // Audio Engine
    val masterVolume: Float = 0.8f,
    val masterPitch: Float = 0.0f,
    val masterFilterCutoff: Float = 1.0f,
    val masterFilterResonance: Float = 0.0f,
    val masterFilterType: FilterType = FilterType.LOW_PASS,
    val masterDistortion: Float = 0.0f,
    val masterDelayAmount: Float = 0.0f,
    val masterDelayTime: Float = 0.5f,
    val masterDelayFeedback: Float = 0.3f,

    // Sequencer
    val currentBpm: Int = 140,
    val targetBpm: Int = 140,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val currentPatternIndex: Int = 0,
    val currentStep: Int = 0,
    val sequencerMode: SequencerMode = SequencerMode.TRIGGER,
    val quantize: QuantizeMode = QuantizeMode.OFF,
    val swing: Float = 0.0f,
    val patternLength: Int = 16, // 16 or 32

    // UI State
    val selectedPadIndex: Int = 0,
    val selectedKnobIndex: Int = -1,
    val showWaveform: Boolean = false,
    val waveformSampleId: String = "",
    val sliceStartFrame: Int = 0,
    val sliceEndFrame: Int = 0,
    val isSliceMode: Boolean = false,

    // Metronome
    val metronomeEnabled: Boolean = true,
    val metronomeVolume: Float = 0.5f,

    // Audio Settings
    val bufferSize: Int = 256,
    val sampleRate: Int = 44100,
    val enableLowLatency: Boolean = true
) {
    companion object {
        val DEFAULT_BPM = 140
        val MIN_BPM = 120
        val MAX_BPM = 220
    }

    fun withBpm(bpm: Int): AppState = copy(
        currentBpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
        targetBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    )

    fun withTargetBpm(bpm: Int): AppState = copy(
        targetBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    )

    fun incrementBpm(): AppState = withTargetBpm(targetBpm + 1)
    fun decrementBpm(): AppState = withTargetBpm(targetBpm - 1)

    fun nextPattern(): AppState = copy(
        currentPatternIndex = (currentPatternIndex + 1) % 4
    )

    fun previousPattern(): AppState = copy(
        currentPatternIndex = (currentPatternIndex + 3) % 4
    )

    fun togglePlay(): AppState = copy(isPlaying = !isPlaying)
    fun setPlaying(playing: Boolean): AppState = copy(isPlaying = playing)

    fun toggleRecord(): AppState = copy(isRecording = !isRecording)
    fun setRecording(recording: Boolean): AppState = copy(isRecording = recording)

    fun nextMode(): AppState = copy(
        sequencerMode = when (sequencerMode) {
            SequencerMode.TRIGGER -> SequencerMode.STEP_SEQUENCER
            SequencerMode.STEP_SEQUENCER -> SequencerMode.SAMPLE_SLICER
            SequencerMode.SAMPLE_SLICER -> SequencerMode.TRIGGER
        }
    )

    fun setMode(mode: SequencerMode): AppState = copy(sequencerMode = mode)
}

enum class SequencerMode(@Serializable(with = SequencerModeSerializer::class)) {
    TRIGGER,          // Direct pad triggering
    STEP_SEQUENCER,   // 16/32 step sequencing
    SAMPLE_SLICER     // Sample chopping/slicing
}

class SequencerModeSerializer : kotlinx.serialization.KSerializer<SequencerMode> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("SequencerMode", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.Encoding, value: SequencerMode) {
        encoder.encodeStringValue(value.name)
    }

    override fun deserialize(decoder: kotlinx.serialization.Decoding): SequencerMode {
        return SequencerMode.valueOf(decoder.decodeString())
    }
}

enum class QuantizeMode(@Serializable(with = QuantizeModeSerializer::class)) {
    OFF,        // No quantization
    QUARTER,    // 1/4 notes
    EIGHTH,     // 1/8 notes
    SIXTEENTH,  // 1/16 notes
    THIRTY_SECOND // 1/32 notes
}

class QuantizeModeSerializer : kotlinx.serialization.KSerializer<QuantizeMode> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("QuantizeMode", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.Encoding, value: QuantizeMode) {
        encoder.encodeStringValue(value.name)
    }

    override fun deserialize(decoder: kotlinx.serialization.Decoding): QuantizeMode {
        return QuantizeMode.valueOf(decoder.decodeString())
    }
}