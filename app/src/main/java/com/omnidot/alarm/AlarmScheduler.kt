package com.omnidot.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.omnidot.MainActivity
import com.omnidot.automation.AutomationRepository
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationTrigger
import java.time.ZonedDateTime

object AlarmScheduler {
    const val ACTION_RING = "com.omnidot.ALARM_RING"
    const val ACTION_TIME_RULE = "com.omnidot.TIME_RULE"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_RULE_ID = "rule_id"

    /** Schedules the alarm's next ring (or cancels it when disabled). Returns the ring time. */
    fun scheduleNext(context: Context, alarm: CalmAlarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return null
        }
        return AlarmSchedule.nextTrigger(now, alarm).also { ringAt(context, alarm.id, it) }
    }

    /** Uses the alarm-clock API: exact through Doze, and shows the alarm icon in the status bar. */
    fun ringAt(context: Context, alarmId: String, at: ZonedDateTime) {
        val show = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TAB, "alarms"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager(context).setAlarmClock(AlarmManager.AlarmClockInfo(at.toInstant().toEpochMilli(), show), ringIntent(context, alarmId))
    }

    fun cancel(context: Context, alarmId: String) = alarmManager(context).cancel(ringIntent(context, alarmId))

    fun syncTimeRules(context: Context, rules: List<AutomationRule>, now: ZonedDateTime = ZonedDateTime.now()) {
        rules.filter { it.trigger == AutomationTrigger.SCHEDULED_TIME }.forEach { rule ->
            val minute = DaysText.parseTime(rule.scheduledTime)
            if (!rule.enabled || minute == null) {
                cancelTimeRule(context, rule.id)
            } else {
                val at = AlarmSchedule.nextTrigger(now, CalmAlarm(rule.id, minute, rule.days))
                val am = alarmManager(context)
                val op = timeRuleIntent(context, rule.id)
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), op)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), op)
                }
            }
        }
    }

    fun cancelTimeRule(context: Context, ruleId: String) = alarmManager(context).cancel(timeRuleIntent(context, ruleId))

    suspend fun rescheduleAll(context: Context) {
        AlarmRepository(context).current().forEach { scheduleNext(context, it) }
        syncTimeRules(context, AutomationRepository(context).current())
    }

    private fun ringIntent(context: Context, alarmId: String) = PendingIntent.getBroadcast(
        context, "alarm:$alarmId".hashCode(),
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_RING).putExtra(EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun timeRuleIntent(context: Context, ruleId: String) = PendingIntent.getBroadcast(
        context, "rule:$ruleId".hashCode(),
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_TIME_RULE).putExtra(EXTRA_RULE_ID, ruleId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
