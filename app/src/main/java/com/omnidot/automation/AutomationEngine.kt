package com.omnidot.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.omnidot.alarm.AlarmRingService
import com.omnidot.core.WallpaperRefresher
import com.omnidot.core.MatrixPreset
import com.omnidot.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import android.speech.tts.TextToSpeech

object AutomationEngine {

    suspend fun execute(context: Context, rule: AutomationRule, event: TriggerEvent? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val preferencesManager = PreferencesManager(context)
            val currentPrefs = preferencesManager.current()

            when (rule.action) {
                AutomationAction.APPLY_PRESET -> {
                    preferencesManager.setPreset(rule.preset)
                    val updated = currentPrefs.copy(preset = rule.preset)
                    WallpaperRefresher.apply(context, updated)
                    "Lockscreen updated with '${rule.preset.title}'"
                }

                AutomationAction.REFRESH_CURRENT -> {
                    WallpaperRefresher.apply(context, currentPrefs)
                    "Current lockscreen HUD refreshed"
                }

                AutomationAction.CYCLE_PRESET -> {
                    val presets = MatrixPreset.entries
                    val currentIndex = presets.indexOf(currentPrefs.preset).coerceAtLeast(0)
                    val nextPreset = presets[(currentIndex + 1) % presets.size]
                    preferencesManager.setPreset(nextPreset)
                    val updated = currentPrefs.copy(preset = nextPreset)
                    WallpaperRefresher.apply(context, updated)
                    "Cycled to next preset: ${nextPreset.title}"
                }

                AutomationAction.CYCLE_ACCENT -> {
                    val colors = listOf(
                        0xFF5FB3A6.toInt(), // Teal
                        0xFF00C853.toInt(), // Emerald Green
                        0xFF2979FF.toInt(), // Cyber Blue
                        0xFFAA00FF.toInt(), // Purple
                        0xFFFF1744.toInt(), // Crimson
                        0xFFFFD600.toInt(), // Yellow
                        0xFFFFFFFF.toInt(), // OLED White
                    )
                    val currentIndex = colors.indexOf(currentPrefs.accentColor).coerceAtLeast(0)
                    val nextColor = colors[(currentIndex + 1) % colors.size]
                    preferencesManager.setAccentColor(nextColor)
                    val updated = currentPrefs.copy(accentColor = nextColor)
                    WallpaperRefresher.apply(context, updated)
                    "Cycled accent color to #${Integer.toHexString(nextColor).uppercase()}"
                }

                AutomationAction.HTTP_REQUEST -> {
                    val resolvedUrl = resolveVariables(rule.url, context, rule)
                    performHttpGet(resolvedUrl)
                }

                AutomationAction.HTTP_POST -> {
                    val resolvedUrl = resolveVariables(rule.url, context, rule)
                    val payload = if (rule.customPayload.isNotBlank()) {
                        resolveVariables(rule.customPayload, context, rule)
                    } else {
                        buildDefaultJsonPayload(context, rule)
                    }
                    performHttpPost(resolvedUrl, payload, "application/json")
                }

                AutomationAction.DISCORD_WEBHOOK -> {
                    val resolvedUrl = resolveVariables(rule.url, context, rule)
                    val resolvedMessage = resolveVariables(rule.message, context, rule)
                    val discordJson = JSONObject().apply {
                        put("content", "⚡ **OmniDot Trigger:** ${rule.trigger.label}\n$resolvedMessage")
                        val embed = JSONObject().apply {
                            put("title", "OmniDot Automation Event")
                            put("description", resolvedMessage)
                            put("color", 0x5FB3A6) // OmniDot teal
                            val fields = JSONArray().apply {
                                put(JSONObject().put("name", "Trigger").put("value", rule.trigger.label).put("inline", true))
                                put(JSONObject().put("name", "Active Matrix").put("value", rule.preset.title).put("inline", true))
                                put(JSONObject().put("name", "Battery").put("value", "${getBatteryLevel(context)}%").put("inline", true))
                            }
                            put("fields", fields)
                            put("footer", JSONObject().put("text", "OmniDot Local Automation"))
                        }
                        put("embeds", JSONArray().put(embed))
                    }.toString()
                    performHttpPost(resolvedUrl, discordJson, "application/json")
                }

                AutomationAction.SLACK_WEBHOOK -> {
                    val resolvedUrl = resolveVariables(rule.url, context, rule)
                    val resolvedMessage = resolveVariables(rule.message, context, rule)
                    val slackJson = JSONObject().apply {
                        put("text", "⚡ *OmniDot:* ${rule.trigger.label} → $resolvedMessage\n• Battery: ${getBatteryLevel(context)}%\n• HUD: ${rule.preset.title}")
                    }.toString()
                    performHttpPost(resolvedUrl, slackJson, "application/json")
                }

                AutomationAction.SHOW_NOTIFICATION -> {
                    val resolvedMessage = resolveVariables(rule.message, context, rule)
                    showNotification(context, "OmniDot Automation", resolvedMessage)
                    "Notification displayed"
                }

                AutomationAction.VIBRATE -> {
                    triggerVibration(context)
                    "Haptic vibration pulse fired"
                }

                AutomationAction.COPY_STATS -> {
                    val stats = getProgressSummary(rule.preset)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("OmniDot Stats", stats)
                    clipboard.setPrimaryClip(clip)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Copied to clipboard: $stats", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    "Copied to clipboard: \"$stats\""
                }

                AutomationAction.OPEN_URL -> {
                    val resolvedUrl = resolveVariables(rule.url, context, rule)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Opened link: $resolvedUrl"
                }

                AutomationAction.SET_PHONE_ALARM -> {
                    val at = PhoneAlarmTime.inMinutes(java.time.LocalTime.now(), rule.alarmInMinutes)
                    val label = resolveVariables(rule.alarmLabel.ifBlank { "Link from {sender}" }, context, rule, event).take(60)
                    val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                        .putExtra(android.provider.AlarmClock.EXTRA_HOUR, at.hour)
                        .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, at.minute)
                        .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
                        .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                        "Phone alarm set for %02d:%02d".format(at.hour, at.minute)
                    } catch (e: Exception) {
                        // No Clock app or a background-start block: fall back to OmniDot's own calm alarm.
                        com.omnidot.alarm.AlarmScheduler.snooze(
                            context, AlarmRingService.Ring(rule.id, label, rule.snoozeMinutes, false),
                            java.time.ZonedDateTime.now().plusMinutes(rule.alarmInMinutes.toLong()),
                        )
                        "Calm alarm set for %02d:%02d (Clock app unavailable)".format(at.hour, at.minute)
                    }
                }

