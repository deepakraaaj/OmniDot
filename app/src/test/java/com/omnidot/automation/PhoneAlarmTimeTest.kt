package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class PhoneAlarmTimeTest {
    @Test
    fun `adds minutes and wraps past midnight`() {
        assertEquals(LocalTime.of(22, 50), PhoneAlarmTime.inMinutes(LocalTime.of(22, 30), 20))
        assertEquals(LocalTime.of(0, 10), PhoneAlarmTime.inMinutes(LocalTime.of(23, 50, 40), 20))
    }
}
