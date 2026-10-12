package com.omnidot.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            AlarmScheduler.ACTION_RING -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_START)
                        .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
                )
            }
            AlarmScheduler.ACTION_TIME_RULE -> {
                val ruleId = intent.getStringExtra(AlarmScheduler.EXTRA_RULE_ID) ?: return
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val repository = AutomationRepository(context)
                        val rules = repository.current()
                        rules.firstOrNull { it.id == ruleId && it.enabled }?.let { rule ->
                            val result = AutomationEngine.execute(context, rule)
                            repository.recordRun(rule.id, result.getOrElse { "Error: ${it.message}" })
                        }
                        AlarmScheduler.syncTimeRules(context, rules)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
