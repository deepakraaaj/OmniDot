package com.omnidot.alarm

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

object AlarmSchedule {
    /** The next occurrence strictly after [now] on one of [days] (empty = any day). */
    fun nextTrigger(now: ZonedDateTime, minuteOfDay: Int, days: Set<DayOfWeek> = emptySet()): ZonedDateTime {
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var day = now.toLocalDate()
        repeat(8) {
            val candidate = day.atTime(time).atZone(now.zone)
            if (candidate.isAfter(now) && (days.isEmpty() || candidate.dayOfWeek in days)) return candidate
            day = day.plusDays(1)
        }
        return now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    fun ringsIn(now: ZonedDateTime, next: ZonedDateTime): String {
        val minutes = Duration.between(now, next).toMinutes()
        return when {
            minutes < 1 -> "Rings in less than a minute"
            minutes < 60 -> "Rings in $minutes min"
            minutes % 60 == 0L -> "Rings in ${minutes / 60} h"
            else -> "Rings in ${minutes / 60} h ${minutes % 60} min"
        }
    }
}
