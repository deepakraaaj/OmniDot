package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalDaysTest {
    private val today = LocalDate.of(2026, 10, 12)
    private fun goal(
        tier: GoalTier,
        start: LocalDate = LocalDate.of(2026, 10, 1),
        end: LocalDate? = null,
        checks: Set<LocalDate> = emptySet(),
        archived: Boolean = false,
    ) = Goal("id-$tier-$start", "g", tier.defaultEmoji, tier, start, end, checks, archived)

    @Test
    fun `achieved shows the highest tier checked that day`() {
        val d = LocalDate.of(2026, 10, 5)
        val goals = listOf(goal(GoalTier.SMALL, checks = setOf(d)), goal(GoalTier.BIG, checks = setOf(d)), goal(GoalTier.MEDIUM))
        assertEquals(DayState.Achieved(GoalTier.BIG), GoalDays.dayState(d, goals, today))
    }

    @Test
    fun `missed today-pending no-goal and future states`() {
        val goals = listOf(goal(GoalTier.SMALL))
        assertEquals(DayState.Missed, GoalDays.dayState(LocalDate.of(2026, 10, 3), goals, today))
        assertEquals(DayState.TodayPending, GoalDays.dayState(today, goals, today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 9, 30), goals, today))
        assertEquals(DayState.Future, GoalDays.dayState(today.plusDays(1), goals, today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(today, emptyList(), today))
    }

    @Test
    fun `archived goals and check-ins outside the date range never count`() {
        val outside = LocalDate.of(2026, 9, 28)
        val shifted = goal(GoalTier.BIG, start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 10), checks = setOf(outside, LocalDate.of(2026, 10, 11)))
        assertEquals(DayState.NoGoal, GoalDays.dayState(outside, listOf(shifted), today))
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 10, 11), listOf(shifted), today))
        val archived = goal(GoalTier.BIG, checks = setOf(LocalDate.of(2026, 10, 5)), archived = true)
        assertEquals(DayState.NoGoal, GoalDays.dayState(LocalDate.of(2026, 10, 5), listOf(archived), today))
    }

    @Test
    fun `toggle only works inside range and not in the future`() {
        val g = goal(GoalTier.SMALL)
        assertTrue(g.toggle(today, today).checkIns.contains(today))
        assertFalse(g.toggle(today, today).toggle(today, today).checkIns.contains(today))
        assertEquals(g, g.toggle(today.plusDays(1), today))
        assertEquals(g, g.toggle(LocalDate.of(2026, 9, 1), today))
        assertTrue(g.withCheckIn(today, true, today).withCheckIn(today, true, today).checkIns == setOf(today))
    }

    @Test
    fun `streak counts back from today or yesterday`() {
        val checks = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(2, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = checks), today))
        assertEquals(3, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = checks + today), today))
        assertEquals(0, GoalDays.currentStreak(goal(GoalTier.SMALL, checks = setOf(today.minusDays(3))), today))
    }

    @Test
    fun `progress counts check-ins over elapsed active days`() {
        val g = goal(GoalTier.SMALL, start = LocalDate.of(2026, 10, 3), checks = setOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 9, 1)))
        assertEquals(2 to 10, GoalDays.progress(g, today))
        val notStarted = goal(GoalTier.SMALL, start = today.plusDays(3))
        assertEquals(0 to 0, GoalDays.progress(notStarted, today))
    }
}