                AutomationAction.RING_ALARM -> {
                    AlarmRingService.start(context, AlarmRingService.Ring(rule.id, rule.alarmLabel, rule.snoozeMinutes, rule.speakGreeting))
                    "Calm alarm ringing"
                }

                AutomationAction.SPEAK_TEXT -> {
                    val textToSpeak = resolveVariables(rule.targetMessage.ifBlank { rule.message }, context, rule)
                    if (rule.playChime) {
                        SoundSynthesizer.playSoftBell()
                        kotlinx.coroutines.delay(700)
                    }
                    VoiceFeedbackManager.speak(
                        context = context,
                        text = textToSpeak,
                        pitch = rule.speechPitch,
                        rate = rule.speechRate,
                        volume = currentPrefs.voiceStyle.volume,
                        waitForCompletion = true,
                    )
                    "Spoke \"$textToSpeak\""
                }

                AutomationAction.SEND_WHATSAPP -> {
                    val msg = resolveVariables(rule.targetMessage.ifBlank { rule.message }, context, rule)
                    val phone = rule.contactPhone.trim()
                    val url = if (phone.isNotBlank()) {
                        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
                        "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                    } else {
                        "https://api.whatsapp.com/send?text=${Uri.encode(msg)}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                        "WhatsApp opened for ${rule.contactName.ifBlank { "Contact" }}: \"$msg\""
                    } catch (e: Exception) {
                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                        "WhatsApp intent opened: \"$msg\""
                    }
                }

