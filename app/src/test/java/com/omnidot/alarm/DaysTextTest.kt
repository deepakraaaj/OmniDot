package com.omnidot.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

class DaysTextTest {
    @Test
    fun `describes common day sets in plain words`() {
        assertEquals("Once", DaysText.describe(emptySet()))
        assertEquals("Every day", DaysText.describe(java.time.DayOfWeek.entries.toSet()))
        assertEquals("Weekdays", DaysText.describe(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)))
        assertEquals("Weekends", DaysText.describe(setOf(SATURDAY, SUNDAY)))
        assertEquals("Mon, Wed", DaysText.describe(setOf(WEDNESDAY, MONDAY)))
    }

    @Test
    fun `parses and formats clock times`() {
        assertEquals(390, DaysText.parseTime("06:30"))
        assertEquals(null, DaysText.parseTime("25:00"))
        assertEquals("06:30", DaysText.formatTime(390))
    }
}
