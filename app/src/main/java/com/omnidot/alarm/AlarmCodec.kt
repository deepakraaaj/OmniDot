package com.omnidot.alarm

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

object AlarmCodec {
    fun encode(alarms: List<CalmAlarm>): String = JSONArray().apply {
        alarms.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id)
                put("minute", a.minuteOfDay)
                put("days", JSONArray(a.days.sorted().map { it.name }))
                put("label", a.label)
                put("enabled", a.enabled)
                put("snooze", a.snoozeMinutes)
                put("greeting", a.speakGreeting)
            })
        }
    }.toString()

    fun decode(raw: String): List<CalmAlarm> {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val item = array.getJSONObject(i)
                val id = item.optString("id")
                if (id.isBlank()) return@runCatching null
                val days = item.optJSONArray("days")
                CalmAlarm(
                    id = id,
                    minuteOfDay = item.optInt("minute", 7 * 60).coerceIn(0, 1439),
                    days = if (days == null) emptySet() else (0 until days.length())
                        .mapNotNull { d -> DayOfWeek.entries.firstOrNull { it.name == days.optString(d) } }.toSet(),
                    label = item.optString("label"),
                    enabled = item.optBoolean("enabled", true),
                    snoozeMinutes = item.optInt("snooze", 10).coerceIn(1, 60),
                    speakGreeting = item.optBoolean("greeting", true),
                )
            }.getOrNull()
        }
    }
}
