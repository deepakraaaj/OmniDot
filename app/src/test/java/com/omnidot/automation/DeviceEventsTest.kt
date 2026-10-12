package com.omnidot.automation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceEventsTest {
    private val wifi = "android.net.conn.CONNECTIVITY_CHANGE"

    @Test
    fun `initial sticky broadcasts never fire automations`() {
        assertNull(DeviceEvents.resolve(Intent.ACTION_HEADSET_PLUG, initialSticky = true, headsetState = 1, wifiNow = false, lastWifi = null))
        assertNull(DeviceEvents.resolve(wifi, initialSticky = true, headsetState = -1, wifiNow = true, lastWifi = null))
    }

    @Test
    fun `headset fires only when plugged in`() {
        assertEquals(AutomationTrigger.HEADSET_PLUG, DeviceEvents.resolve(Intent.ACTION_HEADSET_PLUG, false, 1, false, null))
        assertNull(DeviceEvents.resolve(Intent.ACTION_HEADSET_PLUG, false, 0, false, null))
    }

    @Test
    fun `wifi fires only when its state changes`() {
        assertNull(DeviceEvents.resolve(wifi, false, -1, wifiNow = false, lastWifi = false))
        assertNull(DeviceEvents.resolve(wifi, false, -1, wifiNow = true, lastWifi = true))
        assertEquals(AutomationTrigger.WIFI_CONNECTED, DeviceEvents.resolve(wifi, false, -1, wifiNow = true, lastWifi = false))
        assertEquals(AutomationTrigger.WIFI_DISCONNECTED, DeviceEvents.resolve(wifi, false, -1, wifiNow = false, lastWifi = true))
        assertNull(DeviceEvents.resolve(wifi, false, -1, wifiNow = true, lastWifi = null))
    }

    @Test
    fun `power and unlock events pass through`() {
        assertEquals(AutomationTrigger.POWER_CONNECTED, DeviceEvents.resolve(Intent.ACTION_POWER_CONNECTED, false, -1, false, null))
        assertEquals(AutomationTrigger.SCREEN_UNLOCKED, DeviceEvents.resolve(Intent.ACTION_USER_PRESENT, false, -1, false, null))
    }
}
