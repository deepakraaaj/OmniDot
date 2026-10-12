package com.omnidot.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.omnidot.data.PreferencesManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class MatrixSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = PreferencesManager(applicationContext)
        val settings = preferences.current()
        if (!settings.automaticUpdates) return Result.success()

        // Enqueue tomorrow before rendering so a transient OEM/WallpaperManager failure
        // cannot break the daily chain. WorkManager safely defers this through Doze.
        scheduleNext(applicationContext)
        return try {
            WallpaperRefresher.refreshIfEnabled(applicationContext)
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: IllegalStateException) {
            Result.failure()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK = "omnidot_midnight_refresh"

        fun scheduleNext(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val delay = Duration.between(now, nextMidnight).coerceAtLeast(Duration.ZERO)
            val request = OneTimeWorkRequestBuilder<MatrixSyncWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}

