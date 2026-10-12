package com.omnidot.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.reminder.CheckInReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> {
                MatrixSyncWorker.scheduleNext(context)
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val settings = PreferencesManager(context).current()
                        if (settings.reminderEnabled) CheckInReminderWorker.schedule(context, settings.reminderMinuteOfDay)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
