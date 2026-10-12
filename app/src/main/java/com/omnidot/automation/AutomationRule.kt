package com.omnidot.automation

import com.omnidot.core.MatrixPreset
import java.util.UUID

enum class TriggerCategory(val label: String) {
    ALL("All"),
    COMMUNICATION("WhatsApp & Chat"),
    SCHEDULE("Time & Weather"),
    HARDWARE("Power & Battery"),
    SYSTEM("Device & Events"),
    NETWORK("Webhooks & Net"),
}

enum class AutomationTrigger(
    val label: String,
    val category: TriggerCategory,
    val subtitle: String,
    val guideInfo: String,
    val available: Boolean = true,
    val needsAppAlive: Boolean = false,
) {
    WHATSAPP_MESSAGE(
        label = "WhatsApp message received",
        category = TriggerCategory.COMMUNICATION,
        subtitle = "When a message arrives from a chosen contact",
        guideInfo = "Monitors incoming WhatsApp notifications for matching sender name or message keywords.",
    ),
    WEATHER_RAIN(
        label = "Rain / Weather change",
        category = TriggerCategory.SCHEDULE,
        subtitle = "When morning forecast indicates rain (e.g. 7:00 AM)",
        guideInfo = "Triggers if weather conditions indicate rain or temperature shifts.",
        available = false,
    ),
    SCHEDULED_TIME(
        label = "At a time",
        category = TriggerCategory.SCHEDULE,
        subtitle = "At a set time, every day or on chosen days",
        guideInfo = "Runs automatically at your chosen scheduled hour and minute every day.",
    ),
    PHONE_ALARM_RINGS(
        label = "When my alarm rings",
        category = TriggerCategory.SCHEDULE,
        subtitle = "When your Clock app's alarm goes off",
        guideInfo = "Detects the ringing alarm from your Clock app. Needs notification access.",
    ),
    ALARM_DISMISSED(
        label = "When I turn off my alarm",
        category = TriggerCategory.SCHEDULE,
        subtitle = "When you dismiss or snooze your alarm",
        guideInfo = "Runs right after your Clock app's alarm (or a calm alarm) stops, so your morning routine can follow.",
    ),
    POWER_CONNECTED(
        label = "Charger connected",
        category = TriggerCategory.HARDWARE,
        subtitle = "When plugged into power or wireless pad",
        guideInfo = "Triggers immediately as soon as phone detects charging.",
        needsAppAlive = true,
    ),
    POWER_DISCONNECTED(
        label = "Charger disconnected",
        category = TriggerCategory.HARDWARE,
        subtitle = "When power cable is unplugged",
        guideInfo = "Fires when power cable is removed.",
        needsAppAlive = true,
    ),
    BATTERY_LOW(
        label = "Battery below 20%",
        category = TriggerCategory.HARDWARE,
        subtitle = "When charge drops below 20%",
        guideInfo = "System alert when battery drops below 20%.",
        needsAppAlive = true,
    ),
    BATTERY_OKAY(
        label = "Battery fully charged",
        category = TriggerCategory.HARDWARE,
        subtitle = "When charge reaches 100% / healthy",
        guideInfo = "Fires when battery finishes charging.",
        needsAppAlive = true,
    ),
    DEVICE_BOOT(
        label = "Device restarted",
        category = TriggerCategory.SYSTEM,
        subtitle = "After phone finishes rebooting",
        guideInfo = "Triggered when Android finishes booting up.",
    ),
    SCREEN_UNLOCKED(
        label = "Screen unlocked",
        category = TriggerCategory.SYSTEM,
        subtitle = "When device lockscreen is unlocked",
        guideInfo = "Triggers when you unlock the device.",
        needsAppAlive = true,
    ),
    DAILY_MIDNIGHT(
        label = "Midnight rollover",
        category = TriggerCategory.SYSTEM,
        subtitle = "Every night at 00:00 midnight",
        guideInfo = "Fires automatically every night at 00:00.",
        available = false,
    ),
    WIFI_CONNECTED(
        label = "Wi-Fi connected",
        category = TriggerCategory.NETWORK,
        subtitle = "When connected to local Wi-Fi",
        guideInfo = "Fires when joining a Wi-Fi network.",
        needsAppAlive = true,
    ),
    WIFI_DISCONNECTED(
        label = "Wi-Fi disconnected",
        category = TriggerCategory.NETWORK,
        subtitle = "When disconnected from Wi-Fi",
        guideInfo = "Fires when leaving Wi-Fi.",
        needsAppAlive = true,
    ),
    HEADSET_PLUG(
        label = "Headphones connected",
        category = TriggerCategory.HARDWARE,
        subtitle = "When wired headphones are plugged in",
        guideInfo = "Fires when headphones or earbuds are connected.",
        needsAppAlive = true,
    ),
    INBOUND_WEBHOOK(
        label = "Deep Link / Remote Trigger",
        category = TriggerCategory.NETWORK,
        subtitle = "Trigger via omnidot:// URL, Tasker, or ADB",
        guideInfo = "Fires when OmniDot receives an external deep link or ADB broadcast.",
    );

    companion object {
        val pickerOptions: List<AutomationTrigger> = entries.filter { it.available }

        fun fromStored(value: String?): AutomationTrigger =
            entries.firstOrNull { it.name == value } ?: POWER_CONNECTED
    }
}

