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
    fun `default charger connected and disconnected rules are configured for energetic and dramatic feedback`() {
        val connectedRule = AutomationRule.createDefaultChargerConnectedRule()
        assertEquals(AutomationTrigger.POWER_CONNECTED, connectedRule.trigger)
        assertEquals(AutomationAction.SPEAK_TEXT, connectedRule.action)
        assertEquals("YESSS!", connectedRule.targetMessage)
        assertTrue("Speech pitch should be energetic (> 1.2f)", connectedRule.speechPitch > 1.2f)
        assertTrue("Speech rate should be upbeat (>= 1.1f)", connectedRule.speechRate >= 1.1f)
        assertTrue("Chime should be enabled", connectedRule.playChime)

        val disconnectedRule = AutomationRule.createDefaultChargerDisconnectedRule()
        assertEquals(AutomationTrigger.POWER_DISCONNECTED, disconnectedRule.trigger)
        assertEquals(AutomationAction.SPEAK_TEXT, disconnectedRule.action)
        assertEquals("Oh Noo!!", disconnectedRule.targetMessage)
        assertTrue("Speech pitch should be lower/dramatic (< 1.0f)", disconnectedRule.speechPitch < 1.0f)
        assertTrue("Chime should be enabled", disconnectedRule.playChime)
    }
}
