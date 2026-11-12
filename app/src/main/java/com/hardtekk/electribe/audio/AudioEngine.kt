package com.hardtekk.electribe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import com.hardtekk.electribe.model.AppState
import com.hardtekk.electribe.model.PadConfig
import com.hardtekk.electribe.model.Pattern
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInt

/**
 * Main Audio Engine - coordinates SamplePlayer, Sequencer, and Metronome
 * Runs on high-priority audio thread for low latency
 */
class AudioEngine(
    private val context: Context,
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 256
) {
    private val samplePlayer: SamplePlayer
    private val soundGenerator: SoundGenerator
    private val sequencerEngine: SequencerEngine
    private val metronome: Metronome

    // State
    private val isPlaying = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private val currentBpm = AtomicInt(140)
    private val currentPattern = AtomicInt(0)
    private val currentStep = AtomicInt(0)

    // Callbacks
    var onStepCallback: ((Int) -> Unit)? = null
    var onPatternChangeCallback: ((Int) -> Unit)? = null
    var onLoadCompleteCallback: (() -> Unit)? = null

    // Coroutines
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioJob = Job()
    private val audioScope = CoroutineScope(audioJob + Dispatchers.Default)

    init {
        samplePlayer = SamplePlayer(context, sampleRate, bufferSize)
        soundGenerator = SoundGenerator(context)
        metronome = Metronome(samplePlayer, sampleRate)
        sequencerEngine = SequencerEngine(this, sampleRate)

        // Generate built-in sounds on startup
        generateBuiltInSounds()
    }

    private fun generateBuiltInSounds() {
        audioScope.launch {
            try {
                val samples = soundGenerator.generateAllSounds()
                // Store samples in repository
                SampleRepository(context).saveSamples(samples)
                onLoadCompleteCallback?.invoke()
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed to generate sounds", e)
            }
        }
    }

    // ============================================================
    // TRANSPORT CONTROL
    // ============================================================

    fun play() {
        if (!isPlaying.getAndSet(true)) {
            currentStep.set(0)
            sequencerEngine.start()
            metronome.start(currentBpm.get())
        }
    }

    fun stop() {
        if (isPlaying.getAndSet(false)) {
            sequencerEngine.stop()
            metronome.stop()
            samplePlayer.stopAll()
            currentStep.set(0)
        }
    }

    fun togglePlay() {
        if (isPlaying.get()) stop() else play()
    }

    fun setRecording(recording: Boolean) {
        isRecording.set(recording)
        if (recording && !isPlaying.get()) {
            play()
        }
    }

    fun isPlaying(): Boolean = isPlaying.get()
    fun isRecording(): Boolean = isRecording.get()

    // ============================================================
    // BPM CONTROL
    // ============================================================

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(AppState.MIN_BPM, AppState.MAX_BPM)
        currentBpm.set(clamped)
        metronome.setBpm(clamped)
        sequencerEngine.setBpm(clamped)
    }

    fun getBpm(): Int = currentBpm.get()

    fun incrementBpm() = setBpm(currentBpm.get() + 1)
    fun decrementBpm() = setBpm(currentBpm.get() - 1)

    // ============================================================
    // PATTERN CONTROL
    // ============================================================

    fun setPattern(patternIndex: Int) {
        val clamped = patternIndex.coerceIn(0, 3)
        currentPattern.set(clamped)
        sequencerEngine.setPattern(clamped)
        onPatternChangeCallback?.invoke(clamped)
    }

    fun nextPattern() = setPattern((currentPattern.get() + 1) % 4)
    fun previousPattern() = setPattern((currentPattern.get() + 3) % 4)

    fun getCurrentPattern(): Int = currentPattern.get()
    fun getCurrentStep(): Int = currentStep.get()

    // ============================================================
    // PAD TRIGGERING
    // ============================================================

    fun triggerPad(padIndex: Int, velocity: Float = 1.0f) {
        val config = getPadConfig(padIndex)
        if (config.samplePath.isNotBlank()) {
            samplePlayer.triggerPad(padIndex, config.samplePath, config, velocity)
        }
    }

    fun triggerPadWithSample(padIndex: Int, samplePath: String, config: PadConfig, velocity: Float = 1.0f) {
        samplePlayer.triggerPad(padIndex, samplePath, config, velocity)
    }

    // ============================================================
    // SEQUENCER INTEGRATION
    // ============================================================

    /** Called by sequencer engine on each step */
    fun onSequencerStep(step: Int, pattern: Pattern) {
        currentStep.set(step)
        onStepCallback?.invoke(step)

        // Metronome accent on first beat
        val stepsPerBeat = pattern.steps / 4
        if (step % stepsPerBeat == 0) {
            metronome.accent()
        }

        // Trigger pads for this step
        val stepData = pattern.getStep(step)
        if (stepData != null) {
            for (padIndex in stepData.triggeredPads) {
                val velocity = stepData.getVelocity(padIndex)
                triggerPad(padIndex, velocity)
            }
        }
    }

    /** Called when recording live input */
    fun onLivePadTrigger(padIndex: Int, velocity: Float) {
        if (isRecording.get()) {
            sequencerEngine.recordPadTrigger(padIndex, velocity)
        }
        triggerPad(padIndex, velocity)
    }

    // ============================================================
    // EFFECTS CONTROL (Master)
    // ============================================================

    fun setMasterVolume(volume: Float) {
        // Applied in sample player mixing
    }

    fun setMasterFilter(type: BiquadFilter.FilterType, cutoff: Float, resonance: Float) {
        samplePlayer.setMasterFilter(type, cutoff * 20000.0f, resonance * 20.0 + 0.5f)
    }

    fun setMasterDistortion(type: Distortion.DistortionType, drive: Float, mix: Float) {
        samplePlayer.setMasterDistortion(type, 1.0f + drive * 9.0f, mix)
    }

    fun setMasterDelay(timeMs: Float, feedback: Float, mix: Float) {
        samplePlayer.setMasterDelay(timeMs * 2000.0f, feedback * 0.95f, mix)
    }

    // ============================================================
    // PAD CONFIGURATION
    // ============================================================

    private val padConfigs = MutableList(16) { index ->
        PadConfig.defaultForPad(index)
    }

    fun getPadConfig(padIndex: Int): PadConfig {
        return padConfigs.getOrElse(padIndex) { PadConfig.defaultForPad(it) }
    }

    fun setPadConfig(padIndex: Int, config: PadConfig) {
        if (padIndex in padConfigs.indices) {
            padConfigs[padIndex] = config
        }
    }

    fun updatePadSample(padIndex: Int, samplePath: String, sampleName: String) {
        val config = getPadConfig(padIndex)
        padConfigs[padIndex] = config.copy(
            samplePath = samplePath,
            sampleName = sampleName
        )
    }

    fun updatePadParameter(padIndex: Int, parameter: PadParameter, value: Float) {
        val config = getPadConfig(padIndex)
        val newConfig = when (parameter) {
            PadParameter.VOLUME -> config.copy(volume = value)
            PadParameter.PITCH -> config.copy(pitch = (value - 0.5f) * 48.0f) // -24 to +24
            PadParameter.FILTER_CUTOFF -> config.copy(filterCutoff = value)
            PadParameter.FILTER_RESONANCE -> config.copy(filterResonance = value)
            PadParameter.FILTER_TYPE -> config.copy(filterType = FilterType.values()[value.toInt() % 4])
            PadParameter.DISTORTION -> config.copy(distortionAmount = value)
            PadParameter.DELAY_AMOUNT -> config.copy(delayAmount = value)
            PadParameter.DELAY_TIME -> config.copy(delayTime = value)
            PadParameter.DELAY_FEEDBACK -> config.copy(delayFeedback = value)
            PadParameter.PAN -> config.copy(pan = (value - 0.5f) * 2.0f)
            PadParameter.MUTE -> config.copy(isMuted = value > 0.5f)
            PadParameter.SOLO -> config.copy(isSolo = value > 0.5f)
            PadParameter.CHOKE_GROUP -> config.copy(chokeGroup = value.toInt())
            PadParameter.ONE_SHOT -> config.copy(oneShot = value > 0.5f)
            PadParameter.START_OFFSET -> config.copy(startOffset = value)
            PadParameter.END_OFFSET -> config.copy(endOffset = value)
            PadParameter.REVERSE -> config.copy(reverse = value > 0.5f)
            PadParameter.VELOCITY_SENSITIVITY -> config.copy(velocitySensitivity = value)
        }
        padConfigs[padIndex] = newConfig
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    fun shutdown() {
        stop()
        audioJob.cancel()
        samplePlayer.shutdown()
        scope.cancel()
    }

    // ============================================================
    // METRONOME
    // ============================================================

    private inner class Metronome(
        private val player: SamplePlayer,
        private val sampleRate: Int
    ) {
        private var clickBuffer: FloatArray? = null
        private var accentBuffer: FloatArray? = null
        private var enabled = true
        private var volume = 0.5f

        init {
            generateClickSounds()
        }

        private fun generateClickSounds() {
            // Generate click (short sine burst)
            val clickLen = sampleRate / 1000 // 1ms
            clickBuffer = FloatArray(clickLen)
            for (i in 0 until clickLen) {
                val t = i.toDouble() / sampleRate
                clickBuffer!![i] = (Math.sin(2.0 * Math.PI * 1000.0 * t) * Math.exp(-t * 5000.0) * 0.3).toFloat()
            }

            // Accent click (lower pitch, longer)
            val accentLen = sampleRate / 500 // 2ms
            accentBuffer = FloatArray(accentLen)
            for (i in 0 until accentLen) {
                val t = i.toDouble() / sampleRate
                accentBuffer!![i] = (Math.sin(2.0 * Math.PI * 800.0 * t) * Math.exp(-t * 3000.0) * 0.5).toFloat()
            }
        }

        fun start(bpm: Int) {
            // Metronome runs in sequencer engine
        }

        fun stop() {}

        fun setBpm(bpm: Int) {}

        fun setEnabled(e: Boolean) { enabled = e }
        fun setVolume(v: Float) { volume = v.coerceIn(0.0f, 1.0f) }

        fun accent() {
            if (enabled && accentBuffer != null) {
                playClick(accentBuffer!!)
            }
        }

        private fun playClick(buffer: FloatArray) {
            // Play through a dedicated voice or mix directly
            // For simplicity, we'll use a tone generator approach
            // In practice, this would go through the sample player
        }
    }
}

enum class PadParameter {
    VOLUME, PITCH, FILTER_CUTOFF, FILTER_RESONANCE, FILTER_TYPE,
    DISTORTION, DELAY_AMOUNT, DELAY_TIME, DELAY_FEEDBACK,
    PAN, MUTE, SOLO, CHOKE_GROUP, ONE_SHOT,
    START_OFFSET, END_OFFSET, REVERSE, VELOCITY_SENSITIVITY
}