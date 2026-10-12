package com.omnidot.automation

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omnidot.core.MatrixPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.automationDataStore by preferencesDataStore(name = "omnidot_automations")

class AutomationRepository(private val context: Context) {
    private val rulesKey = stringPreferencesKey("rules_json")
    private val seededKey = booleanPreferencesKey("defaults_seeded")

    val rules: Flow<List<AutomationRule>> = context.automationDataStore.data.map { values ->
        decode(values[rulesKey].orEmpty())
    }

    suspend fun current(): List<AutomationRule> = rules.first()

    suspend fun save(rule: AutomationRule) = update { existing ->
        existing.filterNot { it.id == rule.id } + rule
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = update { existing ->
        existing.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    suspend fun recordRun(id: String, status: String) = update { existing ->
        existing.map {
            if (it.id == id) it.copy(
                lastRunTime = System.currentTimeMillis(),
                lastRunStatus = status,
            ) else it
        }
    }

    suspend fun delete(id: String) = update { existing -> existing.filterNot { it.id == id } }

    suspend fun ensureDefaultRules() {
        context.automationDataStore.edit { values ->
            if (values[seededKey] == true) return@edit
            if (decode(values[rulesKey].orEmpty()).isEmpty()) values[rulesKey] = encode(defaultRules())
            values[seededKey] = true
        }
    }

    private suspend fun update(transform: (List<AutomationRule>) -> List<AutomationRule>) {
        context.automationDataStore.edit { values ->
            values[rulesKey] = encode(transform(decode(values[rulesKey].orEmpty())))
        }
    }

    private fun encode(rules: List<AutomationRule>): String = JSONArray().apply {
        rules.forEach { rule ->
            put(JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("enabled", rule.enabled)
                put("trigger", rule.trigger.name)
                put("action", rule.action.name)
                put("preset", rule.preset.name)
                put("contactName", rule.contactName)
                put("contactPhone", rule.contactPhone)
                put("targetMessage", rule.targetMessage)
                put("scheduledTime", rule.scheduledTime)
                put("days", JSONArray(rule.days.sorted().map { it.name }))
                put("weatherCondition", rule.weatherCondition)
                put("url", rule.url)
                put("message", rule.message)
                put("customPayload", rule.customPayload)
                put("lastRunTime", rule.lastRunTime)
                put("lastRunStatus", rule.lastRunStatus)
                put("speechPitch", rule.speechPitch.toDouble())
                put("speechRate", rule.speechRate.toDouble())
                put("playChime", rule.playChime)
                put("alarmLabel", rule.alarmLabel)
                put("snoozeMinutes", rule.snoozeMinutes)
                put("speakGreeting", rule.speakGreeting)
                put("onlyLinks", rule.onlyLinks)
                put("alarmInMinutes", rule.alarmInMinutes)
                put("code", rule.code)
            })
        }
    }.toString()

    private fun decode(raw: String): List<AutomationRule> = runCatching {
        val array = if (raw.isBlank()) JSONArray() else JSONArray(raw)
        val decoded = buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    AutomationRule(
                        id = item.optString("id", java.util.UUID.randomUUID().toString()),
                        name = item.optString("name", "Custom workflow"),
                        enabled = item.optBoolean("enabled", true),
                        trigger = AutomationTrigger.fromStored(item.optString("trigger")),
                        action = AutomationAction.fromStored(item.optString("action")),
                        preset = MatrixPreset.fromStored(item.optString("preset")),
                        contactName = item.optString("contactName", ""),
                        contactPhone = item.optString("contactPhone", ""),
                        targetMessage = item.optString("targetMessage", ""),
                        scheduledTime = item.optString("scheduledTime", "07:00"),
                        days = item.optJSONArray("days")?.let { days ->
                            (0 until days.length()).mapNotNull { d -> java.time.DayOfWeek.entries.firstOrNull { it.name == days.optString(d) } }.toSet()
                        } ?: emptySet(),
                        weatherCondition = item.optString("weatherCondition", "Rain"),
                        url = item.optString("url"),
                        message = item.optString("message", "Progress: {year_percent}% complete"),
                        customPayload = item.optString("customPayload", ""),
                        lastRunTime = item.optLong("lastRunTime", 0L),
                        lastRunStatus = item.optString("lastRunStatus", ""),
                        speechPitch = item.optDouble("speechPitch", 1.0).toFloat(),
                        speechRate = item.optDouble("speechRate", 1.0).toFloat(),
                        playChime = item.optBoolean("playChime", true),
                        alarmLabel = item.optString("alarmLabel", ""),
                        snoozeMinutes = item.optInt("snoozeMinutes", 10).coerceIn(1, 60),
                        speakGreeting = item.optBoolean("speakGreeting", true),
                        onlyLinks = item.optBoolean("onlyLinks", false),
                        alarmInMinutes = item.optInt("alarmInMinutes", 20).coerceIn(1, 1440),
                        code = item.optString("code", ""),
                    ),
                )
            }
        }
        decoded
    }.getOrDefault(emptyList())

    companion object {
        fun defaultRules(): List<AutomationRule> = listOf(
            AutomationRule.createDefaultChargerConnectedRule(),
            AutomationRule.createDefaultChargerDisconnectedRule(),
        )
    }
}
