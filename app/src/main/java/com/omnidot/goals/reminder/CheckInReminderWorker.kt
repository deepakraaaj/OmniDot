package com.omnidot.goals.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.omnidot.data.PreferencesManager
import com.omnidot.goals.GoalRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class CheckInReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = PreferencesManager(applicationContext).current()
        if (!settings.reminderEnabled) return Result.success()
        schedule(applicationContext, settings.reminderMinuteOfDay)
        val today = LocalDate.now()
        CheckInNotifier.show(applicationContext, ReminderSchedule.pendingToday(GoalRepository(applicationContext).current(), today), today)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "omnidot_checkin_reminder"

        fun schedule(context: Context, minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now()) {
            val delay = ReminderSchedule.delayUntilNext(now, minuteOfDay)
            val request = OneTimeWorkRequestBuilder<CheckInReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }
}
