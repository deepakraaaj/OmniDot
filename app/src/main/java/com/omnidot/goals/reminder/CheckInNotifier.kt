package com.omnidot.goals.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omnidot.MainActivity
import com.omnidot.R
import com.omnidot.goals.Goal
import java.time.LocalDate

object CheckInNotifier {
    const val CHANNEL_ID = "goal_checkins"
    const val NOTIFICATION_ID = 4201

    fun show(context: Context, pending: List<Goal>, today: LocalDate) {
        if (pending.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Goal check-ins", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Evening reminder to mark today's goals" },
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TAB, "goals")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (pending.size == 1) {
            val goal = pending.single()
            builder.setContentTitle("${goal.emoji} Did you achieve it today?")
                .setContentText(goal.title)
                .addAction(0, "Done", action(context, CheckInReceiver.ACTION_DONE, goal.id, today, 1))
                .addAction(0, "Not today", action(context, CheckInReceiver.ACTION_SKIP, goal.id, today, 2))
        } else {
            builder.setContentTitle("${pending.size} goals to check in")
                .setContentText(pending.joinToString(" · ") { it.title })
        }
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun action(context: Context, action: String, goalId: String, day: LocalDate, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, CheckInReceiver::class.java).setAction(action)
                .putExtra(CheckInReceiver.EXTRA_GOAL_ID, goalId)
                .putExtra(CheckInReceiver.EXTRA_EPOCH_DAY, day.toEpochDay()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
