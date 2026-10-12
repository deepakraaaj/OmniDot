package com.omnidot.goals

import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface DayState {
    data class Achieved(val tier: GoalTier) : DayState
    data object Missed : DayState
    data object NoGoal : DayState
    data object TodayPending : DayState
    data object Future : DayState
}

object GoalDays {
    fun dayState(date: LocalDate, goals: List<Goal>, today: LocalDate): DayState {
        if (date.isAfter(today)) return DayState.Future
        val active = goals.filter { it.activeOn(date) }
        if (active.isEmpty()) return DayState.NoGoal
        val best = active.filter { date in it.checkIns }.maxByOrNull { it.tier.rank }
        return when {
            best != null -> DayState.Achieved(best.tier)
            date == today -> DayState.TodayPending
            else -> DayState.Missed
        }
    }

    fun currentStreak(goal: Goal, today: LocalDate): Int {
        var day = if (today in goal.checkIns) today else today.minusDays(1)
        var count = 0
        while (day in goal.checkIns && goal.activeOn(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** (achieved days, elapsed active days) up to and including today. */
    fun progress(goal: Goal, today: LocalDate): Pair<Int, Int> {
        if (today.isBefore(goal.startDate)) return 0 to 0
        val last = goal.endDate?.takeIf { it.isBefore(today) } ?: today
        val elapsed = ChronoUnit.DAYS.between(goal.startDate, last).toInt() + 1
        val achieved = goal.checkIns.count { !it.isBefore(goal.startDate) && !it.isAfter(last) }
        return achieved to elapsed
    }
}
