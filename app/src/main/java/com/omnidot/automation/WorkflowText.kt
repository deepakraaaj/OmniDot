package com.omnidot.automation

import com.omnidot.alarm.DaysText
import java.net.URI

object WorkflowText {
    data class LastRun(val text: String, val failed: Boolean)

    fun whenText(rule: AutomationRule): String = when (rule.trigger) {
        AutomationTrigger.PHONE_ALARM_RINGS -> "When my alarm rings"
        AutomationTrigger.ALARM_DISMISSED -> "When I turn off my alarm"
        AutomationTrigger.POWER_CONNECTED -> "When charger connects"
        AutomationTrigger.POWER_DISCONNECTED -> "When charger disconnects"
        AutomationTrigger.BATTERY_LOW -> "When battery drops below 20%"
        AutomationTrigger.BATTERY_OKAY -> "When battery is charged"
        AutomationTrigger.DEVICE_BOOT -> "When the phone restarts"
        AutomationTrigger.SCREEN_UNLOCKED -> "When you unlock the phone"
        AutomationTrigger.HEADSET_PLUG -> "When headphones connect"
        AutomationTrigger.WIFI_CONNECTED -> "When Wi-Fi connects"
        AutomationTrigger.WIFI_DISCONNECTED -> "When Wi-Fi disconnects"
        AutomationTrigger.WHATSAPP_MESSAGE -> "When ${rule.contactName.ifBlank { "anyone" }} ${if (rule.onlyLinks) "sends a link" else "messages"} on WhatsApp"
        AutomationTrigger.INBOUND_WEBHOOK -> "When a remote trigger arrives"
        AutomationTrigger.SCHEDULED_TIME -> "${if (rule.days.isEmpty()) "Every day" else DaysText.describe(rule.days)} at ${rule.scheduledTime}"
        AutomationTrigger.WEATHER_RAIN -> "When rain is forecast"
        AutomationTrigger.DAILY_MIDNIGHT -> "At midnight"
    }

    fun doText(rule: AutomationRule): String = when (rule.action) {
        AutomationAction.RUN_CODE -> rule.code.lines().count { it.isNotBlank() }.let { if (it == 0) "Run your code" else "Run code ($it lines)" }
        AutomationAction.SET_PHONE_ALARM -> "Set phone alarm in ${rule.alarmInMinutes} min"
        AutomationAction.RING_ALARM -> if (rule.alarmLabel.isBlank()) "Ring calm alarm" else "Ring calm alarm \"${rule.alarmLabel}\""
        AutomationAction.SPEAK_TEXT -> if (rule.targetMessage.isBlank()) "Speak your words" else "Speak \"${rule.targetMessage}\""
        AutomationAction.SEND_WHATSAPP -> "Open WhatsApp to ${rule.contactName.ifBlank { "a contact" }}"
        AutomationAction.CALL_PHONE -> "Open dialer for ${rule.contactName.ifBlank { "a contact" }}"
        AutomationAction.APPLY_PRESET -> "Show ${rule.preset.title} on lock screen"
        AutomationAction.REFRESH_CURRENT -> "Refresh lock screen"
        AutomationAction.CYCLE_PRESET -> "Switch lock screen view"
        AutomationAction.CYCLE_ACCENT -> "Change dot colour"
        AutomationAction.HTTP_REQUEST -> "Call ${host(rule.url)}"
        AutomationAction.HTTP_POST -> "Send webhook to ${host(rule.url)}"
        AutomationAction.DISCORD_WEBHOOK -> "Post to Discord"
        AutomationAction.SLACK_WEBHOOK -> "Post to Slack"
        AutomationAction.SHOW_NOTIFICATION -> "Show notification \"${rule.message}\""
        AutomationAction.VIBRATE -> "Vibrate"
        AutomationAction.COPY_STATS -> "Copy progress to clipboard"
        AutomationAction.OPEN_URL -> "Open ${host(rule.url)}"
    }

    fun name(rule: AutomationRule): String =
        "${whenText(rule)} → ${doText(rule).replaceFirstChar { it.lowercase() }}"

    fun lastRun(rule: AutomationRule, nowMillis: Long): LastRun {
        if (rule.lastRunTime == 0L) return LastRun("Not run yet", false)
        val status = rule.lastRunStatus
        val failed = status.startsWith("Error") || status.startsWith("✕") || status.startsWith("Failed")
        if (failed) {
            val reason = status.removePrefix("✕").trim().removePrefix("Failed:").removePrefix("Error:").trim()
            return LastRun("✕ $reason", true)
        }
        val minutes = (nowMillis - rule.lastRunTime) / 60_000
        val ago = when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 24 * 60 -> "${minutes / 60} h ago"
            else -> "${minutes / (24 * 60)} d ago"
        }
        return LastRun("✓ Ran $ago", false)
    }

    fun canSave(rule: AutomationRule): Boolean {
        if (!rule.trigger.available) return false
        if (rule.trigger == AutomationTrigger.SCHEDULED_TIME && DaysText.parseTime(rule.scheduledTime) == null) return false
        return when (rule.action) {
            AutomationAction.SPEAK_TEXT -> rule.targetMessage.isNotBlank()
            AutomationAction.SEND_WHATSAPP, AutomationAction.CALL_PHONE ->
                rule.targetMessage.isNotBlank() && rule.contactPhone.isNotBlank()
            AutomationAction.HTTP_REQUEST, AutomationAction.HTTP_POST, AutomationAction.DISCORD_WEBHOOK,
            AutomationAction.SLACK_WEBHOOK, AutomationAction.OPEN_URL ->
                rule.url.startsWith("http://") || rule.url.startsWith("https://")
            AutomationAction.SHOW_NOTIFICATION -> rule.message.isNotBlank()
            AutomationAction.SET_PHONE_ALARM -> rule.alarmInMinutes in 1..1440
            AutomationAction.RUN_CODE -> rule.code.isNotBlank()
            else -> true
        }
    }

    private fun host(url: String): String =
        runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: "a URL"
}
