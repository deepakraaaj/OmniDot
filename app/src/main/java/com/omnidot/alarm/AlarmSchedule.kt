package com.omnidot.alarm

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

object AlarmSchedule {
    /** The next ring strictly after [now], honouring the alarm's weekdays (none = once). */
    fun nextTrigger(now: ZonedDateTime, alarm: CalmAlarm): ZonedDateTime {
        val time = LocalTime.of(alarm.minuteOfDay / 60, alarm.minuteOfDay % 60)
        var day = now.toLocalDate()
        repeat(8) {
            val candidate = day.atTime(time).atZone(now.zone)
            if (candidate.isAfter(now) && (alarm.days.isEmpty() || candidate.dayOfWeek in alarm.days)) return candidate
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
