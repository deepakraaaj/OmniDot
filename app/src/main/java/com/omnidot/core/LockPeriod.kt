package com.omnidot.core

enum class LockPeriod(val label: String) {
    THIS_MONTH("This month"),
    LAST_12_WEEKS("Last 12 weeks"),
    THIS_YEAR("This year");

    companion object {
        fun fromStored(value: String?): LockPeriod =
            entries.firstOrNull { it.name == value } ?: THIS_MONTH
    }
}
