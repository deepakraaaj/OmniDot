package com.omnidot.alarm

import java.time.DayOfWeek

data class CalmAlarm(
    val id: String,
    val minuteOfDay: Int,
    /** Empty means a one-time alarm. */
    val days: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val speakGreeting: Boolean = true,
)
