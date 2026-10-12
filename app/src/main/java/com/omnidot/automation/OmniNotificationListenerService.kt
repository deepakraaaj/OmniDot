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
    private val alarmTracker by lazy { AlarmNotificationTracker(packageName) }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return
        alarmTracker.onRemoved(sbn.key)?.let { runTrigger(it) }
    }

    private fun runTrigger(trigger: AutomationTrigger) {
        scope.launch {
            val repository = AutomationRepository(applicationContext)
            repository.current().filter { it.enabled && it.trigger == trigger }.forEach { rule ->
                val result = AutomationEngine.execute(applicationContext, rule)
                repository.recordRun(rule.id, result.getOrElse { "Error: ${it.message}" })
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        alarmTracker.onPosted(sbn.key, pkg, sbn.notification.category)?.let { runTrigger(it) }
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()

        if (title.isBlank()) return

        Log.d("OmniDot", "WhatsApp notification received from: '$title', text: '$text'")

        scope.launch {
            val repository = AutomationRepository(applicationContext)
            val rules = repository.current()

            val link = LinkDetector.firstLink(text)
            val matchingRules = rules.filter { rule ->
                rule.enabled && rule.trigger == AutomationTrigger.WHATSAPP_MESSAGE &&
                    (rule.contactName.isBlank() || title.contains(rule.contactName.trim(), ignoreCase = true)) &&
                    (!rule.onlyLinks || link != null)
            }
            val event = TriggerEvent(sender = title, message = text, link = link.orEmpty())

            matchingRules.forEach { rule ->
                val result = AutomationEngine.execute(applicationContext, rule, event)
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
