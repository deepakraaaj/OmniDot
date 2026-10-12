package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmNotificationTrackerTest {
    private val own = "com.omnidot"

    @Test
    fun `an alarm notification from the clock app rings once even when it updates`() {
        val t = AlarmNotificationTracker(own)
        assertEquals(AutomationTrigger.PHONE_ALARM_RINGS, t.onPosted("k1", "com.coloros.alarmclock", "alarm"))
        assertNull(t.onPosted("k1", "com.coloros.alarmclock", "alarm"))
    }

    @Test
    fun `removing the ringing alarm notification means it was turned off`() {
        val t = AlarmNotificationTracker(own)
        t.onPosted("k1", "com.google.android.deskclock", "alarm")
        assertEquals(AutomationTrigger.ALARM_DISMISSED, t.onRemoved("k1"))
        assertNull(t.onRemoved("k1"))
    }

    @Test
    fun `other notifications and our own alarm are ignored on post`() {
        val t = AlarmNotificationTracker(own)
        assertNull(t.onPosted("k2", "com.whatsapp", "msg"))
        assertNull(t.onPosted("k3", own, "alarm"))
        assertNull(t.onRemoved("k2"))
    }
}
