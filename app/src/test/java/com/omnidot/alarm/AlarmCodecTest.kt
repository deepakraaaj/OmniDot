package com.omnidot.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class AlarmCodecTest {
    @Test
    fun `round trip and lenient decode`() {
        val a = CalmAlarm("a", 390, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), "Gym", enabled = false, snoozeMinutes = 5, speakGreeting = false)
        val b = CalmAlarm("b", 420)
        assertEquals(listOf(a, b), AlarmCodec.decode(AlarmCodec.encode(listOf(a, b))))
        assertEquals(emptyList<CalmAlarm>(), AlarmCodec.decode("nope"))
        assertEquals(listOf("ok"), AlarmCodec.decode("""[{"id":""},{"id":"ok","minute":5000,"days":["FUNDAY","MONDAY"]}]""").map { it.id })
        assertEquals(1439, AlarmCodec.decode("""[{"id":"ok","minute":5000}]""").single().minuteOfDay)
    }
}