enum class ActionCategory(val label: String) {
    ALL("All"),
    COMMUNICATION("Call & WhatsApp"),
    VOICE("Voice & Speech"),
    MATRIX("Lockscreen HUD"),
    WEBHOOK("Webhooks & Cloud"),
    SYSTEM("System & Alerts"),
}

enum class AutomationAction(
    val label: String,
    val category: ActionCategory,
    val subtitle: String,
    val guideInfo: String,
) {
    SET_PHONE_ALARM(
        label = "Set phone alarm",
        category = ActionCategory.SYSTEM,
        subtitle = "Add an alarm to your Clock app, minutes from now",
        guideInfo = "Sets a one-time alarm in your Clock app, for example 20 minutes after a meeting link arrives.",
    ),
    RING_ALARM(
        label = "Ring calm alarm",
        category = ActionCategory.VOICE,
        subtitle = "Soft bell that slowly rises, full screen, snooze, good-morning goals",
        guideInfo = "Rings on the alarm channel until you tap Snooze or I'm up. Pair it with an At a time trigger.",
    ),
    SPEAK_TEXT(
        label = "Speak aloud (Voice TTS)",
        category = ActionCategory.VOICE,
        subtitle = "Speak your text aloud in a calm voice",
        guideInfo = "Uses Android's native Text-to-Speech engine to voice your text out loud through the speaker.",
    ),
    SEND_WHATSAPP(
        label = "Send WhatsApp message",
        category = ActionCategory.COMMUNICATION,
        subtitle = "Send WhatsApp text to contact (e.g. \"Bring umbrella\")",
        guideInfo = "Opens WhatsApp conversation with target contact and pre-filled message text.",
    ),
    CALL_PHONE(
        label = "Open dialer + speak aloud",
        category = ActionCategory.COMMUNICATION,
        subtitle = "Prepare a call and speak text on this device",
        guideInfo = "Opens the dialer, then speaks the configured text on the device. Android does not route app speech into the call.",
    ),
    APPLY_PRESET(
        label = "Apply matrix preset",
        category = ActionCategory.MATRIX,
        subtitle = "Set lockscreen to chosen matrix HUD",
        guideInfo = "Compiles the chosen matrix to the lockscreen wallpaper.",
    ),
    REFRESH_CURRENT(
        label = "Refresh wallpaper",
        category = ActionCategory.MATRIX,
        subtitle = "Recompute HUD with latest date & time",
        guideInfo = "Re-renders the current lockscreen HUD to ensure dots are up-to-date.",
    ),
    CYCLE_PRESET(
        label = "Cycle to next matrix",
        category = ActionCategory.MATRIX,
        subtitle = "Rotate: Goals → Year → Life",
        guideInfo = "Switches to the next matrix sequentially.",
    ),
    CYCLE_ACCENT(
        label = "Cycle accent color",
        category = ActionCategory.MATRIX,
        subtitle = "Rotate dot colors: Amber, Emerald, Blue, Crimson",
        guideInfo = "Cycles through curated OLED matrix palette accent colors.",
    ),
    HTTP_REQUEST(
        label = "Call remote URL (GET)",
        category = ActionCategory.WEBHOOK,
        subtitle = "HTTP GET with dynamic query variables",
        guideInfo = "Sends an HTTP GET request to any endpoint.",
    ),
    HTTP_POST(
        label = "Send Webhook (POST)",
        category = ActionCategory.WEBHOOK,
        subtitle = "POST JSON payload to remote server",
        guideInfo = "Sends a POST request with JSON body to Home Assistant, n8n, Zapier, etc.",
    ),
    DISCORD_WEBHOOK(
        label = "Discord webhook",
        category = ActionCategory.WEBHOOK,
        subtitle = "Post formatted card to Discord channel",
        guideInfo = "Sends a rich embed card directly to a Discord webhook URL.",
    ),
    SLACK_WEBHOOK(
        label = "Slack webhook",
        category = ActionCategory.WEBHOOK,
        subtitle = "Post message to Slack / Mattermost",
        guideInfo = "Sends a formatted incoming webhook message to a Slack channel.",
    ),
    SHOW_NOTIFICATION(
        label = "System notification",
        category = ActionCategory.SYSTEM,
        subtitle = "Post an Android notification alert",
        guideInfo = "Creates an Android status bar notification alert.",
    ),
    VIBRATE(
        label = "Haptic vibration",
        category = ActionCategory.SYSTEM,
        subtitle = "Tactile double-pulse vibration",
        guideInfo = "Gives a distinct tactile double-pulse haptic buzz on the phone.",
    ),
    COPY_STATS(
        label = "Copy stats to clipboard",
        category = ActionCategory.SYSTEM,
        subtitle = "Copy progress string to clipboard",
        guideInfo = "Formats current date progress and copies it to the clipboard.",
    ),
    OPEN_URL(
        label = "Open web link",
        category = ActionCategory.SYSTEM,
        subtitle = "Launch link in default browser",
        guideInfo = "Fires an intent to open your preferred web dashboard.",
    );

    companion object {
        val workflowOptions: List<AutomationAction> =
            entries.filterNot { it == APPLY_PRESET || it == CYCLE_PRESET }

        fun fromStored(value: String?): AutomationAction =
            entries.firstOrNull { it.name == value } ?: SHOW_NOTIFICATION
    }
}

