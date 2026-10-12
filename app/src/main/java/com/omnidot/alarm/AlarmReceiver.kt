package com.omnidot.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnidot.automation.AutomationEngine
import com.omnidot.automation.AutomationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            AlarmScheduler.ACTION_RING -> AlarmRingService.Ring.from(intent)?.let { AlarmRingService.start(context, it) }
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
