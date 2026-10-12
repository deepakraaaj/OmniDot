package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalListOpsTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val a = Goal("a", "A", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, emptySet())
    private val b = a.copy(id = "b", title = "B")

    @Test
    fun `upsert replaces in place and appends new`() {
        val updated = GoalListOps.upsert(listOf(a, b), a.copy(title = "A2"))
        assertEquals(listOf("A2", "B"), updated.map { it.title })
        assertEquals(3, GoalListOps.upsert(updated, a.copy(id = "c")).size)
    }

    @Test
    fun `check-in for a missing or archived goal is a no-op`() {
        val list = listOf(a, b.copy(archived = true))
        assertEquals(list, GoalListOps.setCheckIn(list, "gone", today, true, today))
        assertEquals(list, GoalListOps.setCheckIn(list, "b", today, true, today))
    }

    @Test
    fun `set check-in and toggle update only the target goal`() {
        val done = GoalListOps.setCheckIn(listOf(a, b), "a", today, true, today)
        assertTrue(today in done[0].checkIns)
        assertTrue(done[1].checkIns.isEmpty())
        assertTrue(GoalListOps.toggle(done, "a", today, today)[0].checkIns.isEmpty())
        assertEquals(listOf(b), GoalListOps.remove(done, "a"))
    }
}
