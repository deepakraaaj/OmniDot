package com.omnidot.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RisingBellTest {
    @Test
    fun `starts very quiet and rises to full over 90 seconds`() {
        assertEquals(0.05f, RisingBell.volumeAt(0), 0.001f)
        assertTrue(RisingBell.volumeAt(45) in 0.4f..0.65f)
        assertEquals(1f, RisingBell.volumeAt(90), 0.001f)
        assertEquals(1f, RisingBell.volumeAt(600), 0.001f)
    }

    @Test
    fun `vibration joins only once the alarm has been ignored for three minutes`() {
        assertEquals(false, RisingBell.shouldVibrate(179))
        assertEquals(true, RisingBell.shouldVibrate(180))
    }
}
