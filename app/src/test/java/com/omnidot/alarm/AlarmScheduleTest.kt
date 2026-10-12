package com.omnidot.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(d: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 10, d, h, m, 0, 0, zone) // Oct 12 2026 = Monday

    @Test
    fun `one-time alarm rings today if still ahead, else tomorrow`() {
        val alarm = CalmAlarm("a", 6 * 60 + 30)
        assertEquals(at(12, 6, 30), AlarmSchedule.nextTrigger(at(12, 5, 0), alarm))
        assertEquals(at(13, 6, 30), AlarmSchedule.nextTrigger(at(12, 6, 30), alarm))
    }

    @Test
    fun `weekday alarm skips the weekend`() {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val alarm = CalmAlarm("a", 7 * 60, days = weekdays)
        // Friday Oct 16 after 7:00 → Monday Oct 19
        assertEquals(at(19, 7, 0), AlarmSchedule.nextTrigger(at(16, 8, 0), alarm))
        assertEquals(at(13, 7, 0), AlarmSchedule.nextTrigger(at(12, 9, 0), alarm))
    }

    @Test
    fun `rings in text is friendly`() {
        assertEquals("Rings in 1 h 30 min", AlarmSchedule.ringsIn(at(12, 5, 0), at(12, 6, 30)))
        assertEquals("Rings in 45 min", AlarmSchedule.ringsIn(at(12, 6, 0), at(12, 6, 45)))
        assertEquals("Rings in less than a minute", AlarmSchedule.ringsIn(at(12, 6, 0), at(12, 6, 0).plusSeconds(20)))
    }
}
