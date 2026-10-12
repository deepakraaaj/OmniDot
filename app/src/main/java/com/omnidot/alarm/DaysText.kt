package com.omnidot.alarm

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object DaysText {
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun describe(days: Set<DayOfWeek>): String = when (days) {
        emptySet<DayOfWeek>() -> "Once"
        DayOfWeek.entries.toSet() -> "Every day"
        weekdays -> "Weekdays"
        weekend -> "Weekends"
        else -> days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    }

    fun parseTime(text: String): Int? {
        val match = Regex("""([01]\d|2[0-3]):([0-5]\d)""").matchEntire(text.trim()) ?: return null
        return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
    }

    fun formatTime(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
}
