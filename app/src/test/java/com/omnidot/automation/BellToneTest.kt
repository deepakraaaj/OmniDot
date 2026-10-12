package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class BellToneTest {
    private fun rms(samples: List<Short>) = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    @Test
    fun `bell is soft, decays, and never clips`() {
        val pcm = BellTone.generate(44_100)
        assertEquals((2.4 * 44_100).toInt(), pcm.size)
        assertTrue(pcm.maxOf { abs(it.toInt()) } <= (0.55 * Short.MAX_VALUE).toInt())
        val tenth = pcm.size / 10
        assertTrue(rms(pcm.take(tenth)) > 8 * rms(pcm.takeLast(tenth)))
    }

    @Test
    fun `speech timeout scales with text and is bounded`() {
        assertEquals(4_000L, VoiceFeedbackManager.timeoutFor("Hi"))
        assertTrue(VoiceFeedbackManager.timeoutFor("x".repeat(200)) > 4_000L)
        assertEquals(60_000L, VoiceFeedbackManager.timeoutFor("x".repeat(5_000)))
    }
}
