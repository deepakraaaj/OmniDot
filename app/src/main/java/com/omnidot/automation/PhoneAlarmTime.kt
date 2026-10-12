package com.omnidot.automation

import java.time.LocalTime

object PhoneAlarmTime {
    /** Clock time [minutes] from [now], rounded to the minute (Clock apps take hour and minute). */
    fun inMinutes(now: LocalTime, minutes: Int): LocalTime = now.withSecond(0).withNano(0).plusMinutes(minutes.toLong())
}
