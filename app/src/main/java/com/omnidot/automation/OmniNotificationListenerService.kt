package com.omnidot.automation

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OmniNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()

        if (title.isBlank()) return

        Log.d("OmniDot", "WhatsApp notification received from: '$title', text: '$text'")

        scope.launch {
            val repository = AutomationRepository(applicationContext)
            val rules = repository.current()

            val matchingRules = rules.filter { rule ->
                rule.enabled && rule.trigger == AutomationTrigger.WHATSAPP_MESSAGE &&
                    (rule.contactName.isBlank() || title.contains(rule.contactName.trim(), ignoreCase = true))
            }

            matchingRules.forEach { rule ->
                val result = AutomationEngine.execute(applicationContext, rule)
                val statusMsg = result.getOrElse { "Failed: ${it.message}" }
                repository.recordRun(rule.id, statusMsg)
            }
        }
    }

    companion object {
        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            val myComponent = ComponentName(context, OmniNotificationListenerService::class.java).flattenToString()
            return enabledListeners.contains(myComponent)
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
