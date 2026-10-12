package com.omnidot.goals.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnidot.core.WallpaperRefresher
import com.omnidot.goals.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        context.getSystemService(NotificationManager::class.java).cancel(CheckInNotifier.NOTIFICATION_ID)
        if (action != ACTION_DONE) return
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val day = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay()))
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                GoalRepository(context).setCheckIn(goalId, day, done = true)
                runCatching { WallpaperRefresher.refreshIfEnabled(context) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.omnidot.CHECKIN_DONE"
        const val ACTION_SKIP = "com.omnidot.CHECKIN_SKIP"
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_EPOCH_DAY = "epoch_day"
    }
}
