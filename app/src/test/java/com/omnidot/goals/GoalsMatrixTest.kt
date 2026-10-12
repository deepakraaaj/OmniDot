package com.omnidot.goals

import com.omnidot.core.LockPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GoalsMatrixTest {
    @Test
    fun `month starting on sunday needs six monday-first rows`() {
        // 2026-03-01 is a Sunday, March has 31 days
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, LocalDate.of(2026, 3, 15))
        assertEquals(7, m.columns)
        assertEquals(6, m.rows)
        assertEquals(6 + 31, m.cells.size)
        (0 until 6).forEach { assertNull(m.cells[it]) }
        assertEquals(LocalDate.of(2026, 3, 1), m.cells[6])
        assertEquals("March", m.title)
    }

    @Test
    fun `leap february starting on thursday`() {
        // 2024-02-01 is a Thursday, 29 days
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, LocalDate.of(2024, 2, 10))
        assertEquals(3 + 29, m.cells.size)
        assertEquals(5, m.rows)
        assertEquals(LocalDate.of(2024, 2, 29), m.cells.last())
    }

    @Test
    fun `last 12 weeks ends on the sunday of this week`() {
        val today = LocalDate.of(2026, 10, 14) // Wednesday
        val m = GoalsMatrix.build(emptyList(), LockPeriod.LAST_12_WEEKS, today)
        assertEquals(84, m.cells.size)
        assertEquals(12, m.rows)
        assertEquals(LocalDate.of(2026, 10, 18), m.cells.last())
        assertEquals(LocalDate.of(2026, 7, 27), m.cells.first())
        assertEquals("Last 12 weeks", m.title)
    }

    @Test
    fun `this year uses 21 columns`() {
        val m = GoalsMatrix.build(emptyList(), LockPeriod.THIS_YEAR, LocalDate.of(2024, 6, 1))
        assertEquals(366, m.cells.size)
        assertEquals(21, m.columns)
        assertEquals(18, m.rows)
        assertEquals("2024", m.title)
    }

    @Test
    fun `detail counts achieved days so far and prompts when no goals`() {
        val today = LocalDate.of(2026, 10, 12)
        assertEquals("Add a goal to start", GoalsMatrix.build(emptyList(), LockPeriod.THIS_MONTH, today).detail)
        val g = Goal("a", "Run", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, setOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 12)))
        assertEquals("2 of 12 days showed up", GoalsMatrix.build(listOf(g), LockPeriod.THIS_MONTH, today).detail)
    }

    @Test
    fun `days before any goal existed are not counted as missed`() {
        val today = LocalDate.of(2026, 10, 12)
        val g = Goal("a", "Run", "🌱", GoalTier.SMALL, today, null, setOf(today))
        assertEquals("1 of 1 days showed up", GoalsMatrix.build(listOf(g), LockPeriod.THIS_MONTH, today).detail)
    }
}