data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val trigger: AutomationTrigger,
    val action: AutomationAction,
    val preset: MatrixPreset = MatrixPreset.YEAR_PROGRESS,
    val contactName: String = "",
    val contactPhone: String = "",
    val targetMessage: String = "",
    val scheduledTime: String = "07:00",
    /** Days for SCHEDULED_TIME; empty means every day. */
    val days: Set<java.time.DayOfWeek> = emptySet(),
    val weatherCondition: String = "Rain",
    val url: String = "",
    val message: String = "Progress: {year_percent}% complete",
    val customPayload: String = "",
    val lastRunTime: Long = 0L,
    val lastRunStatus: String = "",
    val speechPitch: Float = VoiceStyle.CALM.pitch,
    val speechRate: Float = VoiceStyle.CALM.rate,
    val playChime: Boolean = true,
    val alarmLabel: String = "",
    val snoozeMinutes: Int = 10,
    val speakGreeting: Boolean = true,
    /** WhatsApp trigger: only messages that contain a link. */
    val onlyLinks: Boolean = false,
    val alarmInMinutes: Int = 20,
) {
    companion object {
        fun createDefaultChargerConnectedRule(): AutomationRule = AutomationRule(
            id = "default_charger_connected",
            name = "When charger connects → speak",
            trigger = AutomationTrigger.POWER_CONNECTED,
            action = AutomationAction.SPEAK_TEXT,
            targetMessage = "Charging started",
        )

        fun createDefaultChargerDisconnectedRule(): AutomationRule = AutomationRule(
            id = "default_charger_disconnected",
            name = "When charger disconnects → speak",
            trigger = AutomationTrigger.POWER_DISCONNECTED,
            action = AutomationAction.SPEAK_TEXT,
            targetMessage = "Charger disconnected",
        )
    }
}
