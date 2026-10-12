package com.omnidot.goals

import com.omnidot.goals.reminder.ReminderSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `later today schedules today`() {
        val now = ZonedDateTime.of(2026, 10, 12, 18, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(3), ReminderSchedule.delayUntilNext(now, 21 * 60))
    }

    @Test
    fun `passed or exact time schedules tomorrow`() {
        val now = ZonedDateTime.of(2026, 10, 12, 21, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(24), ReminderSchedule.delayUntilNext(now, 21 * 60))
        val late = ZonedDateTime.of(2026, 10, 12, 22, 30, 0, 0, zone)
        assertEquals(Duration.ofMinutes(22 * 60 + 30), ReminderSchedule.delayUntilNext(late, 21 * 60))
    }

    @Test
    fun `dst spring forward day is 23 hours long`() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 21, 0, 0, 0, ny) // DST starts 2026-03-08
        assertEquals(Duration.ofHours(23), ReminderSchedule.delayUntilNext(now, 21 * 60))
    }

    @Test
    fun `pending lists active unchecked goals only`() {
        val today = LocalDate.of(2026, 10, 12)
        val base = Goal("a", "A", "🌱", GoalTier.SMALL, LocalDate.of(2026, 10, 1), null, emptySet())
        val goals = listOf(
            base,
            base.copy(id = "done", checkIns = setOf(today)),
            base.copy(id = "future", startDate = today.plusDays(1)),
            base.copy(id = "arch", archived = true),
        )
        assertEquals(listOf("a"), ReminderSchedule.pendingToday(goals, today).map { it.id })
    }
}
