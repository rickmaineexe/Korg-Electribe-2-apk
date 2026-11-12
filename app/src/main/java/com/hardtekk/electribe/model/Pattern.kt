package com.hardtekk.electribe.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class Pattern(
    val patternIndex: Int = 0,                    // 0-3 (4 patterns)
    val name: String = "Pattern ${patternIndex + 1}",
    val steps: Int = 16,                          // 16 or 32 steps
    val stepData: List<StepData> = emptyList(),   // Step sequence data
    val swing: Float = 0.0f,                      // -1.0 to 1.0
    val tempo: Int = 140,                         // BPM
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        fun createEmpty(patternIndex: Int, steps: Int = 16, tempo: Int = 140): Pattern {
            val stepData = MutableList(steps) { index ->
                StepData(stepIndex = index, triggeredPads = emptySet(), velocities = emptyMap())
            }
            return Pattern(
                patternIndex = patternIndex,
                name = "Pattern ${patternIndex + 1}",
                steps = steps,
                stepData = stepData,
                tempo = tempo
            )
        }
    }

    fun getStep(stepIndex: Int): StepData? {
        return stepData.getOrNull(stepIndex)
    }

    fun withStep(stepIndex: Int, stepData: StepData): Pattern {
        val newSteps = this.stepData.toMutableList()
        if (stepIndex < newSteps.size) {
            newSteps[stepIndex] = stepData
        }
        return copy(stepData = newSteps, lastModified = System.currentTimeMillis())
    }

    fun togglePadAtStep(stepIndex: Int, padIndex: Int, velocity: Float = 1.0f): Pattern {
        val step = getStep(stepIndex) ?: return this
        val newTriggered = step.triggeredPads.toMutableSet()
        val newVelocities = step.velocities.toMutableMap()

        if (newTriggered.contains(padIndex)) {
            newTriggered.remove(padIndex)
            newVelocities.remove(padIndex)
        } else {
            newTriggered.add(padIndex)
            newVelocities[padIndex] = velocity.coerceIn(0.0f, 1.0f)
        }

        return withStep(stepIndex, step.copy(
            triggeredPads = newTriggered,
            velocities = newVelocities
        ))
    }

    fun clearStep(stepIndex: Int): Pattern {
        val step = getStep(stepIndex) ?: return this
        return withStep(stepIndex, step.copy(triggeredPads = emptySet(), velocities = emptyMap()))
    }

    fun clearAllSteps(): Pattern {
        val newSteps = MutableList(steps) { index ->
            StepData(stepIndex = index, triggeredPads = emptySet(), velocities = emptyMap())
        }
        return copy(stepData = newSteps, lastModified = System.currentTimeMillis())
    }

    fun copyWith(steps: Int? = null, tempo: Int? = null, swing: Float? = null): Pattern {
        val newSteps = steps ?: this.steps
        let {
            if (it != this.steps) {
                val stepList = this.stepData.toMutableList()
                if (it > this.steps) {
                    // Add new steps
                    repeat(it - this.steps) { i ->
                        stepList.add(StepData(stepIndex = this.steps + i, triggeredPads = emptySet(), velocities = emptyMap()))
                    }
                } else if (it < this.steps) {
                    // Remove excess steps
                    while (stepList.size > it) {
                        stepList.removeAt(stepList.lastIndex)
                    }
                }
                copy(steps = it, stepData = stepList)
            } else {
                this
            }
        }?.let { pattern ->
            pattern.copy(
                tempo = tempo ?: pattern.tempo,
                swing = swing ?: pattern.swing,
                lastModified = System.currentTimeMillis()
            )
        } ?: this
    }
}

@Serializable
@Keep
data class StepData(
    val stepIndex: Int = 0,
    val triggeredPads: Set<Int> = emptySet(),     // Which pads are triggered on this step
    val velocities: Map<Int, Float> = emptyMap(), // Pad index -> velocity (0.0-1.0)
    val parameterLocks: Map<Int, ParameterLock> = emptyMap() // Pad -> parameter locks
) {
    fun isPadTriggered(padIndex: Int): Boolean = triggeredPads.contains(padIndex)
    fun getVelocity(padIndex: Int): Float = velocities[padIndex] ?: 1.0f
}

@Serializable
@Keep
data class ParameterLock(
    val filterCutoff: Float? = null,
    val filterResonance: Float? = null,
    val pitch: Float? = null,
    val volume: Float? = null,
    val distortion: Float? = null,
    val delay: Float? = null
)