package com.omnidot.automation

/**
 * Detects the phone's own alarm from notifications: clock apps post their ringing alarm with
 * category "alarm" and remove it when it is dismissed or snoozed. Works across OEM clock apps.
 */
class AlarmNotificationTracker(private val ownPackage: String) {
    private val ringing = mutableSetOf<String>()

    @Synchronized
    fun onPosted(key: String, packageName: String, category: String?): AutomationTrigger? {
        if (category != "alarm" || packageName == ownPackage) return null
        return if (ringing.add(key)) AutomationTrigger.PHONE_ALARM_RINGS else null
    }

    @Synchronized
    fun onRemoved(key: String): AutomationTrigger? =
        if (ringing.remove(key)) AutomationTrigger.ALARM_DISMISSED else null
}
