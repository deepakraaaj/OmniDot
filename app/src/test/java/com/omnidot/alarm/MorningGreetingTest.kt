package com.omnidot.alarm

import com.omnidot.goals.Goal
import com.omnidot.goals.GoalTier
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MorningGreetingTest {
    private val today = LocalDate.of(2026, 10, 12)

    @Test
    fun `greets without goals`() {
        assertEquals("Good morning. Take a slow breath. Today is yours.", MorningGreeting.text(emptyList(), today))
    }

    @Test
    fun `reads today's goals with streaks, biggest first`() {
        val run = Goal("r", "Run 5 km", "⛰️", GoalTier.BIG, today.minusDays(10), null, (1..5L).map { today.minusDays(it) }.toSet())
        val water = Goal("w", "Drink water", "🌱", GoalTier.SMALL, today.minusDays(3), null, emptySet())
        val future = Goal("f", "Later", "🌊", GoalTier.MEDIUM, today.plusDays(2), null, emptySet())
        assertEquals(
            "Good morning. Today: Run 5 km, day 6. Drink water. Take a slow breath. Today is yours.",
            MorningGreeting.text(listOf(water, run, future), today),
        )
    }
}
