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
import kotlinx.coroutines.CancellationException

class MatrixSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = PreferencesManager(applicationContext)
        val settings = preferences.current()
        if (!settings.automaticUpdates) return Result.success()

        try {
            WallpaperRefresher.refreshIfEnabled(applicationContext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            // Permission or OEM refusal: retrying today won't help, try again tomorrow.
        } catch (_: IllegalStateException) {
        } catch (_: Exception) {
            if (runAttemptCount < 3) return Result.retry()
        }
        // Chain tomorrow only once today's render is settled. REPLACE would cancel this very
        // worker mid-render (a running worker still counts as unfinished unique work), so
        // APPEND_OR_REPLACE queues tomorrow behind it instead. Always report success: a failed
        // prerequisite would also fail the appended request and break the daily chain.
        scheduleNext(applicationContext, policy = ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "omnidot_midnight_refresh"

        fun scheduleNext(
            context: Context,
            now: ZonedDateTime = ZonedDateTime.now(),
            policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        ) {
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val delay = Duration.between(now, nextMidnight).coerceAtLeast(Duration.ZERO)
            val request = OneTimeWorkRequestBuilder<MatrixSyncWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                policy,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}

