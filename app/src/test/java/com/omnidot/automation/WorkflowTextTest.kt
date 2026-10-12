package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowTextTest {
    private fun rule(trigger: AutomationTrigger, action: AutomationAction, block: AutomationRule.() -> AutomationRule = { this }) =
        AutomationRule(name = "", trigger = trigger, action = action).block()

    @Test
    fun `sentences read naturally`() {
        val r = rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT) { copy(targetMessage = "Charging started") }
        assertEquals("When charger connects", WorkflowText.whenText(r))
        assertEquals("Speak \"Charging started\"", WorkflowText.doText(r))
        assertEquals("When charger connects → speak \"Charging started\"", WorkflowText.name(r))
        val wa = rule(AutomationTrigger.WHATSAPP_MESSAGE, AutomationAction.HTTP_POST) { copy(url = "https://hooks.example.com/x") }
        assertEquals("When anyone messages on WhatsApp", WorkflowText.whenText(wa))
        assertEquals("Send webhook to hooks.example.com", WorkflowText.doText(wa))
        assertEquals("Send webhook to a URL", WorkflowText.doText(wa.copy(url = "not a url")))
    }

    @Test
    fun `last run text`() {
        val now = 10_000_000L
        val base = rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.VIBRATE)
        assertEquals(WorkflowText.LastRun("Not run yet", false), WorkflowText.lastRun(base, now))
        assertEquals(WorkflowText.LastRun("✓ Ran just now", false), WorkflowText.lastRun(base.copy(lastRunTime = now - 20_000, lastRunStatus = "ok"), now))
        assertEquals(WorkflowText.LastRun("✓ Ran 5 min ago", false), WorkflowText.lastRun(base.copy(lastRunTime = now - 300_000, lastRunStatus = "ok"), now))
        assertEquals(WorkflowText.LastRun("✕ boom", true), WorkflowText.lastRun(base.copy(lastRunTime = now - 1, lastRunStatus = "Error: boom"), now))
        assertEquals(WorkflowText.LastRun("✕ bad", true), WorkflowText.lastRun(base.copy(lastRunTime = now - 1, lastRunStatus = "✕ Failed: bad"), now))
    }

    @Test
    fun `can save requires message, contact and url where needed`() {
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT)))
        assertTrue(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT) { copy(targetMessage = "hi") }))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SEND_WHATSAPP) { copy(targetMessage = "hi") }))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.HTTP_POST)))
        assertTrue(WorkflowText.canSave(rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.VIBRATE)))
        assertTrue(WorkflowText.canSave(rule(AutomationTrigger.SCHEDULED_TIME, AutomationAction.VIBRATE)))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.SCHEDULED_TIME, AutomationAction.VIBRATE) { copy(scheduledTime = "7am") }))
        assertFalse(WorkflowText.canSave(rule(AutomationTrigger.WEATHER_RAIN, AutomationAction.VIBRATE)))
    }

    @Test
    fun `blank speak text reads as a prompt not empty quotes`() {
        val r = rule(AutomationTrigger.POWER_CONNECTED, AutomationAction.SPEAK_TEXT)
        assertEquals("Speak your words", WorkflowText.doText(r))
    }

    @Test
    fun `time trigger sentence names the days`() {
        val r = rule(AutomationTrigger.SCHEDULED_TIME, AutomationAction.VIBRATE) { copy(scheduledTime = "07:00") }
        assertEquals("Every day at 07:00", WorkflowText.whenText(r))
        val weekdays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY)
        assertEquals("Weekdays at 07:00", WorkflowText.whenText(r.copy(days = weekdays)))
    }

    @Test
    fun `alarm sentences`() {
        val ring = rule(AutomationTrigger.SCHEDULED_TIME, AutomationAction.RING_ALARM) { copy(scheduledTime = "06:30") }
        assertEquals("Ring calm alarm", WorkflowText.doText(ring))
        assertEquals("Ring calm alarm \"Gym\"", WorkflowText.doText(ring.copy(alarmLabel = "Gym")))
        assertTrue(WorkflowText.canSave(ring))
        assertEquals("When I turn off my alarm", WorkflowText.whenText(rule(AutomationTrigger.ALARM_DISMISSED, AutomationAction.VIBRATE)))
        assertEquals("When my alarm rings", WorkflowText.whenText(rule(AutomationTrigger.PHONE_ALARM_RINGS, AutomationAction.VIBRATE)))
    }

    @Test
    fun `link alarm sentences and validation`() {
        val r = rule(AutomationTrigger.WHATSAPP_MESSAGE, AutomationAction.SET_PHONE_ALARM) { copy(contactName = "Pradeep", onlyLinks = true, alarmInMinutes = 20) }
        assertEquals("When Pradeep sends a link on WhatsApp", WorkflowText.whenText(r))
        assertEquals("Set phone alarm in 20 min", WorkflowText.doText(r))
        assertTrue(WorkflowText.canSave(r))
        assertFalse(WorkflowText.canSave(r.copy(alarmInMinutes = 0)))
    }
}
