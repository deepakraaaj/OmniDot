package com.omnidot.core

enum class MatrixPreset(
    val title: String,
    val subtitle: String,
    val columns: Int,
) {
    GOALS("Goals", "Your biggest win each day", 7),
    YEAR_PROGRESS("Year in dots", "Every day of the current year", 21),
    LIFE_IN_WEEKS("Life in weeks", "4,160 weeks across 80 years", 52);

    companion object {
        private val legacyToYear = setOf("SPRINT_100", "CONTRIBUTION_HEATMAP")

        fun fromStored(value: String?): MatrixPreset = when {
            value in legacyToYear -> YEAR_PROGRESS
            else -> entries.firstOrNull { it.name == value } ?: GOALS
        }
    }
}
