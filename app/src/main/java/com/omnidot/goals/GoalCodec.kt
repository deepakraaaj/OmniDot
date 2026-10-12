package com.omnidot.goals

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object GoalCodec {
    fun encode(goals: List<Goal>): String = JSONArray().apply {
        goals.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("emoji", g.emoji)
                put("tier", g.tier.name)
                put("start", g.startDate.toString())
                g.endDate?.let { put("end", it.toString()) }
                put("checkIns", JSONArray(g.checkIns.sorted().map { it.toString() }))
                put("archived", g.archived)
            })
        }
    }.toString()

    /** True when [raw] holds data that cannot be parsed at all (as opposed to empty or partly bad). */
    fun isCorrupt(raw: String): Boolean = raw.isNotBlank() && runCatching { JSONArray(raw) }.isFailure

    fun decode(raw: String): List<Goal> {
        if (raw.isBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching { decodeOne(array.getJSONObject(index)) }.getOrNull()
        }
    }

    private fun decodeOne(item: JSONObject): Goal? {
        val id = item.optString("id")
        if (id.isBlank()) return null
        val tier = GoalTier.fromStored(item.optString("tier"))
        val checks = item.optJSONArray("checkIns")
        return Goal(
            id = id,
            title = item.optString("title").ifBlank { "Goal" },
            emoji = item.optString("emoji").ifBlank { tier.defaultEmoji },
            tier = tier,
            startDate = LocalDate.parse(item.getString("start")),
            endDate = item.optString("end").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            checkIns = if (checks == null) emptySet() else (0 until checks.length())
                .mapNotNull { runCatching { LocalDate.parse(checks.getString(it)) }.getOrNull() }
                .toSet(),
            archived = item.optBoolean("archived", false),
        )
    }
}
