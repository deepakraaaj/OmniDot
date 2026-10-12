package com.omnidot.automation

import com.omnidot.core.MatrixPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationRuleTest {

    @Test
    fun `all triggers have valid labels and categories`() {
        for (trigger in AutomationTrigger.entries) {
            assertTrue(trigger.label.isNotBlank())
            assertTrue(trigger.subtitle.isNotBlank())
            assertTrue(trigger.guideInfo.isNotBlank())
            assertNotNull(trigger.category)
        }
    }

    @Test
    fun `all actions have valid labels and categories`() {
        for (action in AutomationAction.entries) {
            assertTrue(action.label.isNotBlank())
            assertTrue(action.subtitle.isNotBlank())
            assertTrue(action.guideInfo.isNotBlank())
            assertNotNull(action.category)
        }
    }

    @Test
    fun `trigger fallback works safely on null or invalid stored values`() {
        assertEquals(AutomationTrigger.POWER_CONNECTED, AutomationTrigger.fromStored(null))
        assertEquals(AutomationTrigger.POWER_CONNECTED, AutomationTrigger.fromStored("NON_EXISTENT_TRIGGER"))
        assertEquals(AutomationTrigger.SCREEN_UNLOCKED, AutomationTrigger.fromStored("SCREEN_UNLOCKED"))
        assertEquals(AutomationTrigger.INBOUND_WEBHOOK, AutomationTrigger.fromStored("INBOUND_WEBHOOK"))
    }

    @Test
    fun `action fallback works safely on null or invalid stored values`() {
        assertEquals(AutomationAction.SHOW_NOTIFICATION, AutomationAction.fromStored(null))
        assertEquals(AutomationAction.SHOW_NOTIFICATION, AutomationAction.fromStored("UNKNOWN_ACTION"))
        assertEquals(AutomationAction.DISCORD_WEBHOOK, AutomationAction.fromStored("DISCORD_WEBHOOK"))
        assertEquals(AutomationAction.HTTP_POST, AutomationAction.fromStored("HTTP_POST"))
        assertEquals(AutomationAction.VIBRATE, AutomationAction.fromStored("VIBRATE"))
    }

    @Test
    fun `new workflow options omit preset-changing actions but retain saved action compatibility`() {
        assertTrue(AutomationAction.APPLY_PRESET !in AutomationAction.workflowOptions)
        assertTrue(AutomationAction.CYCLE_PRESET !in AutomationAction.workflowOptions)
        assertTrue(AutomationAction.REFRESH_CURRENT in AutomationAction.workflowOptions)
        assertTrue(AutomationAction.CYCLE_ACCENT in AutomationAction.workflowOptions)
        assertEquals(AutomationAction.APPLY_PRESET, AutomationAction.fromStored("APPLY_PRESET"))
        assertEquals(AutomationAction.CYCLE_PRESET, AutomationAction.fromStored("CYCLE_PRESET"))
    }

    @Test
    fun `automation rule default construction is valid`() {
        val rule = AutomationRule(
            name = "Test Workflow",
            trigger = AutomationTrigger.POWER_CONNECTED,
            action = AutomationAction.SHOW_NOTIFICATION,
            preset = MatrixPreset.YEAR_PROGRESS,
        )
        assertTrue(rule.id.isNotBlank())
        assertTrue(rule.enabled)
        assertEquals("Test Workflow", rule.name)
        assertEquals(AutomationTrigger.POWER_CONNECTED, rule.trigger)
        assertEquals(AutomationAction.SHOW_NOTIFICATION, rule.action)
    }

    @Test
    fun `default charger rules are calm`() {
        val connected = AutomationRule.createDefaultChargerConnectedRule()
        assertEquals(AutomationTrigger.POWER_CONNECTED, connected.trigger)
        assertEquals("Charging started", connected.targetMessage)
        assertEquals(VoiceStyle.CALM.pitch, connected.speechPitch)
        assertEquals(VoiceStyle.CALM.rate, connected.speechRate)
        val disconnected = AutomationRule.createDefaultChargerDisconnectedRule()
        assertEquals("Charger disconnected", disconnected.targetMessage)
        assertEquals(VoiceStyle.CALM.rate, disconnected.speechRate)
    }

    @Test
    fun `hidden triggers are excluded from the picker`() {
        val hidden = setOf(AutomationTrigger.WEATHER_RAIN, AutomationTrigger.DAILY_MIDNIGHT)
        hidden.forEach { assertTrue(!it.available && it !in AutomationTrigger.pickerOptions) }
        assertTrue(AutomationTrigger.POWER_CONNECTED in AutomationTrigger.pickerOptions)
        assertTrue(AutomationTrigger.SCHEDULED_TIME in AutomationTrigger.pickerOptions)
        assertTrue(AutomationTrigger.ALARM_DISMISSED in AutomationTrigger.pickerOptions)
        assertTrue(AutomationTrigger.PHONE_ALARM_RINGS in AutomationTrigger.pickerOptions)
        assertTrue(AutomationAction.RING_ALARM in AutomationAction.workflowOptions)
    }

    @Test
    fun `rule defaults carry no personal data`() {
        val rule = AutomationRule(name = "x", trigger = AutomationTrigger.POWER_CONNECTED, action = AutomationAction.VIBRATE)
        assertEquals("", rule.contactName)
        assertEquals("", rule.targetMessage)
    }
}
