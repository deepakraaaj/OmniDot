package com.omnidot.automation

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.omnidot.alarm.AlarmRingService
import com.omnidot.alarm.AlarmScheduler
import java.time.LocalTime
import java.time.ZonedDateTime

object PhoneAlarms {
    /** Sets a one-time Clock app alarm [minutes] from now; falls back to OmniDot's calm alarm. Returns "HH:mm" and whether the Clock app took it. */
    fun set(context: Context, id: String, minutes: Int, label: String): Pair<String, Boolean> {
        val at = PhoneAlarmTime.inMinutes(LocalTime.now(), minutes)
        val time = "%02d:%02d".format(at.hour, at.minute)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, at.hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, at.minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label.take(60))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            time to true
        } catch (e: Exception) {
            // No Clock app or a background-start block: use OmniDot's own calm alarm instead.
            AlarmScheduler.snooze(context, AlarmRingService.Ring(id, label, 10, false), ZonedDateTime.now().plusMinutes(minutes.toLong()))
            time to false
        }
    }
}
