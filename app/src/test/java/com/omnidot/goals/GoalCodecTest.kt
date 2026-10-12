package com.omnidot.goals

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalCodecTest {
    private val goal = Goal("g1", "Run 5 km", "🏃", GoalTier.BIG, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
        setOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5)), archived = false)

    @Test
    fun `round trip preserves every field`() {
        val open = goal.copy(id = "g2", endDate = null, archived = true, checkIns = emptySet())
        assertEquals(listOf(goal, open), GoalCodec.decode(GoalCodec.encode(listOf(goal, open))))
    }

    @Test
    fun `blank or garbage input yields empty list`() {
        assertEquals(emptyList<Goal>(), GoalCodec.decode(""))
        assertEquals(emptyList<Goal>(), GoalCodec.decode("not json"))
    }

    @Test
    fun `bad entries are dropped while good ones survive`() {
        val raw = """[
          {"id":"bad","title":"x","tier":"BIG","start":"not-a-date"},
          {"id":"","title":"no id","start":"2026-10-01"},
          {"id":"ok","title":"Read","tier":"WHAT","start":"2026-10-01","checkIns":["2026-10-02","oops"]}
        ]"""
        val decoded = GoalCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("ok", decoded[0].id)
        assertEquals(GoalTier.SMALL, decoded[0].tier)
        assertEquals("🌱", decoded[0].emoji)
        assertEquals(setOf(LocalDate.of(2026, 10, 2)), decoded[0].checkIns)
    }

    @Test
    fun `only unparseable non-empty blobs count as corrupt`() {
        assertEquals(false, GoalCodec.isCorrupt(""))
        assertEquals(false, GoalCodec.isCorrupt("[]"))
        assertEquals(true, GoalCodec.isCorrupt("{broken"))
    }
}
