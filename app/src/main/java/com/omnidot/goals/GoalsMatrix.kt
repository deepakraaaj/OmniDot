package com.omnidot.goals

import com.omnidot.core.LockPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil

data class GoalsMatrixState(
    val cells: List<LocalDate?>,
    val columns: Int,
    val rows: Int,
    val title: String,
    val detail: String,
)

object GoalsMatrix {
    fun build(goals: List<Goal>, period: LockPeriod, today: LocalDate): GoalsMatrixState {
        val (cells, columns, title) = when (period) {
            LockPeriod.THIS_MONTH -> {
                val first = today.withDayOfMonth(1)
                val lead = first.dayOfWeek.value - 1
                val days = (0 until today.lengthOfMonth()).map { first.plusDays(it.toLong()) }
                Triple(List<LocalDate?>(lead) { null } + days, 7,
                    today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            }
            LockPeriod.LAST_12_WEEKS -> {
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val start = end.minusDays(83)
                Triple((0 until 84).map { start.plusDays(it.toLong()) }, 7, period.label)
            }
            LockPeriod.THIS_YEAR -> {
                val first = today.withDayOfYear(1)
                Triple((0 until today.lengthOfYear()).map { first.plusDays(it.toLong()) }, 21, today.year.toString())
            }
        }
        val rows = ceil(cells.size / columns.toFloat()).toInt()
        val visibleGoals = goals.filterNot { it.archived }
        val detail = if (visibleGoals.isEmpty()) {
            "Add a goal to start"
        } else {
            // Only days with at least one active goal count; days before any goal existed are not "missed".
            val states = cells.filterNotNull().filter { !it.isAfter(today) }
                .map { GoalDays.dayState(it, visibleGoals, today) }
                .filter { it != DayState.NoGoal }
            "${states.count { it is DayState.Achieved }} of ${states.size} days showed up"
        }
        return GoalsMatrixState(cells, columns, rows, title, detail)
    }
}
