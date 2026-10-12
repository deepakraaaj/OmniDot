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
    }

    @Test
    fun `sprint is zero before start and inclusive on first day`() {
        val settings = OmniDotPreferences(
            preset = MatrixPreset.SPRINT_100,
            sprintStart = LocalDate.of(2026, 4, 20),
        )
        assertEquals(0, engine.stateFor(settings, LocalDate.of(2026, 4, 19)).completed)
        assertEquals(1, engine.stateFor(settings, LocalDate.of(2026, 4, 20)).completed)
        assertEquals(100, engine.stateFor(settings, LocalDate.of(2027, 1, 1)).completed)
    }

    @Test
    fun `contribution matrix is exactly 52 by 7`() {
        val state = engine.stateFor(
            OmniDotPreferences(preset = MatrixPreset.CONTRIBUTION_HEATMAP),
            LocalDate.of(2026, 10, 12),
        )
        assertEquals(52, state.columns)
        assertEquals(7, state.rows)
        assertEquals(364, state.total)
    }
}

