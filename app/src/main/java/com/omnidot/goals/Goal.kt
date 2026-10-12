package com.omnidot.goals

import java.time.LocalDate

enum class GoalTier(val rank: Int, val label: String, val defaultEmoji: String) {
    SMALL(1, "Small", "🌱"),
    MEDIUM(2, "Medium", "🌊"),
    BIG(3, "Big", "⛰️");

    companion object {
        fun fromStored(value: String?): GoalTier = entries.firstOrNull { it.name == value } ?: SMALL
    }
}

data class Goal(
    val id: String,
    val title: String,
    val emoji: String,
    val tier: GoalTier,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val checkIns: Set<LocalDate>,
    val archived: Boolean = false,
) {
    fun activeOn(date: LocalDate): Boolean =
        !archived && !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))

    fun canCheckIn(date: LocalDate, today: LocalDate): Boolean = activeOn(date) && !date.isAfter(today)

    fun toggle(date: LocalDate, today: LocalDate): Goal =
        withCheckIn(date, date !in checkIns, today)

    fun withCheckIn(date: LocalDate, done: Boolean, today: LocalDate): Goal {
        if (!canCheckIn(date, today)) return this
        return copy(checkIns = if (done) checkIns + date else checkIns - date)
    }
}
