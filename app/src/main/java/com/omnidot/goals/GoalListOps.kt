package com.omnidot.goals

import java.time.LocalDate

object GoalListOps {
    fun upsert(list: List<Goal>, goal: Goal): List<Goal> =
        if (list.any { it.id == goal.id }) list.map { if (it.id == goal.id) goal else it } else list + goal

    fun remove(list: List<Goal>, id: String): List<Goal> = list.filterNot { it.id == id }

    fun setCheckIn(list: List<Goal>, id: String, date: LocalDate, done: Boolean, today: LocalDate): List<Goal> =
        list.map { if (it.id == id) it.withCheckIn(date, done, today) else it }

    fun toggle(list: List<Goal>, id: String, date: LocalDate, today: LocalDate): List<Goal> =
        list.map { if (it.id == id) it.toggle(date, today) else it }
}
