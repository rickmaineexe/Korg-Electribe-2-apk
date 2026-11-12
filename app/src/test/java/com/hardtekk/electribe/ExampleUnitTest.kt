package com.hardtekk.electribe

import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](https://developer.android.com/training/testing/unit-testing/local-unit-tests).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPadConfigDefaults() {
        val config = com.hardtekk.electribe.model.PadConfig.defaultForPad(0)
        assertEquals(0, config.padIndex)
        assertEquals(1.0f, config.volume, 0.001f)
        assertEquals(0.0f, config.pitch, 0.001f)
        assertEquals(1.0f, config.filterCutoff, 0.001f)
        assertEquals(0.0f, config.filterResonance, 0.001f)
        assertFalse(config.isMuted)
        assertFalse(config.isSolo)
        assertEquals(0, config.chokeGroup)
        assertTrue(config.oneShot)
    }

    @Test
    fun testPatternCreation() {
        val pattern = com.hardtekk.electribe.model.Pattern.createEmpty(0, 16, 140)
        assertEquals(0, pattern.patternIndex)
        assertEquals(16, pattern.steps)
        assertEquals(140, pattern.tempo)
        assertEquals(16, pattern.stepData.size)
    }

    @Test
    fun testStepToggle() {
        var pattern = com.hardtekk.electribe.model.Pattern.createEmpty(0, 16, 140)
        pattern = pattern.togglePadAtStep(0, 0, 1.0f)
        val step = pattern.getStep(0)
        assertNotNull(step)
        assertTrue(step!!.triggeredPads.contains(0))
        assertEquals(1.0f, step.getVelocity(0), 0.001f)

        // Toggle off
        pattern = pattern.togglePadAtStep(0, 0, 1.0f)
        val step2 = pattern.getStep(0)
        assertFalse(step2!!.triggeredPads.contains(0))
    }

    @Test
    fun testBpmClamping() {
        val state = com.hardtekk.electribe.model.AppState()
        val highState = state.withBpm(300)
        assertEquals(com.hardtekk.electribe.model.AppState.MAX_BPM, highState.currentBpm)

        val lowState = state.withBpm(60)
        assertEquals(com.hardtekk.electribe.model.AppState.MIN_BPM, lowState.currentBpm)
    }

    @Test
    fun testPitchRatio() {
        val ratio1 = com.hardtekk.electribe.audio.DSP.PitchShifter.semitonesToRatio(12.0f)
        assertEquals(2.0f, ratio1, 0.001f)

        val ratio2 = com.hardtekk.electribe.audio.DSP.PitchShifter.semitonesToRatio(-12.0f)
        assertEquals(0.5f, ratio2, 0.001f)

        val semitones = com.hardtekk.electribe.audio.DSP.PitchShifter.ratioToSemitones(2.0f)
        assertEquals(12.0f, semitones, 0.001f)
    }

    @Test
    fun testBiquadFilterCoefficients() {
        val filter = com.hardtekk.electribe.audio.DSP.BiquadFilter()
        filter.setSampleRate(44100.0)
        filter.setFrequency(1000.0)
        filter.setResonance(1.0)
        filter.setType(com.hardtekk.electribe.audio.DSP.BiquadFilter.FilterType.LOW_PASS)

        // Process a few samples
        var output = filter.process(1.0)
        assertTrue(output.absoluteValue <= 1.0)

        output = filter.process(0.0)
        assertTrue(output.absoluteValue <= 1.0)
    }

    @Test
    fun testDistortion() {
        val distortion = com.hardtekk.electribe.audio.DSP.Distortion()
        distortion.setSampleRate(44100.0)
        distortion.setDrive(2.0f)
        distortion.setMix(1.0f)

        val output = distortion.process(0.5f)
        assertTrue(output.absoluteValue <= 1.0)

        // Test hard clip
        distortion.type = com.hardtekk.electribe.audio.DSP.Distortion.DistortionType.HARD_CLIP
        val clipped = distortion.process(2.0f)
        assertEquals(1.0f, clipped, 0.001f)
    }

    @Test
    fun testDelay() {
        val delay = com.hardtekk.electribe.audio.DSP.Delay()
        delay.setSampleRate(44100.0)
        delay.setDelayTimeMs(250.0f)
        delay.setFeedback(0.3f)
        delay.setMix(0.5f)

        val (outL, outR) = delay.process(1.0f, 0.0f)
        assertTrue(outL.absoluteValue <= 1.0)
        assertTrue(outR.absoluteValue <= 1.0)
    }
}