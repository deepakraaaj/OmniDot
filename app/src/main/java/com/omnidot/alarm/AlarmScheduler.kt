package com.omnidot.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.omnidot.MainActivity
import com.omnidot.automation.AutomationAction
import com.omnidot.automation.AutomationRepository
import com.omnidot.automation.AutomationRule
import com.omnidot.automation.AutomationTrigger
import java.time.ZonedDateTime

object AlarmScheduler {
    const val ACTION_RING = "com.omnidot.ALARM_RING"
    const val ACTION_TIME_RULE = "com.omnidot.TIME_RULE"
    const val EXTRA_RULE_ID = "rule_id"

    /** Arms every enabled "At a time" workflow; alarm workflows use the alarm-clock API. */
    fun syncTimeRules(context: Context, rules: List<AutomationRule>, now: ZonedDateTime = ZonedDateTime.now()) {
        rules.filter { it.trigger == AutomationTrigger.SCHEDULED_TIME }.forEach { rule ->
            val minute = DaysText.parseTime(rule.scheduledTime)
            if (!rule.enabled || minute == null) {
                cancelTimeRule(context, rule.id)
                return@forEach
            }
            val at = AlarmSchedule.nextTrigger(now, minute, rule.days).toInstant().toEpochMilli()
            val op = timeRuleIntent(context, rule.id)
            val am = alarmManager(context)
            when {
                // Alarm-clock scheduling: exact through Doze and shows the alarm icon in the status bar.
                rule.action == AutomationAction.RING_ALARM -> am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(context)), op)
                Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms() -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
                else -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
            }
        }
    }

    fun cancelTimeRule(context: Context, ruleId: String) = alarmManager(context).cancel(timeRuleIntent(context, ruleId))

    suspend fun rescheduleAll(context: Context) = syncTimeRules(context, AutomationRepository(context).current())

    /** Re-rings a calm alarm later (snooze), carrying its settings along. */
    fun snooze(context: Context, ring: AlarmRingService.Ring, at: ZonedDateTime) {
        val op = PendingIntent.getBroadcast(
            context, "snooze:${ring.id}".hashCode(),
            ring.putInto(Intent(context, AlarmReceiver::class.java).setAction(ACTION_RING)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager(context).setAlarmClock(AlarmManager.AlarmClockInfo(at.toInstant().toEpochMilli(), showIntent(context)), op)
    }

    private fun showIntent(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun timeRuleIntent(context: Context, ruleId: String) = PendingIntent.getBroadcast(
        context, "rule:$ruleId".hashCode(),
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_TIME_RULE).putExtra(EXTRA_RULE_ID, ruleId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