                AutomationAction.CALL_PHONE -> {
                    val phone = rule.contactPhone.trim()
                    require(phone.isNotBlank()) { "Add a phone number for ${rule.contactName.ifBlank { "the contact" }}" }
                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                    if (rule.targetMessage.isNotBlank()) {
                        val textToSpeak = resolveVariables(rule.targetMessage, context, rule)
                        VoiceFeedbackManager.speak(context, textToSpeak, rule.speechPitch, rule.speechRate, currentPrefs.voiceStyle.volume)
                    }
                    "Dialer opened for ${rule.contactName.ifBlank { phone }}"
                }
            }
        }
    }

    private suspend fun resolveVariables(input: String, context: Context, rule: AutomationRule, event: TriggerEvent? = null): String {
        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val totalDays = if (today.isLeapYear) 366 else 365
        val yearPercent = String.format("%.1f", (dayOfYear.toFloat() / totalDays) * 100f)
        val battery = getBatteryLevel(context)

        val greeting = if ("{greeting}" in input) {
            com.omnidot.alarm.MorningGreeting.text(com.omnidot.goals.GoalRepository(context).current(), today)
        } else ""
        return input
            .replace("{greeting}", greeting)
            .replace("{sender}", event?.sender?.ifBlank { null } ?: rule.contactName.ifBlank { "someone" })
            .replace("{message}", event?.message.orEmpty())
            .replace("{link}", event?.link.orEmpty())
            .replace("{battery}", battery.toString())
            .replace("{date}", today.toString())
            .replace("{year_percent}", yearPercent)
            .replace("{day_of_year}", dayOfYear.toString())
            .replace("{total_days}", totalDays.toString())
            .replace("{preset}", rule.preset.title)
            .replace("{trigger}", rule.trigger.label)
            .replace("{event}", rule.trigger.name)
            .replace("{contact}", rule.contactName)
            .replace("{phone}", rule.contactPhone)
            .replace("{target_message}", rule.targetMessage)
            .replace("{time}", rule.scheduledTime)
            .replace("{weather}", rule.weatherCondition)
            .replace("{timestamp}", System.currentTimeMillis().toString())
    }

    private fun buildDefaultJsonPayload(context: Context, rule: AutomationRule): String {
        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val totalDays = if (today.isLeapYear) 366 else 365
        val yearPercent = (dayOfYear.toFloat() / totalDays) * 100f

        return JSONObject().apply {
            put("event", rule.trigger.name)
            put("trigger", rule.trigger.label)
            put("timestamp", System.currentTimeMillis())
            put("date", today.toString())
            put("day_of_year", dayOfYear)
            put("year_percent", String.format("%.1f", yearPercent))
            put("battery_percent", getBatteryLevel(context))
            put("preset", rule.preset.name)
            put("preset_title", rule.preset.title)
        }.toString(2)
    }

    private fun performHttpGet(rawUrl: String): String {
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            throw IllegalArgumentException("Invalid URL: must start with http:// or https://")
        }
        val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", "OmniDot-Android/1.0")
        }
        val code = connection.responseCode
        connection.inputStream.use { it.readBytes() }
        connection.disconnect()
        return "HTTP GET $code OK"
    }

    private fun performHttpPost(rawUrl: String, body: String, contentType: String): String {
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            throw IllegalArgumentException("Invalid Webhook URL: must start with http:// or https://")
        }
        val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("User-Agent", "OmniDot-Android/1.0")
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body)
            writer.flush()
        }
        val code = connection.responseCode
        if (code in 200..299) {
            connection.inputStream.use { it.readBytes() }
            connection.disconnect()
            return "Webhook POST $code Successful"
        } else {
            val errorBody = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            connection.disconnect()
            throw RuntimeException("Webhook POST returned HTTP $code: $errorBody")
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "workflow_results"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Automation Workflows", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifications triggered by local automation rules"
                },
            )
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.omnidot.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun triggerVibration(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 160), -1),
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 160), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 80, 160), -1)
            }
        }
    }

    fun getBatteryLevel(context: Context): Int {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level.toFloat() / scale) * 100).toInt() else 100
    }

    fun getProgressSummary(preset: MatrixPreset): String {
        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val totalDays = if (today.isLeapYear) 366 else 365
        val yearPercent = String.format("%.1f", (dayOfYear.toFloat() / totalDays) * 100f)
        return "OmniDot · Day $dayOfYear/$totalDays ($yearPercent%) · ${preset.title}"
    }

    fun speakText(context: Context, text: String, pitch: Float = 1.0f, rate: Float = 1.0f) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            VoiceFeedbackManager.speak(context, text, pitch, rate)
        }
    }
}
