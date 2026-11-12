package com.hardtekk.electribe.sequencer

import android.util.Log
import com.hardtekk.electribe.audio.AudioEngine
import com.hardtekk.electribe.model.Pattern
import com.hardtekk.electribe.model.QuantizeMode
import com.hardtekk.electribe.model.StepData
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInt

/**
 * Step Sequencer Engine - runs on audio thread for precise timing
 * Handles pattern playback, live recording, quantization, swing
 */
class SequencerEngine(
    private val audioEngine: AudioEngine,
    private val sampleRate: Int = 44100
) {
    // Patterns (4 patterns, each with 16/32 steps)
    private val patterns = Array(4) { index ->
        Pattern.createEmpty(index, 16, 140)
    }

    // Sequencer state
    private val isRunning = AtomicBoolean(false)
    private val currentPatternIndex = AtomicInt(0)
    private val currentStep = AtomicInt(0)
    private val currentTick = AtomicInt(0)

    // Timing
    private var bpm = 140
    private var stepsPerBeat = 4 // 16th notes
    private var samplesPerStep: Long = calculateSamplesPerStep()
    private var swing = 0.0f // -1.0 to 1.0
    private var patternLength = 16

    // Live recording
    private val isRecording = AtomicBoolean(false)
    private val quantizeMode = QuantizeMode.SIXTEENTH
    private val pendingTriggers = MutableList<PendingTrigger>()

    // High-resolution timing
    private var nextStepTimeNs = 0L
    private val stepCallback: (Int, Pattern) -> Unit

    // Coroutine for sequencer loop
    private val sequencerJob = Job()
    private val sequencerScope = CoroutineScope(sequencerJob + Dispatchers.Default)

    init {
        stepCallback = { step, pattern ->
            audioEngine.onSequencerStep(step, pattern)
        }
    }

    private fun calculateSamplesPerStep(): Long {
        val samplesPerBeat = (60.0 * sampleRate / bpm).toLong()
        return samplesPerBeat / stepsPerBeat
    }

    // ============================================================
    // PUBLIC API
    // ============================================================

    fun start() {
        if (!isRunning.getAndSet(true)) {
            currentStep.set(0)
            currentTick.set(0)
            nextStepTimeNs = System.nanoTime() + samplesPerStep * 1_000_000 / (sampleRate / 1000)
            sequencerScope.launch { sequencerLoop() }
        }
    }

    fun stop() {
        if (isRunning.getAndSet(false)) {
            sequencerJob.cancel()
            sequencerJob = Job()
            sequencerScope = CoroutineScope(sequencerJob + Dispatchers.Default)
        }
    }

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(120, 220)
        samplesPerStep = calculateSamplesPerStep()
    }

    fun setSwing(s: Float) {
        swing = s.coerceIn(-0.5f, 0.5f)
    }

    fun setPatternLength(length: Int) {
        patternLength = length.coerceIn(16, 32)
        stepsPerBeat = patternLength / 4
        samplesPerStep = calculateSamplesPerStep()

        // Resize all patterns
        for (i in patterns.indices) {
            patterns[i] = patterns[i].copyWith(steps = patternLength)
        }
    }

    fun setPattern(index: Int) {
        currentPatternIndex.set(index.coerceIn(0, 3))
    }

    fun getCurrentPattern(): Pattern = patterns[currentPatternIndex.get()]

    fun getPattern(index: Int): Pattern = patterns[index.coerceIn(0, 3)]

    fun setPatternPattern(index: Int, pattern: Pattern) {
        patterns[index.coerceIn(0, 3)] = pattern
    }

    fun copyPattern(from: Int, to: Int) {
        patterns[to.coerceIn(0, 3)] = patterns[from.coerceIn(0, 3)].copy(
            patternIndex = to,
            lastModified = System.currentTimeMillis()
        )
    }

    fun clearCurrentPattern() {
        val idx = currentPatternIndex.get()
        patterns[idx] = Pattern.createEmpty(idx, patternLength, bpm)
    }

    // ============================================================
    // LIVE RECORDING
    // ============================================================

    fun setRecording(recording: Boolean) {
        isRecording.set(recording)
        if (recording) {
            pendingTriggers.clear()
        }
    }

    fun isRecording(): Boolean = isRecording.get()

    fun recordPadTrigger(padIndex: Int, velocity: Float) {
        if (!isRecording.get()) return

        val now = System.nanoTime()
        val stepTime = nextStepTimeNs - samplesPerStep * 1_000_000 / (sampleRate / 1000)
        val timeSinceStep = now - stepTime

        // Quantize to nearest step
        val quantizedStep = quantizeToStep(timeSinceStep)
        val targetStep = (currentStep.get() + quantizedStep) % patternLength

        pendingTriggers.add(PendingTrigger(
            step = targetStep,
            padIndex = padIndex,
            velocity = velocity,
            timestamp = now
        ))
    }

    private fun quantizeToStep(timeSinceStepNs: Long): Int {
        val stepDurationNs = samplesPerStep * 1_000_000 / (sampleRate / 1000)
        val stepFraction = timeSinceStepNs.toDouble() / stepDurationNs

        return when (quantizeMode) {
            QuantizeMode.OFF -> Math.round(stepFraction).toInt()
            QuantizeMode.QUARTER -> Math.round(stepFraction * 4.0) / 4
            QuantizeMode.EIGHTH -> Math.round(stepFraction * 8.0) / 8
            QuantizeMode.SIXTEENTH -> Math.round(stepFraction * 16.0) / 16
            QuantizeMode.THIRTY_SECOND -> Math.round(stepFraction * 32.0) / 32
        }.toInt()
    }

    private fun applyPendingTriggers() {
        val pattern = getCurrentPattern()
        var newPattern = pattern

        for (trigger in pendingTriggers) {
            newPattern = newPattern.togglePadAtStep(trigger.step, trigger.padIndex, trigger.velocity)
        }

        patterns[currentPatternIndex.get()] = newPattern
        pendingTriggers.clear()
    }

    // ============================================================
    // SEQUENCER LOOP (runs on coroutine)
    // ============================================================

    private fun sequencerLoop() {
        val stepDurationNs = samplesPerStep * 1_000_000 / (sampleRate / 1000)

        while (isRunning.get()) {
            val now = System.nanoTime()

            // Check if it's time for next step
            if (now >= nextStepTimeNs) {
                // Apply swing to timing
                val swingOffset = if (currentStep.get() % 2 == 1) {
                    (stepDurationNs * swing * 0.5).toLong()
                } else {
                    0L
                }

                // Process current step
                val step = currentStep.get()
                val pattern = getCurrentPattern()
                stepCallback(step, pattern)

                // Advance step
                val nextStep = (step + 1) % patternLength
                currentStep.set(nextStep)
                currentTick.incrementAndGet()

                // Schedule next step
                nextStepTimeNs += stepDurationNs + swingOffset

                // Apply any pending recorded triggers
                if (isRecording.get() && pendingTriggers.isNotEmpty()) {
                    applyPendingTriggers()
                }

                // Check for pattern change (if queued)
                // (pattern changes happen on next loop iteration)
            } else {
                // Sleep until next step
                val sleepNs = nextStepTimeNs - now
                if (sleepNs > 1_000_000) { // > 1ms
                    try {
                        Thread.sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt())
                    } catch (e: InterruptedException) {
                        break
                    }
                } else {
                    // Busy wait for sub-ms accuracy
                    Thread.onSpinWait()
                }
            }
        }
    }

    // ============================================================
    // STEP EDITING (for step sequencer mode)
    // ============================================================

    fun toggleStep(padIndex: Int, step: Int): Pattern {
        val pattern = getCurrentPattern()
        val newPattern = pattern.togglePadAtStep(step, padIndex, 1.0f)
        patterns[currentPatternIndex.get()] = newPattern
        return newPattern
    }

    fun setStepVelocity(padIndex: Int, step: Int, velocity: Float): Pattern {
        val pattern = getCurrentPattern()
        val stepData = pattern.getStep(step) ?: return pattern

        if (stepData.triggeredPads.contains(padIndex)) {
            val newVelocities = stepData.velocities.toMutableMap()
            newVelocities[padIndex] = velocity.coerceIn(0.0f, 1.0f)
            val newStepData = stepData.copy(velocities = newVelocities)
            val newPattern = pattern.withStep(step, newStepData)
            patterns[currentPatternIndex.get()] = newPattern
            return newPattern
        }
        return pattern
    }

    fun clearStep(step: Int): Pattern {
        val pattern = getCurrentPattern()
        val newPattern = pattern.clearStep(step)
        patterns[currentPatternIndex.get()] = newPattern
        return newPattern
    }

    fun clearAllSteps(): Pattern {
        val pattern = getCurrentPattern()
        val newPattern = pattern.clearAllSteps()
        patterns[currentPatternIndex.get()] = newPattern
        return newPattern
    }

    // ============================================================
    // PENDING TRIGGER DATA CLASS
    // ============================================================

    private data class PendingTrigger(
        val step: Int,
        val padIndex: Int,
        val velocity: Float,
        val timestamp: Long
    )
}