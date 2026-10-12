package com.omnidot.core

enum class MatrixPreset(
    val title: String,
    val subtitle: String,
    val columns: Int,
    val fixedRows: Int?,
) {
    YEAR_PROGRESS(
        title = "Year in dots",
        subtitle = "Every day of the current year",
        columns = 21,
        fixedRows = null,
    ),
    LIFE_IN_WEEKS(
        title = "Life in weeks",
        subtitle = "4,160 weeks across 80 years",
        columns = 52,
        fixedRows = 80,
    ),
    SPRINT_100(
        title = "100-day sprint",
        subtitle = "A focused hundred-day horizon",
        columns = 10,
        fixedRows = 10,
    ),
    CONTRIBUTION_HEATMAP(
        title = "Contribution field",
        subtitle = "52 weeks × 7 days, entirely local",
        columns = 52,
        fixedRows = 7,
    );

    companion object {
        fun fromStored(value: String?): MatrixPreset =
            entries.firstOrNull { it.name == value } ?: YEAR_PROGRESS
    }
}

