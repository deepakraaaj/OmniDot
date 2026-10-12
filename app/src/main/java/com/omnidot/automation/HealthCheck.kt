package com.omnidot.automation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

enum class HealthIssue(val title: String, val detail: String) {
    NOTIFICATIONS_BLOCKED("Notifications are off", "Workflows that show notifications can't alert you."),
    LISTENER_MISSING("WhatsApp access needed", "Allow notification access so WhatsApp workflows can run."),
    BACKGROUND_RESTRICTED("Background usage is off", "Allow background usage so device events reach OmniDot."),
}

data class HealthProbe(val notificationsGranted: Boolean, val listenerGranted: Boolean, val backgroundAllowed: Boolean)

object HealthCheck {
    fun issues(
        notificationsGranted: Boolean,
        listenerGranted: Boolean,
        backgroundAllowed: Boolean,
        rules: List<AutomationRule>,
    ): List<HealthIssue> {
        val enabled = rules.filter { it.enabled }
        if (enabled.isEmpty()) return emptyList()
        return buildList {
            if (!notificationsGranted && enabled.any { it.action == AutomationAction.SHOW_NOTIFICATION }) add(HealthIssue.NOTIFICATIONS_BLOCKED)
            if (!listenerGranted && enabled.any { it.trigger == AutomationTrigger.WHATSAPP_MESSAGE }) add(HealthIssue.LISTENER_MISSING)
            if (!backgroundAllowed) add(HealthIssue.BACKGROUND_RESTRICTED)
        }
    }

    fun probe(context: Context): HealthProbe = HealthProbe(
        notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        listenerGranted = OmniNotificationListenerService.isNotificationAccessGranted(context),
        // "Allow background usage" (even when battery stays "Optimized") is enough for OmniDot;
        // only the Restricted state stops background work, so check that rather than the unrestricted allowlist.
        backgroundAllowed = Build.VERSION.SDK_INT < 28 || !context.getSystemService(ActivityManager::class.java).isBackgroundRestricted,
    )

    /** Settings screen that fixes [issue]. Notification permission on API 33+ is requested in-app instead. */
    fun fixIntent(context: Context, issue: HealthIssue): Intent = when (issue) {
        HealthIssue.NOTIFICATIONS_BLOCKED -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        HealthIssue.LISTENER_MISSING -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        HealthIssue.BACKGROUND_RESTRICTED -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
