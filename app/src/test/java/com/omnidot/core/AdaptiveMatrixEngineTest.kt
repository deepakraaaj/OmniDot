package com.omnidot.core

import com.omnidot.data.OmniDotPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AdaptiveMatrixEngineTest {
    private val engine = AdaptiveMatrixEngine()

    @Test
    fun `year progress handles leap years`() {
        val state = engine.stateFor(
            OmniDotPreferences(preset = MatrixPreset.YEAR_PROGRESS),
            LocalDate.of(2024, 12, 31),
        )
        assertEquals(366, state.total)
        assertEquals(366, state.completed)
        assertEquals(18, state.rows)
        assertEquals(365, state.currentIndex)
    }

    @Test
    fun `life weeks is capped at 4160`() {
        val state = engine.stateFor(
            OmniDotPreferences(
                preset = MatrixPreset.LIFE_IN_WEEKS,
                birthDate = LocalDate.of(1900, 1, 1),
            ),
            LocalDate.of(2026, 1, 1),
        )
        assertEquals(4_160, state.total)
        assertEquals(4_160, state.completed)
        assertEquals(null, state.currentIndex)
    }

    @Test
    fun `life matrix marks the current week separately from completed weeks`() {
        val state = engine.stateFor(
            OmniDotPreferences(
                preset = MatrixPreset.LIFE_IN_WEEKS,
                birthDate = LocalDate.of(2000, 1, 1),
            ),
            LocalDate.of(2000, 1, 3),
        )
        assertEquals(0, state.completed)
        assertEquals(0, state.currentIndex)
    }
}
