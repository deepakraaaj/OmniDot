package com.omnidot.goals.reminder

import com.omnidot.goals.Goal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderSchedule {
    fun delayUntilNext(now: ZonedDateTime, minuteOfDay: Int): Duration {
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var next = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!next.isAfter(now)) next = now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        return Duration.between(now, next)
    }

    fun pendingToday(goals: List<Goal>, today: LocalDate): List<Goal> =
        goals.filter { it.activeOn(today) && today !in it.checkIns }
}
