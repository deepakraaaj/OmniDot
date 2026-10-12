package com.omnidot.core

import com.omnidot.automation.VoiceStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixPresetTest {
    @Test
    fun `removed presets migrate to year progress`() {
        assertEquals(MatrixPreset.YEAR_PROGRESS, MatrixPreset.fromStored("SPRINT_100"))
        assertEquals(MatrixPreset.YEAR_PROGRESS, MatrixPreset.fromStored("CONTRIBUTION_HEATMAP"))
    }

    @Test
    fun `unknown or missing preset defaults to goals`() {
        assertEquals(MatrixPreset.GOALS, MatrixPreset.fromStored(null))
        assertEquals(MatrixPreset.GOALS, MatrixPreset.fromStored("NOPE"))
        assertEquals(MatrixPreset.LIFE_IN_WEEKS, MatrixPreset.fromStored("LIFE_IN_WEEKS"))
    }

    @Test
    fun `lock period and voice style fall back safely`() {
        assertEquals(LockPeriod.THIS_MONTH, LockPeriod.fromStored(null))
        assertEquals(LockPeriod.THIS_YEAR, LockPeriod.fromStored("THIS_YEAR"))
        assertEquals(VoiceStyle.CALM, VoiceStyle.fromStored("bogus"))
    }

    @Test
    fun `calm voice matches spec values`() {
        assertEquals(0.9f, VoiceStyle.CALM.rate)
        assertEquals(1.0f, VoiceStyle.CALM.pitch)
        assertEquals(0.8f, VoiceStyle.CALM.volume)
    }
}
