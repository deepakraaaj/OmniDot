package com.omnidot.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class HealthIssue(val title: String, val detail: String) {
    NOTIFICATIONS_BLOCKED("Notifications are off", "Workflows that show notifications can't alert you."),
    LISTENER_MISSING("WhatsApp access needed", "Allow notification access so WhatsApp workflows can run."),
    BATTERY_OPTIMIZED("Battery saver may pause workflows", "Let OmniDot run unrestricted so device events reach it."),
}

data class HealthProbe(val notificationsGranted: Boolean, val listenerGranted: Boolean, val ignoringBatteryOptimizations: Boolean)

object HealthCheck {
    fun issues(
        notificationsGranted: Boolean,
        listenerGranted: Boolean,
        ignoringBatteryOptimizations: Boolean,
        rules: List<AutomationRule>,
    ): List<HealthIssue> {
        val enabled = rules.filter { it.enabled }
        if (enabled.isEmpty()) return emptyList()
        return buildList {
            if (!notificationsGranted && enabled.any { it.action == AutomationAction.SHOW_NOTIFICATION }) add(HealthIssue.NOTIFICATIONS_BLOCKED)
            if (!listenerGranted && enabled.any { it.trigger == AutomationTrigger.WHATSAPP_MESSAGE }) add(HealthIssue.LISTENER_MISSING)
            if (!ignoringBatteryOptimizations) add(HealthIssue.BATTERY_OPTIMIZED)
        }
    }

    fun probe(context: Context): HealthProbe = HealthProbe(
        notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        listenerGranted = OmniNotificationListenerService.isNotificationAccessGranted(context),
        ignoringBatteryOptimizations = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
    )

    /** Settings screen that fixes [issue]. Notification permission on API 33+ is requested in-app instead. */
    fun fixIntent(context: Context, issue: HealthIssue): Intent = when (issue) {
        HealthIssue.NOTIFICATIONS_BLOCKED -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        HealthIssue.LISTENER_MISSING -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        HealthIssue.BATTERY_OPTIMIZED -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
