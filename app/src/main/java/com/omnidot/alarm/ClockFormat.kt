package com.omnidot.alarm

import android.content.Context
import android.text.format.DateFormat

/** Formats a minute-of-day in the phone's 12- or 24-hour style. */
fun formatClock(context: Context, minuteOfDay: Int): String {
    if (DateFormat.is24HourFormat(context)) return DaysText.formatTime(minuteOfDay)
    val h = minuteOfDay / 60
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(hour12, minuteOfDay % 60, if (h < 12) "AM" else "PM")
}
