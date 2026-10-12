package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthCheckTest {
    private val speak = AutomationRule(name = "", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.SPEAK_TEXT, targetMessage = "hi")
    private val notify = speak.copy(id = "n", action = AutomationAction.SHOW_NOTIFICATION)
    private val whatsapp = speak.copy(id = "w", trigger = AutomationTrigger.WHATSAPP_MESSAGE)

    @Test
    fun `no rules means no issues`() {
        assertEquals(emptyList<HealthIssue>(), HealthCheck.issues(false, false, false, emptyList()))
    }

    @Test
    fun `issues appear only when relevant to enabled rules`() {
        assertEquals(listOf(HealthIssue.BATTERY_OPTIMIZED), HealthCheck.issues(false, false, false, listOf(speak)))
        assertEquals(listOf(HealthIssue.NOTIFICATIONS_BLOCKED), HealthCheck.issues(false, true, true, listOf(notify)))
        assertEquals(listOf(HealthIssue.LISTENER_MISSING), HealthCheck.issues(true, false, true, listOf(whatsapp)))
        assertEquals(emptyList<HealthIssue>(), HealthCheck.issues(false, false, false, listOf(whatsapp.copy(enabled = false))))
    }
}
