package com.omnidot.automation

import android.content.Intent

/** Maps a runtime device broadcast to a trigger, filtering replays and non-changes. */
object DeviceEvents {
    private const val CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

    fun resolve(action: String, initialSticky: Boolean, headsetState: Int, wifiNow: Boolean, lastWifi: Boolean?): AutomationTrigger? {
        // registerReceiver replays the last sticky broadcast immediately; that is not a new event.
        if (initialSticky) return null
        return when (action) {
            Intent.ACTION_POWER_CONNECTED -> AutomationTrigger.POWER_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> AutomationTrigger.POWER_DISCONNECTED
            Intent.ACTION_BATTERY_LOW -> AutomationTrigger.BATTERY_LOW
            Intent.ACTION_BATTERY_OKAY -> AutomationTrigger.BATTERY_OKAY
            Intent.ACTION_USER_PRESENT -> AutomationTrigger.SCREEN_UNLOCKED
            Intent.ACTION_HEADSET_PLUG -> AutomationTrigger.HEADSET_PLUG.takeIf { headsetState == 1 }
            CONNECTIVITY_CHANGE -> when {
                lastWifi == null || lastWifi == wifiNow -> null
                wifiNow -> AutomationTrigger.WIFI_CONNECTED
                else -> AutomationTrigger.WIFI_DISCONNECTED
            }
            else -> null
        }
    }
}
