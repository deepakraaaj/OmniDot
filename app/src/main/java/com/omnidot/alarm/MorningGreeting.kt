package com.omnidot.alarm

import com.omnidot.goals.Goal
import com.omnidot.goals.GoalDays
import java.time.LocalDate

object MorningGreeting {
    private const val CLOSING = "Take a slow breath. Today is yours."

    fun text(goals: List<Goal>, today: LocalDate): String {
        val todays = goals.filter { it.activeOn(today) }.sortedByDescending { it.tier.rank }
        if (todays.isEmpty()) return "Good morning. $CLOSING"
        val lines = todays.joinToString(" ") { goal ->
            val streak = GoalDays.currentStreak(goal, today)
            if (streak > 0) "${goal.title}, day ${streak + 1}." else "${goal.title}."
        }
        return "Good morning. Today: $lines $CLOSING"
    }
}
